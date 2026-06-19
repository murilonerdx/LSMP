package br.com.murilo.liberthia.admin.sounds;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Gerencia sons custom: salva .ogg em disco e gera resource pack ZIP que os
 * clientes do MC baixam pra ter acesso aos sons no /playsound.
 *
 * Estrutura no disco:
 *   ./liberthia_sounds/
 *     <namespace>/
 *       <key>.ogg
 *
 * O ZIP gerado tem:
 *   pack.mcmeta
 *   assets/<namespace>/sounds.json    (gerado dinamicamente)
 *   assets/<namespace>/sounds/<key>.ogg ...
 */
@Service
public class SoundsService {

    private static final Logger log = LoggerFactory.getLogger(SoundsService.class);
    private static final String PACK_DESCRIPTION = "Liberthia Custom Sounds";
    private static final int PACK_FORMAT = 15; // MC 1.20.x

    private Path baseDir;
    // Cache do zip pra evitar regerar a cada request; invalidado quando sons mudam
    private volatile byte[] zipCache;
    private volatile String zipSha1;

    @PostConstruct
    public void init() {
        baseDir = Paths.get("liberthia_sounds").toAbsolutePath();
        try { Files.createDirectories(baseDir); }
        catch (IOException e) { log.warn("[Sounds] mkdir falhou: {}", e.getMessage()); }
        log.info("[Sounds] dir: {}", baseDir);
        invalidate();
    }

    /** Salva um .ogg em disk. namespace e key são validados pra ser MC-safe. */
    public SoundEntry upload(String namespace, String key, InputStream data) throws IOException {
        String ns = sanitize(namespace, "liberthia");
        String k = sanitize(key, "sound_" + System.currentTimeMillis());
        Path dir = baseDir.resolve(ns);
        Files.createDirectories(dir);
        Path file = dir.resolve(k + ".ogg");
        Files.copy(data, file, StandardCopyOption.REPLACE_EXISTING);
        invalidate();
        long size = Files.size(file);
        log.info("[Sounds] saved {}:{} ({} bytes)", ns, k, size);
        return new SoundEntry(ns, k, size);
    }

    /** Lista todos sons (namespaces × keys). */
    public List<SoundEntry> list() {
        List<SoundEntry> out = new ArrayList<>();
        if (!Files.isDirectory(baseDir)) return out;
        try (var nsStream = Files.list(baseDir)) {
            nsStream.filter(Files::isDirectory).forEach(nsDir -> {
                String ns = nsDir.getFileName().toString();
                try (var fStream = Files.list(nsDir)) {
                    fStream.filter(p -> p.getFileName().toString().endsWith(".ogg"))
                            .forEach(p -> {
                                String name = p.getFileName().toString();
                                String k = name.substring(0, name.length() - 4);
                                try { out.add(new SoundEntry(ns, k, Files.size(p))); }
                                catch (IOException ignored) {}
                            });
                } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}
        out.sort(Comparator.comparing((SoundEntry e) -> e.namespace).thenComparing(e -> e.key));
        return out;
    }

    public boolean delete(String namespace, String key) {
        try {
            Path file = baseDir.resolve(sanitize(namespace, "liberthia")).resolve(sanitize(key, "") + ".ogg");
            boolean removed = Files.deleteIfExists(file);
            if (removed) invalidate();
            return removed;
        } catch (Exception e) { return false; }
    }

    /** Retorna o ZIP do resource pack (gera/cacheia). */
    public synchronized byte[] resourcePack() throws IOException {
        if (zipCache != null) return zipCache;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            // pack.mcmeta
            String meta = "{\"pack\":{\"pack_format\":" + PACK_FORMAT
                    + ",\"description\":\"" + PACK_DESCRIPTION + "\"}}";
            writeEntry(zip, "pack.mcmeta", meta.getBytes());

            // Agrupa sons por namespace
            Map<String, List<SoundEntry>> byNs = new HashMap<>();
            for (SoundEntry e : list()) byNs.computeIfAbsent(e.namespace, k -> new ArrayList<>()).add(e);

            for (var entry : byNs.entrySet()) {
                String ns = entry.getKey();
                List<SoundEntry> sounds = entry.getValue();

                // sounds.json
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                boolean first = true;
                for (SoundEntry s : sounds) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("\"").append(s.key).append("\":")
                            .append("{\"category\":\"master\",")
                            .append("\"sounds\":[{\"name\":\"").append(ns).append(":").append(s.key)
                            .append("\",\"stream\":true}]}");
                }
                sb.append("}");
                writeEntry(zip, "assets/" + ns + "/sounds.json", sb.toString().getBytes());

                // .ogg files
                for (SoundEntry s : sounds) {
                    Path p = baseDir.resolve(ns).resolve(s.key + ".ogg");
                    if (Files.exists(p)) {
                        writeEntry(zip, "assets/" + ns + "/sounds/" + s.key + ".ogg", Files.readAllBytes(p));
                    }
                }
            }
        }
        zipCache = baos.toByteArray();
        zipSha1 = sha1Hex(zipCache);
        log.info("[Sounds] resource pack rebuilt: {} bytes, sha1={}", zipCache.length, zipSha1);
        return zipCache;
    }

    public synchronized String sha1() throws IOException {
        if (zipSha1 == null) resourcePack();
        return zipSha1;
    }

    public synchronized void invalidate() {
        zipCache = null;
        zipSha1 = null;
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry e = new ZipEntry(name);
        zip.putNextEntry(e);
        zip.write(data);
        zip.closeEntry();
    }

    private static String sanitize(String s, String fallback) {
        if (s == null || s.isBlank()) return fallback;
        String r = s.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return r.isBlank() ? fallback : r;
    }

    private static String sha1Hex(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    public record SoundEntry(String namespace, String key, long sizeBytes) {}
}
