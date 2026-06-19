package br.com.murilo.liberthia.admin.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Renderiza chunks como PNG top-down (16x16 px, escala = 1 bloco por px).
 * Usa o highest motion-blocking block + MapColor (mesma cor que vanilla maps).
 *
 * Concebido pra ser leve: cada PNG ~200-400 bytes; um chunk inteiro renderizado
 * em ~2ms no main thread.
 */
public final class AdminMapRenderer {

    private AdminMapRenderer() {}

    /**
     * Lista chunks visíveis: pega cada player na dimensão e enumera chunks
     * dentro do view-distance dele. É um superset do que está realmente
     * carregado, mas hasChunk() filtra os que não estão.
     */
    public static JsonObject loadedChunks(MinecraftServer server, String dim) {
        JsonObject result = new JsonObject();
        ServerLevel level = resolveLevel(server, dim);
        if (level == null) {
            result.addProperty("error", "dimension not found");
            return result;
        }
        int viewRadius = Math.max(2, Math.min(16, server.getPlayerList().getViewDistance()));
        java.util.Set<Long> seen = new java.util.HashSet<>();
        JsonArray arr = new JsonArray();
        int minCx = Integer.MAX_VALUE, minCz = Integer.MAX_VALUE;
        int maxCx = Integer.MIN_VALUE, maxCz = Integer.MIN_VALUE;
        for (var p : level.players()) {
            int pcx = (int) p.getX() >> 4;
            int pcz = (int) p.getZ() >> 4;
            for (int dx = -viewRadius; dx <= viewRadius; dx++) {
                for (int dz = -viewRadius; dz <= viewRadius; dz++) {
                    int cx = pcx + dx, cz = pcz + dz;
                    if (!level.hasChunk(cx, cz)) continue;
                    long key = ChunkPos.asLong(cx, cz);
                    if (!seen.add(key)) continue;
                    JsonObject c = new JsonObject();
                    c.addProperty("x", cx);
                    c.addProperty("z", cz);
                    arr.add(c);
                    minCx = Math.min(minCx, cx); minCz = Math.min(minCz, cz);
                    maxCx = Math.max(maxCx, cx); maxCz = Math.max(maxCz, cz);
                }
            }
        }
        result.add("chunks", arr);
        result.addProperty("dimension", level.dimension().location().toString());
        result.addProperty("viewRadius", viewRadius);
        if (arr.size() > 0) {
            JsonObject bb = new JsonObject();
            bb.addProperty("minCx", minCx);
            bb.addProperty("minCz", minCz);
            bb.addProperty("maxCx", maxCx);
            bb.addProperty("maxCz", maxCz);
            result.add("bbox", bb);
        }
        return result;
    }

    /** Renderiza chunk (cx, cz) como PNG bytes 16x16. Retorna null se não carregado. */
    public static byte[] renderChunk(MinecraftServer server, String dim, int cx, int cz) {
        ServerLevel level = resolveLevel(server, dim);
        if (level == null) return null;
        if (!level.hasChunk(cx, cz)) return null;
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return null;

        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int prevHeight = level.getMinBuildHeight() + 1;

        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int wx = cx * 16 + dx;
                int wz = cz * 16 + dz;
                int h = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, dx, dz);
                mp.set(wx, h, wz);
                BlockState bs = chunk.getBlockState(mp);
                MapColor mc = bs.getMapColor(level, mp);
                int rgb = mc.col;
                // Shading baseado na altura relativa ao vizinho do norte
                int shadeFactor = 1; // 0=darker, 1=normal, 2=lighter
                if (dz > 0) {
                    int hN = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, dx, dz - 1);
                    if (h > hN) shadeFactor = 2;
                    else if (h < hN) shadeFactor = 0;
                }
                int shaded = applyShade(rgb, shadeFactor);
                img.setRGB(dx, dz, shaded);
                prevHeight = h;
            }
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static int applyShade(int rgb, int factor) {
        if (factor == 1) return rgb;
        // factor 0 = 0.71x, factor 2 = 1.0x (vanilla map shading);
        // simplificado: 0 = 0.7, 2 = 1.0; pra garantir que normal (1) seja brilho médio,
        // usa-se 0.85 default e 0.7/1.0 nos extremos
        double mul = factor == 0 ? 0.65 : 1.0;
        int r = Math.min(255, (int) (((rgb >> 16) & 0xff) * mul));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xff) * mul));
        int b = Math.min(255, (int) (((rgb) & 0xff) * mul));
        return (r << 16) | (g << 8) | b;
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dim) {
        if (dim == null || dim.isBlank()) return server.overworld();
        ResourceLocation loc = ResourceLocation.tryParse(dim);
        if (loc == null) return null;
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, loc);
        return server.getLevel(key);
    }
}
