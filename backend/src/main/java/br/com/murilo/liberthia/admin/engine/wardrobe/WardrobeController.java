package br.com.murilo.liberthia.admin.engine.wardrobe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/wardrobe")
public class WardrobeController {

    private final WardrobeRepository repo;
    private final Path storageDir;

    public WardrobeController(WardrobeRepository repo,
                              @Value("${voice.storage-dir:data/voice-clips}") String voiceDir) {
        this.repo = repo;
        this.storageDir = Paths.get(voiceDir).getParent() != null
                ? Paths.get(voiceDir).getParent().resolve("wardrobe")
                : Paths.get("data/wardrobe");
        try { Files.createDirectories(storageDir); } catch (IOException ignored) {}
    }

    @GetMapping
    public Page<WardrobeItem> list(
            @RequestParam(required = false) String kind,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        var pg = PageRequest.of(page, Math.min(size, 100));
        if (kind != null && !kind.isBlank()) return repo.findByKindOrderByCreatedAtDesc(kind, pg);
        return "top".equals(sort) ? repo.findAllByOrderByUpvotesDesc(pg) : repo.findAllByOrderByCreatedAtDesc(pg);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam MultipartFile file,
                                    @RequestParam String name,
                                    @RequestParam(required = false) String description,
                                    @RequestParam(defaultValue = "armor") String kind,
                                    @RequestParam(required = false) String uploaderUuid,
                                    @RequestParam(required = false) String uploaderName,
                                    @RequestParam(defaultValue = "0") int priceMatter) throws IOException {
        if (file.isEmpty() || file.getSize() > 10L * 1024 * 1024)
            return ResponseEntity.badRequest().body(Map.of("error", "file empty or > 10MB"));
        String ext = guessExt(file.getOriginalFilename(), kind);
        String filename = UUID.randomUUID() + ext;
        Files.write(storageDir.resolve(filename), file.getBytes());

        WardrobeItem w = new WardrobeItem();
        w.setName(name);
        w.setDescription(description == null ? "" : description);
        w.setKind(kind);
        w.setFilePath(filename);
        w.setSizeBytes(file.getSize());
        w.setUploaderUuid(uploaderUuid == null ? "" : uploaderUuid);
        w.setUploaderName(uploaderName == null ? "?" : uploaderName);
        w.setPriceMatter(priceMatter);
        w.setCreatedAt(Instant.now());
        return ResponseEntity.ok(repo.save(w));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<?> download(@PathVariable Long id) {
        return repo.findById(id).map(w -> {
            w.setDownloads(w.getDownloads() + 1);
            repo.save(w);
            Path f = storageDir.resolve(w.getFilePath());
            if (!Files.exists(f)) return ResponseEntity.notFound().<Object>build();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body((Object) new FileSystemResource(f));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/upvote")
    public WardrobeItem upvote(@PathVariable Long id) {
        WardrobeItem w = repo.findById(id).orElseThrow();
        w.setUpvotes(w.getUpvotes() + 1);
        return repo.save(w);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.findById(id).ifPresent(w -> {
            try { Files.deleteIfExists(storageDir.resolve(w.getFilePath())); } catch (IOException ignored) {}
            repo.deleteById(id);
        });
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static String guessExt(String orig, String kind) {
        if (orig != null && orig.contains(".")) return orig.substring(orig.lastIndexOf('.'));
        return switch (kind) {
            case "cpm" -> ".cpmproject";
            case "armor" -> ".armour";
            default -> ".bin";
        };
    }
}
