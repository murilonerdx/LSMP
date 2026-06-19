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
@RequestMapping("/api/particles")
public class ParticlesController {

    private final ParticlesService particles;
    public ParticlesController(ParticlesService particles) { this.particles = particles; }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("key") String key) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "file empty"));
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".png")) return ResponseEntity.badRequest().body(Map.of("error", "only .png accepted"));
        try (var in = file.getInputStream()) {
            ParticlesService.ParticleEntry entry = particles.upload(key, in);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "key", entry.key(),
                    "sizeBytes", entry.sizeBytes(),
                    "particleId", "minecraft:" + entry.key()
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public Map<String, Object> list() throws IOException {
        List<ParticlesService.ParticleEntry> all = particles.list();
        return Map.of(
                "particles", all.stream().map(e -> Map.of(
                        "key", e.key(),
                        "sizeBytes", e.sizeBytes(),
                        "particleId", "minecraft:" + e.key()
                )).toList(),
                "packSha1", particles.sha1(),
                "packSize", particles.resourcePack().length
        );
    }

    @DeleteMapping("/{key}")
    public Map<String, Object> delete(@PathVariable String key) {
        return Map.of("ok", particles.delete(key));
    }

    /** Preview da PNG salva (pra mostrar no frontend). */
    @GetMapping(value = "/preview/{key}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> preview(@PathVariable String key) throws IOException {
        byte[] png = particles.readPng(key);
        if (png == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "max-age=10").body(png);
    }
}

@RestController
class ParticlesPackController {
    private final ParticlesService particles;
    ParticlesPackController(ParticlesService particles) { this.particles = particles; }

    /** /api/particles-pack.zip — público (sem auth, cliente MC baixa direto). */
    @GetMapping(value = "/api/particles-pack.zip", produces = "application/zip")
    public ResponseEntity<byte[]> pack() throws IOException {
        byte[] data = particles.resourcePack();
        String sha1 = particles.sha1();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liberthia-particles.zip\"")
                .header("X-Pack-SHA1", sha1)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=60")
                .body(data);
    }
}
