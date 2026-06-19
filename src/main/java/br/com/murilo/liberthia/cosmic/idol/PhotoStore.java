package br.com.murilo.liberthia.cosmic.idol;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * CLIENT-only: armazena/recupera as fotos da {@link CameraItem}.
 *
 * <p><b>Captura</b> ({@link #capture}): lê o framebuffer principal do jogo (o que está
 * na tela), recorta um quadrado central, reduz pra 128×128, salva como PNG em
 * {@code <.minecraft>/liberthia_photos/<id>.png}. Roda na render thread (acesso a GL).
 *
 * <p><b>Resolução</b> ({@link #texture}): dado um id, carrega o PNG (lazy) como
 * {@link DynamicTexture} via ImageIO→NativeImage (mesma técnica de {@code FaceImages}),
 * cacheia, e devolve a {@link ResourceLocation} pro {@code PhotographItemDecorator}
 * desenhar no slot. Funciona entre sessões (a foto fica salva no disco).
 */
public final class PhotoStore {

    private PhotoStore() {}

    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final Map<String, int[]> DIMS = new HashMap<>(); // id -> [w,h]
    private static final ResourceLocation MISSING =
            new ResourceLocation(LiberthiaMod.MODID, "textures/item/photograph.png");

    /** Largura/altura da textura da foto (default 128; 16 pro fallback). */
    public static int texWidth(String id) {
        int[] d = DIMS.get(id);
        return d != null ? d[0] : (new File(dir(), id + ".png").isFile() ? 128 : 16);
    }
    public static int texHeight(String id) {
        int[] d = DIMS.get(id);
        return d != null ? d[1] : (new File(dir(), id + ".png").isFile() ? 128 : 16);
    }

    public static File dir() {
        File d = new File(Minecraft.getInstance().gameDirectory, "liberthia_photos");
        if (!d.exists()) { try { d.mkdirs(); } catch (Exception ignored) {} }
        return d;
    }

    // r186: captura "estilo F1" — esconde HUD/mãos por 1 frame antes de ler o framebuffer.
    private static volatile String pendingId = null;
    private static boolean prevHideGui = false;
    private static int pendingDelay = 0;
    /** Resolução de saída da foto (quadrada). Maior = mais nítida. */
    public static int captureResolution = 512;

    /**
     * PEDE uma captura: esconde a GUI (como F1) e marca pendente. A leitura real do
     * framebuffer (já SEM HUD/mãos) acontece no fim do próximo frame, em {@link #onRenderTickEnd}.
     * Roda na render thread (Minecraft.execute garante).
     */
    public static boolean capture(String id) {
        Minecraft mc = Minecraft.getInstance();
        prevHideGui = mc.options.hideGui;
        mc.options.hideGui = true;   // igual F1: sem HUD, sem inventário, sem mãos
        pendingId = id;
        pendingDelay = 1;            // espera 1 frame limpo antes de ler
        return true;
    }

    /** Chamado em RenderTickEvent.END por {@code PhotoCaptureClientHandler}. */
    public static void onRenderTickEnd() {
        if (pendingId == null) return;
        if (pendingDelay-- > 0) return;
        String id = pendingId;
        pendingId = null;
        try {
            doCapture(id);
        } finally {
            Minecraft.getInstance().options.hideGui = prevHideGui; // restaura a HUD
        }
    }

    /** Lê o framebuffer (já sem HUD) e salva como PNG em alta resolução. Render thread. */
    private static boolean doCapture(String id) {
        try {
            Minecraft mc = Minecraft.getInstance();
            RenderTarget fb = mc.getMainRenderTarget();
            int fw = fb.width, fh = fb.height;
            if (fw <= 0 || fh <= 0) return false;

            NativeImage full = new NativeImage(fw, fh, false);
            fb.bindRead();
            full.downloadTexture(0, false);
            fb.unbindRead();

            // recorta quadrado central e amostra pra captureResolution (Y invertido)
            int side = Math.min(fw, fh);
            int ox = (fw - side) / 2, oy = (fh - side) / 2;
            int out = Math.max(64, Math.min(captureResolution, side)); // não passa da resolução nativa
            BufferedImage bi = new BufferedImage(out, out, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < out; y++) {
                for (int x = 0; x < out; x++) {
                    int sx = Math.min(fw - 1, ox + (int) ((long) x * side / out));
                    int sy = Math.min(fh - 1, oy + (int) ((long) y * side / out));
                    int abgr = full.getPixelRGBA(sx, fh - 1 - sy); // NativeImage = ABGR, flip Y
                    int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
                    bi.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }
            full.close();

            File f = new File(dir(), id + ".png");
            ImageIO.write(bi, "PNG", f);
            CACHE.remove(id); // força recarregar a textura nova
            DIMS.remove(id);
            return true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[PhotoStore] capture falhou: {}", t.toString());
            return false;
        }
    }

    /** Textura da foto (lazy-load do PNG). Fallback no ícone genérico se não existir. */
    public static ResourceLocation texture(String id) {
        if (id == null || id.isEmpty()) return MISSING;
        ResourceLocation cached = CACHE.get(id);
        if (cached != null) return cached;
        File f = new File(dir(), id + ".png");
        if (!f.isFile()) return MISSING;
        try {
            BufferedImage bi = ImageIO.read(f);
            if (bi == null) return MISSING;
            int w = bi.getWidth(), h = bi.getHeight();
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = bi.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                    if (bi.getType() == BufferedImage.TYPE_INT_RGB) a = 0xFF;
                    img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r); // ABGR
                }
            }
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation rl = Minecraft.getInstance().getTextureManager()
                    .register("liberthia_photo_" + id, tex);
            CACHE.put(id, rl);
            DIMS.put(id, new int[]{w, h});
            return rl;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[PhotoStore] load {} falhou: {}", id, t.toString());
            return MISSING;
        }
    }
}
