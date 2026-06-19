package br.com.murilo.liberthia.cosmic.scare;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * r175 → <b>r177</b>: catálogo de rostos do terror — agora suporta <b>JPEG</b>.
 *
 * <p>Bundled: {@code assets/liberthia/textures/gui/scare/face_1.jpg, face_2.jpg, ...}
 * (sequência; também aceita .png). Como o Minecraft NÃO decodifica JPEG no pipeline
 * de texturas, lemos os bytes via {@code ImageIO} (suporta JPEG/PNG), convertemos
 * pra {@link NativeImage} e registramos como {@link DynamicTexture}.
 *
 * <p>Externos: qualquer .png/.jpg/.jpeg em {@code <.minecraft>/liberthia_faces/} vira
 * rosto extra. {@code /liberthia scare reloadfaces} relê sem reiniciar.
 *
 * <p>Lido pelo {@link ScareClient} no FLASH e pelo Lurker.
 */
public final class FaceImages {

    private FaceImages() {}

    public record Face(ResourceLocation tex, int w, int h) {}

    private static final List<Face> FACES = new ArrayList<>();
    private static boolean loaded = false;
    private static int regCounter = 0;

    public static File dir() {
        return new File(Minecraft.getInstance().gameDirectory, "liberthia_faces");
    }

    /** Carrega (uma vez) bundled (JPEG/PNG) + externos. Render thread. */
    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        FACES.clear();

        // Bundled: face_1, face_2, ... (para na primeira que não existir)
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        for (int i = 1; i <= 256; i++) {
            ResourceLocation jpg = new ResourceLocation(LiberthiaMod.MODID, "textures/gui/scare/face_" + i + ".jpg");
            ResourceLocation png = new ResourceLocation(LiberthiaMod.MODID, "textures/gui/scare/face_" + i + ".png");
            Face f = null;
            if (rm.getResource(jpg).isPresent()) f = loadResource(rm, jpg, "bundle" + i);
            else if (rm.getResource(png).isPresent()) f = loadResource(rm, png, "bundle" + i);
            if (f == null) break;
            FACES.add(f);
        }

        // Externos (pasta do jogador)
        File d = dir();
        try { if (!d.exists()) d.mkdirs(); } catch (Exception ignored) {}
        File[] files = d.listFiles(fl -> {
            String n = fl.getName().toLowerCase();
            return fl.isFile() && (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg"));
        });
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (File fl : files) {
                try (FileInputStream in = new FileInputStream(fl)) {
                    Face f = loadStream(in, "ext");
                    if (f != null) FACES.add(f);
                } catch (Exception ex) {
                    LiberthiaMod.LOGGER.warn("[FaceImages] falhou ao ler {}: {}", fl.getName(), ex.toString());
                }
            }
        }
        LiberthiaMod.LOGGER.info("[FaceImages] {} rosto(s) carregado(s) ({} bundled).", FACES.size(), FACES.size());
    }

    private static Face loadResource(ResourceManager rm, ResourceLocation rl, String key) {
        try (InputStream in = rm.getResource(rl).get().open()) {
            return loadStream(in, key);
        } catch (Exception ex) {
            LiberthiaMod.LOGGER.warn("[FaceImages] falhou bundled {}: {}", rl, ex.toString());
            return null;
        }
    }

    /** Decodifica via ImageIO (JPEG/PNG) → NativeImage RGBA → DynamicTexture registrada. */
    private static Face loadStream(InputStream in, String key) throws Exception {
        BufferedImage bi = ImageIO.read(in);
        if (bi == null) return null;
        int w = bi.getWidth(), h = bi.getHeight();
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = bi.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, gg = (argb >> 8) & 0xFF, b = argb & 0xFF;
                img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (gg << 8) | r); // NativeImage = ABGR
            }
        }
        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation rl = Minecraft.getInstance().getTextureManager()
                .register("liberthia_face_" + key + "_" + (regCounter++), tex);
        return new Face(rl, w, h);
    }

    public static synchronized void reload() {
        loaded = false;
        ensureLoaded();
    }

    public static int count() {
        ensureLoaded();
        return Math.max(1, FACES.size());
    }

    /** Rosto pelo índice 1-based (com wrap). Nunca null. */
    public static Face get(int index1Based) {
        ensureLoaded();
        if (FACES.isEmpty()) {
            return new Face(new ResourceLocation(LiberthiaMod.MODID, "textures/gui/scare/face_1.jpg"), 256, 256);
        }
        return FACES.get(Math.floorMod(index1Based - 1, FACES.size()));
    }
}
