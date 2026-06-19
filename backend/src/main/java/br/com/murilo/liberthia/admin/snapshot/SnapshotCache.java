package br.com.murilo.liberthia.admin.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Cache local de snapshots no backend. Quando um snapshot é fetched do mod,
 * salvamos uma cópia em ./liberthia_snapshot_cache/{uuid}/{ts}.json.
 *
 * Vantagem: se o servidor MC perder dados ou ficar offline, o backend ainda
 * consegue servir snapshots históricos pra restore.
 */
@Component
public class SnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(SnapshotCache.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private Path baseDir;

    @PostConstruct
    public void init() {
        baseDir = Paths.get("liberthia_snapshot_cache").toAbsolutePath();
        try { Files.createDirectories(baseDir); }
        catch (IOException e) { log.warn("[SnapCache] mkdir falhou: {}", e.getMessage()); }
        log.info("[SnapCache] cache dir: {}", baseDir);
    }

    public void store(String uuid, long ts, JsonNode snapshot) {
        try {
            Path dir = baseDir.resolve(uuid);
            Files.createDirectories(dir);
            Path file = dir.resolve(ts + ".json");
            mapper.writeValue(file.toFile(), snapshot);
        } catch (Exception e) {
            log.debug("[SnapCache] store falhou: {}", e.getMessage());
        }
    }

    public JsonNode read(String uuid, long ts) {
        try {
            Path file = baseDir.resolve(uuid).resolve(ts + ".json");
            if (!Files.exists(file)) return null;
            return mapper.readTree(file.toFile());
        } catch (Exception e) {
            return null;
        }
    }

    public List<Long> list(String uuid) {
        Path dir = baseDir.resolve(uuid);
        if (!Files.isDirectory(dir)) return List.of();
        List<Long> result = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.forEach(p -> {
                String name = p.getFileName().toString();
                if (name.endsWith(".json")) {
                    try { result.add(Long.parseLong(name.substring(0, name.length() - 5))); }
                    catch (Exception ignored) {}
                }
            });
        } catch (Exception ignored) {}
        result.sort(Long::compare);
        return result;
    }

    public Path baseDir() { return baseDir; }
}
