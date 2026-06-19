package br.com.murilo.liberthia.admin.sounds;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sounds")
public class SoundsController {

    private final SoundsService sounds;

    public SoundsController(SoundsService sounds) { this.sounds = sounds; }

    /** Upload de .ogg. Form data: file, namespace, key. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "namespace", defaultValue = "liberthia") String namespace,
            @RequestParam("key") String key) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file empty"));
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".ogg")) {
            return ResponseEntity.badRequest().body(Map.of("error", "only .ogg files accepted"));
        }
        try (var in = file.getInputStream()) {
            SoundsService.SoundEntry entry = sounds.upload(namespace, key, in);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "namespace", entry.namespace(),
                    "key", entry.key(),
                    "sizeBytes", entry.sizeBytes(),
                    "playId", entry.namespace() + ":" + entry.key()
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public Map<String, Object> list() throws IOException {
        List<SoundsService.SoundEntry> all = sounds.list();
        return Map.of(
                "sounds", all.stream().map(e -> Map.of(
                        "namespace", e.namespace(),
                        "key", e.key(),
                        "sizeBytes", e.sizeBytes(),
                        "playId", e.namespace() + ":" + e.key()
                )).toList(),
                "packSha1", sounds.sha1(),
                "packSize", sounds.resourcePack().length
        );
    }

    @DeleteMapping("/{namespace}/{key}")
    public Map<String, Object> delete(@PathVariable String namespace, @PathVariable String key) {
        boolean ok = sounds.delete(namespace, key);
        return Map.of("ok", ok);
    }
}

/**
 * Endpoint público pra servir o resource pack (sem auth pra MC clients
 * conseguirem baixar sem precisar de token). Em produção, recomenda-se
 * por trás de CDN ou caching nginx.
 */
@RestController
class SoundsPackController {

    private final SoundsService sounds;

    SoundsPackController(SoundsService sounds) { this.sounds = sounds; }

    /** /api/sounds-pack.zip — público (não exige Authorization). */
    @GetMapping(value = "/api/sounds-pack.zip", produces = "application/zip")
    public ResponseEntity<byte[]> pack() throws IOException {
        byte[] data = sounds.resourcePack();
        String sha1 = sounds.sha1();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liberthia-sounds.zip\"")
                .header("X-Pack-SHA1", sha1)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=60")
                .body(data);
    }
}
