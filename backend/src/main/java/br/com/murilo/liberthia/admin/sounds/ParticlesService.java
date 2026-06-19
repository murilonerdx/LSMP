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
 * Gerencia overrides de texturas de partículas vanilla. Cada upload substitui
 * a textura de uma partícula vanilla específica (ex: "glow", "soul_fire_flame")
 * com a PNG enviada.
 *
 * Resource pack gerado tem estrutura:
 *   pack.mcmeta
 *   assets/minecraft/textures/particle/<override>.png
 *
 * Players precisam ter o pack carregado. Vanilla MC só permite OVERRIDE
 * — não dá pra adicionar NOVAS partículas sem mod, só substituir as existentes.
 */
@Service
public class ParticlesService {

    private static final Logger log = LoggerFactory.getLogger(ParticlesService.class);
    private static final String PACK_DESCRIPTION = "Liberthia Particle Overrides";
    private static final int PACK_FORMAT = 15;

    private Path baseDir;
    private volatile byte[] zipCache;
    private volatile String zipSha1;

    @PostConstruct
    public void init() {
        baseDir = Paths.get("liberthia_particles").toAbsolutePath();
        try { Files.createDirectories(baseDir); }
        catch (IOException e) { log.warn("[Particles] mkdir falhou: {}", e.getMessage()); }
        log.info("[Particles] dir: {}", baseDir);
        invalidate();
    }

    public ParticleEntry upload(String overrideKey, InputStream data) throws IOException {
        String k = sanitize(overrideKey, "glow");
        Path file = baseDir.resolve(k + ".png");
        Files.copy(data, file, StandardCopyOption.REPLACE_EXISTING);
        invalidate();
        long size = Files.size(file);
        log.info("[Particles] saved override:{} ({} bytes)", k, size);
        return new ParticleEntry(k, size);
    }

    public List<ParticleEntry> list() {
        List<ParticleEntry> out = new ArrayList<>();
        if (!Files.isDirectory(baseDir)) return out;
        try (var stream = Files.list(baseDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".png"))
                  .forEach(p -> {
                      String name = p.getFileName().toString();
                      String k = name.substring(0, name.length() - 4);
                      try { out.add(new ParticleEntry(k, Files.size(p))); }
                      catch (IOException ignored) {}
                  });
        } catch (IOException ignored) {}
        out.sort(Comparator.comparing(e -> e.key));
        return out;
    }

    public boolean delete(String key) {
        try {
            Path file = baseDir.resolve(sanitize(key, "") + ".png");
            boolean removed = Files.deleteIfExists(file);
            if (removed) invalidate();
            return removed;
        } catch (Exception e) { return false; }
    }

    /** PNG bytes pra preview no frontend. */
    public byte[] readPng(String key) throws IOException {
        Path file = baseDir.resolve(sanitize(key, "") + ".png");
        if (!Files.exists(file)) return null;
        return Files.readAllBytes(file);
    }

    public synchronized byte[] resourcePack() throws IOException {
        if (zipCache != null) return zipCache;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            String meta = "{\"pack\":{\"pack_format\":" + PACK_FORMAT
                    + ",\"description\":\"" + PACK_DESCRIPTION + "\"}}";
            writeEntry(zip, "pack.mcmeta", meta.getBytes());

            for (ParticleEntry e : list()) {
                Path p = baseDir.resolve(e.key + ".png");
                if (Files.exists(p)) {
                    writeEntry(zip, "assets/minecraft/textures/particle/" + e.key + ".png", Files.readAllBytes(p));
                }
            }
        }
        zipCache = baos.toByteArray();
        zipSha1 = sha1Hex(zipCache);
        log.info("[Particles] resource pack rebuilt: {} bytes", zipCache.length);
        return zipCache;
    }

    public synchronized String sha1() throws IOException {
        if (zipSha1 == null) resourcePack();
        return zipSha1;
    }

    public synchronized void invalidate() {
        zipCache = null; zipSha1 = null;
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

    public record ParticleEntry(String key, long sizeBytes) {}
}
