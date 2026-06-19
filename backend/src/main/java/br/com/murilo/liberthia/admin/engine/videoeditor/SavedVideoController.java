package br.com.murilo.liberthia.admin.engine.videoeditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;

/**
 * Saved videos — vídeos salvos com título/descrição. Tela admin gerencia,
 * /watch/{id} no frontend mostra publicamente (se isPublic=true).
 *
 * Endpoints:
 *  - GET    /api/videos                — lista todos (admin)
 *  - GET    /api/videos/{id}           — detalhe (admin)
 *  - PUT    /api/videos/{id}           — atualiza title/desc/public
 *  - DELETE /api/videos/{id}           — apaga (DB + arquivo)
 *  - POST   /api/videos/save-render/{jobId}  — salva render do editor com metadata
 *  - POST   /api/videos/upload         — upload manual de MP4
 *
 * Públicos (sem auth, whitelist no AuthFilter):
 *  - GET /api/public/videos            — lista vídeos com isPublic=true
 *  - GET /api/public/videos/{id}       — detalhe + incrementa viewCount
 *  - GET /api/public/videos/{id}/stream — stream do MP4 (sem auth)
 */
@RestController
public class SavedVideoController {

    private static final Logger LOG = LoggerFactory.getLogger(SavedVideoController.class);

    public interface Repo extends JpaRepository<SavedVideo, Long> {
        @Query("SELECT v FROM SavedVideo v WHERE v.isPublic = true ORDER BY v.createdAt DESC")
        List<SavedVideo> findPublic(org.springframework.data.domain.Pageable pageable);
    }

    private final Repo repo;
    private final VideoEditorController editor;

    @Value("${video.storage-dir:/app/data/videos}")
    private String videoStorageDir;

    public SavedVideoController(Repo repo, VideoEditorController editor) {
        this.repo = repo;
        this.editor = editor;
    }

    private Path savedDir() {
        try {
            Path p = Paths.get(videoStorageDir, "saved");
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // ADMIN endpoints (require auth)
    // =========================================================================

    @GetMapping("/api/videos")
    public Map<String, Object> listAll(@RequestParam(defaultValue = "100") int limit) {
        var rows = repo.findAll(PageRequest.of(0, Math.max(1, Math.min(500, limit)),
                Sort.by(Sort.Order.desc("createdAt"))));
        return Map.of(
                "videos", rows.getContent(),
                "count", rows.getTotalElements()
        );
    }

    @GetMapping("/api/videos/{id}")
    public ResponseEntity<SavedVideo> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record VideoUpdate(String title, String description, Boolean isPublic) {}

    @PutMapping("/api/videos/{id}")
    @Transactional
    public ResponseEntity<SavedVideo> update(@PathVariable Long id, @RequestBody VideoUpdate body) {
        return repo.findById(id).map(v -> {
            if (body.title() != null && !body.title().isBlank()) v.setTitle(body.title().trim());
            if (body.description() != null) v.setDescription(body.description());
            if (body.isPublic() != null) v.setPublic(body.isPublic());
            return ResponseEntity.ok(repo.save(v));
        }).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/api/videos/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable Long id) {
        return repo.findById(id).map(v -> {
            Path file = savedDir().resolve(v.getFilename());
            try { Files.deleteIfExists(file); } catch (Exception ignored) {}
            repo.delete(v);
            return Map.of("ok", (Object) true, "deleted", (Object) v.getFilename());
        }).orElseGet(() -> Map.of("ok", false, "error", "not found"));
    }

    /**
     * Salva um vídeo renderizado pelo editor — copia o arquivo de
     * /app/data/videos/video_*.mp4 pra /saved/UUID.mp4 e cria metadata.
     */
    public record SaveRenderBody(String title, String description, Boolean isPublic) {}

    @PostMapping("/api/videos/save-render/{jobId}")
    public ResponseEntity<?> saveRender(@PathVariable String jobId, @RequestBody SaveRenderBody body) throws IOException {
        if (body == null || body.title() == null || body.title().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title obrigatório"));
        }
        // Acha o arquivo do render via VideoEditorController
        Path renderFile = editor.findRenderOutput(jobId);
        if (renderFile == null || !Files.exists(renderFile)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "render output não encontrado pra jobId=" + jobId +
                            " (talvez já tenha sido limpo pelo cleanup automático)"));
        }

        String filename = UUID.randomUUID() + ".mp4";
        Path target = savedDir().resolve(filename);
        Files.copy(renderFile, target, StandardCopyOption.REPLACE_EXISTING);

        SavedVideo v = new SavedVideo();
        v.setTitle(body.title().trim());
        v.setDescription(body.description() == null ? "" : body.description());
        v.setFilename(filename);
        v.setSizeBytes(Files.size(target));
        v.setDurationMs(probeDurationMs(target));
        v.setPublic(body.isPublic() == null ? true : body.isPublic());
        v.setSource("rendered");
        v.setRenderJobId(jobId);
        v.setUploadedBy("admin");
        v = repo.save(v);
        return ResponseEntity.ok(v);
    }

    /** Upload manual de MP4 (multipart). */
    @PostMapping(value = "/api/videos/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam("title") String title,
                                    @RequestParam(required = false, defaultValue = "") String description,
                                    @RequestParam(required = false, defaultValue = "true") boolean isPublic) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file vazio"));
        }
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title obrigatório"));
        }
        String orig = file.getOriginalFilename() == null ? "video.mp4" : file.getOriginalFilename();
        if (!orig.toLowerCase().endsWith(".mp4") && !orig.toLowerCase().endsWith(".webm") && !orig.toLowerCase().endsWith(".mov")) {
            return ResponseEntity.badRequest().body(Map.of("error", "extensão precisa ser .mp4/.webm/.mov"));
        }
        String filename = UUID.randomUUID() + ".mp4";
        Path target = savedDir().resolve(filename);
        Files.write(target, file.getBytes());

        SavedVideo v = new SavedVideo();
        v.setTitle(title.trim());
        v.setDescription(description);
        v.setFilename(filename);
        v.setSizeBytes(file.getSize());
        v.setDurationMs(probeDurationMs(target));
        v.setPublic(isPublic);
        v.setSource("uploaded");
        v.setUploadedBy("admin");
        v = repo.save(v);
        return ResponseEntity.ok(v);
    }

    /** Download autenticado (admin) — qualquer vídeo, inclusive não-públicos. */
    @GetMapping("/api/videos/{id}/download")
    public ResponseEntity<FileSystemResource> downloadAdmin(@PathVariable Long id) {
        return repo.findById(id).map(v -> {
            Path file = savedDir().resolve(v.getFilename());
            if (!Files.exists(file)) return new ResponseEntity<FileSystemResource>(HttpStatus.NOT_FOUND);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + sanitize(v.getTitle()) + ".mp4\"")
                    .body(new FileSystemResource(file.toFile()));
        }).orElseGet(() -> new ResponseEntity<FileSystemResource>(HttpStatus.NOT_FOUND));
    }

    // =========================================================================
    // PUBLIC endpoints (whitelist no AuthFilter)
    // =========================================================================

    @GetMapping("/api/public/videos")
    public Map<String, Object> listPublic(@RequestParam(defaultValue = "50") int limit) {
        var rows = repo.findPublic(PageRequest.of(0, Math.max(1, Math.min(200, limit))));
        return Map.of("videos", rows, "count", rows.size());
    }

    @GetMapping("/api/public/videos/{id}")
    @Transactional
    public ResponseEntity<?> getPublic(@PathVariable Long id) {
        return repo.findById(id).<ResponseEntity<?>>map(v -> {
            if (!v.isPublic()) return ResponseEntity.status(404).body(Map.of("error", "video privado ou não encontrado"));
            v.setViewCount(v.getViewCount() + 1);
            repo.save(v);
            return ResponseEntity.ok(v);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Stream público do MP4 (sem auth) — usado pelo player na /watch/{id}. */
    @GetMapping("/api/public/videos/{id}/stream")
    public ResponseEntity<FileSystemResource> stream(@PathVariable Long id) {
        return repo.findById(id).map(v -> {
            if (!v.isPublic()) return new ResponseEntity<FileSystemResource>(HttpStatus.NOT_FOUND);
            Path file = savedDir().resolve(v.getFilename());
            if (!Files.exists(file)) return new ResponseEntity<FileSystemResource>(HttpStatus.NOT_FOUND);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(new FileSystemResource(file.toFile()));
        }).orElseGet(() -> new ResponseEntity<FileSystemResource>(HttpStatus.NOT_FOUND));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private long probeDurationMs(Path file) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffprobe",
                    "-v", "error", "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1", file.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .readLine();
            p.waitFor();
            if (output == null || output.isBlank()) return 0;
            return (long) (Double.parseDouble(output.trim()) * 1000);
        } catch (Exception e) {
            return 0;
        }
    }

    private String sanitize(String s) {
        return s == null ? "video" : s.replaceAll("[^A-Za-z0-9_.\\-]", "_");
    }
}
