package br.com.murilo.liberthia.admin.engine.photos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/photos")
public class ExposurePhotoController {

    private final ExposurePhotoRepository repo;
    private final Path storageDir;

    public ExposurePhotoController(ExposurePhotoRepository repo,
                                   @Value("${voice.storage-dir:data/voice-clips}") String voiceDir) {
        this.repo = repo;
        // Reusa pasta de storage do voice (diretório irmão "photos")
        this.storageDir = Paths.get(voiceDir).getParent() != null
                ? Paths.get(voiceDir).getParent().resolve("photos")
                : Paths.get("data/photos");
        try { Files.createDirectories(storageDir); } catch (IOException ignored) {}
    }

    @GetMapping
    public Page<ExposurePhoto> list(
            @RequestParam(required = false) Boolean cursed,
            @RequestParam(required = false) String authorUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "60") int size) {
        var pg = PageRequest.of(page, Math.min(size, 300));
        if (Boolean.TRUE.equals(cursed)) return repo.findByCursedTrueOrderByTakenAtDesc(pg);
        if (authorUuid != null && !authorUuid.isBlank())
            return repo.findByAuthorUuidOrderByTakenAtDesc(authorUuid, pg);
        return repo.findAllByOrderByTakenAtDesc(pg);
    }

    /** Photo Library: grid de pastas (1 por player) com count + bytes + range de datas. */
    @GetMapping("/library/players")
    public Map<String, Object> libraryPlayers() {
        var rows = repo.aggregateByPlayer();
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("authorUuid", r[0]);
            m.put("authorName", r[1]);
            m.put("photoCount", ((Number) r[2]).longValue());
            m.put("totalBytes", r[3] == null ? 0L : ((Number) r[3]).longValue());
            m.put("firstPhotoAt", r[4] == null ? null : r[4].toString());
            m.put("lastPhotoAt", r[5] == null ? null : r[5].toString());
            m.put("cursedCount", r[6] == null ? 0L : ((Number) r[6]).longValue());
            out.add(m);
        }
        return Map.of("players", out, "count", out.size());
    }

    /** Photo Library: busca com filtros (size/date/cursed) e ordenação. */
    @GetMapping("/library/search")
    public Map<String, Object> librarySearch(
            @RequestParam(required = false) String authorUuid,
            @RequestParam(required = false, defaultValue = "0") long minBytes,
            @RequestParam(required = false, defaultValue = "0") long maxBytes,
            @RequestParam(required = false, defaultValue = "0") long fromMs,
            @RequestParam(required = false, defaultValue = "0") long toMs,
            @RequestParam(required = false, defaultValue = "false") boolean onlyCursed,
            @RequestParam(required = false, defaultValue = "recent") String sort,
            @RequestParam(required = false, defaultValue = "200") int limit) {
        org.springframework.data.domain.Sort s = switch (sort) {
            case "oldest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("takenAt"));
            case "biggest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("sizeBytes"));
            case "smallest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("sizeBytes"));
            default -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("takenAt"));
        };
        var pg = PageRequest.of(0, Math.min(1000, Math.max(1, limit)), s);
        java.time.Instant fromI = fromMs > 0 ? java.time.Instant.ofEpochMilli(fromMs) : java.time.Instant.EPOCH;
        java.time.Instant toI   = toMs > 0   ? java.time.Instant.ofEpochMilli(toMs)   : java.time.Instant.now().plusSeconds(86400);
        var photos = repo.search(
                authorUuid == null || authorUuid.isBlank() ? null : authorUuid,
                Math.max(0, minBytes), Math.max(0, maxBytes),
                fromMs, toMs, fromI, toI, onlyCursed, pg);
        return Map.of("photos", photos, "count", photos.size());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam MultipartFile file,
                                    @RequestParam(required = false) String authorUuid,
                                    @RequestParam(required = false) String authorName,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String description) throws IOException {
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024)
            return ResponseEntity.badRequest().body(Map.of("error", "file empty or > 20MB"));

        byte[] bytes = file.getBytes();
        int w = 0, h = 0;
        try {
            var img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) { w = img.getWidth(); h = img.getHeight(); }
        } catch (Exception ignored) {}

        String filename = UUID.randomUUID() + ".png";
        Path target = storageDir.resolve(filename);
        Files.write(target, bytes);

        ExposurePhoto p = new ExposurePhoto();
        p.setAuthorUuid(authorUuid == null ? "" : authorUuid);
        p.setAuthorName(authorName == null ? "?" : authorName);
        p.setTitle(title == null ? "" : title);
        p.setDescription(description == null ? "" : description);
        p.setFilePath(filename);
        p.setSizeBytes(bytes.length);
        p.setWidth(w); p.setHeight(h);
        p.setTakenAt(Instant.now());
        return ResponseEntity.ok(repo.save(p));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> image(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            Path f = storageDir.resolve(p.getFilePath());
            if (!Files.exists(f)) return ResponseEntity.notFound().<Object>build();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body((Object) new FileSystemResource(f));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/curse")
    public ExposurePhoto curse(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean on) {
        ExposurePhoto p = repo.findById(id).orElseThrow();
        p.setCursed(on);
        return repo.save(p);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.findById(id).ifPresent(p -> {
            try { Files.deleteIfExists(storageDir.resolve(p.getFilePath())); } catch (IOException ignored) {}
            repo.deleteById(id);
        });
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
