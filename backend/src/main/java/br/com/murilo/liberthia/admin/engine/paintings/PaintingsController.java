package br.com.murilo.liberthia.admin.engine.paintings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Repository
interface CustomPaintingRepository extends JpaRepository<CustomPainting, Long> {
    Page<CustomPainting> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable p);
    Page<CustomPainting> findByCategoryOrderByCreatedAtDesc(String category, org.springframework.data.domain.Pageable p);
    Page<CustomPainting> findByFeaturedTrueOrderByCreatedAtDesc(org.springframework.data.domain.Pageable p);
    Page<CustomPainting> findByAuthorUuidOrderByCreatedAtDesc(String authorUuid, org.springframework.data.domain.Pageable p);
}

@RestController
@RequestMapping("/api/paintings")
public class PaintingsController {

    private final CustomPaintingRepository repo;
    private final Path storageDir;

    public PaintingsController(CustomPaintingRepository repo,
                               @Value("${voice.storage-dir:data/voice-clips}") String voiceDir) {
        this.repo = repo;
        // Irmã da pasta voice (data/paintings)
        this.storageDir = Paths.get(voiceDir).getParent() != null
                ? Paths.get(voiceDir).getParent().resolve("paintings")
                : Paths.get("data/paintings");
        try { Files.createDirectories(storageDir); } catch (IOException ignored) {}
    }

    @GetMapping
    public Page<CustomPainting> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String authorUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "60") int size) {
        var pg = PageRequest.of(page, Math.min(size, 200));
        if (Boolean.TRUE.equals(featured)) return repo.findByFeaturedTrueOrderByCreatedAtDesc(pg);
        if (authorUuid != null && !authorUuid.isBlank())
            return repo.findByAuthorUuidOrderByCreatedAtDesc(authorUuid, pg);
        if (category != null && !category.isBlank())
            return repo.findByCategoryOrderByCreatedAtDesc(category, pg);
        return repo.findAllByOrderByCreatedAtDesc(pg);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam MultipartFile file,
                                    @RequestParam String title,
                                    @RequestParam(required = false) String authorUuid,
                                    @RequestParam(required = false) String authorName,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(required = false, defaultValue = "art") String category) throws IOException {
        if (file.isEmpty() || file.getSize() > 30L * 1024 * 1024)
            return ResponseEntity.badRequest().body(Map.of("error", "arquivo vazio ou > 30MB"));

        byte[] bytes = file.getBytes();
        int w = 0, h = 0;
        try {
            var img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) { w = img.getWidth(); h = img.getHeight(); }
        } catch (Exception ignored) {}

        String filename = UUID.randomUUID() + ".png";
        Path target = storageDir.resolve(filename);
        Files.write(target, bytes);

        CustomPainting p = new CustomPainting();
        p.setTitle(title);
        p.setAuthorUuid(authorUuid == null ? "" : authorUuid);
        p.setAuthorName(authorName == null ? "?" : authorName);
        p.setDescription(description == null ? "" : description);
        p.setCategory(category);
        p.setFilePath(filename);
        p.setSizeBytes(bytes.length);
        p.setWidth(w); p.setHeight(h);
        return ResponseEntity.ok(repo.save(p));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> image(@PathVariable Long id) {
        return repo.findById(id).map(p -> {
            Path f = storageDir.resolve(p.getFilePath());
            if (!Files.exists(f)) return ResponseEntity.notFound().<Object>build();
            p.setViewCount(p.getViewCount() + 1);
            repo.save(p);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body((Object) new FileSystemResource(f));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/feature")
    public CustomPainting feature(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean on) {
        CustomPainting p = repo.findById(id).orElseThrow();
        p.setFeatured(on);
        return repo.save(p);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomPainting> update(@PathVariable Long id, @RequestBody CustomPainting body) {
        return repo.findById(id).map(p -> {
            if (body.getTitle() != null) p.setTitle(body.getTitle());
            if (body.getDescription() != null) p.setDescription(body.getDescription());
            if (body.getCategory() != null) p.setCategory(body.getCategory());
            return ResponseEntity.ok(repo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.findById(id).ifPresent(p -> {
            try { Files.deleteIfExists(storageDir.resolve(p.getFilePath())); } catch (IOException ignored) {}
            repo.deleteById(id);
        });
        return Map.of("ok", true);
    }
}
