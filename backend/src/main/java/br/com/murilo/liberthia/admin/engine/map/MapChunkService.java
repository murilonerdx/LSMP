package br.com.murilo.liberthia.admin.engine.map;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Repository
interface MapChunkCacheRepository extends JpaRepository<MapChunkCache, MapChunkCache.PK> {}

@Service
class MapChunkService {
    private static final Logger LOG = LoggerFactory.getLogger(MapChunkService.class);
    private final MapChunkCacheRepository repo;
    private final ModBridgeClient mod;

    MapChunkService(MapChunkCacheRepository repo, ModBridgeClient mod) {
        this.repo = repo;
        this.mod = mod;
    }

    public byte[] getChunkPng(String dim, int cx, int cz) {
        String dimKey = dim == null || dim.isBlank() ? "minecraft:overworld" : dim;
        MapChunkCache.PK pk = new MapChunkCache.PK(dimKey, cx, cz);
        MapChunkCache cached = repo.findById(pk).orElse(null);

        // Cache stale após 5min — refetch
        if (cached != null && cached.getUpdatedAt().isAfter(Instant.now().minus(5, ChronoUnit.MINUTES))) {
            return cached.getPng();
        }

        try {
            byte[] fresh = mod.mapChunkPng(dim, cx, cz);
            if (fresh != null && fresh.length > 0) {
                if (cached == null) repo.save(new MapChunkCache(dimKey, cx, cz, fresh));
                else { cached.setPng(fresh); repo.save(cached); }
                return fresh;
            }
        } catch (Exception e) {
            LOG.debug("chunk fetch fail {}/{}/{}: {}", dim, cx, cz, e.getMessage());
        }
        return cached == null ? null : cached.getPng();
    }
}

@RestController
@RequestMapping("/api/map-db")
class MapChunkController {
    private final MapChunkService svc;
    MapChunkController(MapChunkService svc) { this.svc = svc; }

    @GetMapping(value = "/chunk/{cx}/{cz}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> chunk(@PathVariable int cx, @PathVariable int cz,
                                         @RequestParam(defaultValue = "minecraft:overworld") String dim) {
        byte[] png = svc.getChunkPng(dim, cx, cz);
        if (png == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
