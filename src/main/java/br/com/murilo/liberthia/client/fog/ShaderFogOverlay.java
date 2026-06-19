package br.com.murilo.liberthia.client.fog;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r175: <b>Fog compatível com shaders</b>.
 *
 * <p>Quando um shaderpack (Oculus/Iris) está ativo, ele <b>substitui todo o
 * pipeline de fog do vanilla</b> — então a neblina de terror feita via
 * {@link net.minecraftforge.client.event.ViewportEvent.RenderFog} (planos
 * near/far) é ignorada e "some". Aqui detectamos shaders ativos e, nesse caso,
 * desenhamos a neblina como um <b>haze de tela cheia</b> (tinta + vinheta) que é
 * renderizado POR CIMA da saída do shader — sobrevivendo ao shaderpack.
 *
 * <p>Sem shaders, este overlay NÃO desenha (o fog real por profundidade já é
 * melhor), evitando tinta dupla.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShaderFogOverlay {

    private ShaderFogOverlay() {}

    private static final int COSMIC_COLOR = 0x4C0D8C; // roxo cósmico

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.options.hideGui) return;
        if (!shadersActive()) return; // sem shaders → fog vanilla já cuida

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        String dim = mc.level.dimension().location().toString();

        // Combina fog de zona + pessoal + cósmico — pega o mais forte.
        float strength = 0F;
        int color = COSMIC_COLOR;

        ClientFogZones.Result zone = ClientFogZones.compute(cam.x, cam.y, cam.z, dim);
        if (zone != null && zone.strength > strength) { strength = zone.strength; color = zone.color; }
        if (ClientPersonalFog.isActive() && ClientPersonalFog.density() > strength) {
            strength = ClientPersonalFog.density();
            color = ClientPersonalFog.color();
        }
        if (CosmicClientState.currentFogIntensity > strength) {
            strength = CosmicClientState.currentFogIntensity;
            color = COSMIC_COLOR;
        }

        strength = Math.max(0F, Math.min(1F, strength));
        if (strength <= 0.02F) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();

        int cr = (color >> 16) & 0xFF;
        int cg = (color >> 8) & 0xFF;
        int cb = color & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1) tinta base de tela cheia (haze) — alpha cresce com a densidade
        int baseA = (int) (strength * 0.55F * 255);
        g.fill(0, 0, w, h, (baseA << 24) | (cr << 16) | (cg << 8) | cb);

        // 2) vinheta "fechando" — bandas mais densas no topo e na base
        int edgeA = (int) (Math.min(1F, strength * 1.2F) * 255);
        int edge = (edgeA << 24) | (cr << 16) | (cg << 8) | cb;
        int clear = (cr << 16) | (cg << 8) | cb; // alpha 0
        int band = (int) (h * (0.30F + strength * 0.25F));
        g.fillGradient(0, 0, w, band, edge, clear);          // topo: denso → limpo
        g.fillGradient(0, h - band, w, h, clear, edge);      // base: limpo → denso

        RenderSystem.disableBlend();
    }

    // ── Detecção de shaderpack ativo (Iris/Oculus) via reflexão ───────────
    private static Boolean available = null;
    private static java.lang.reflect.Method mGetInstance;
    private static java.lang.reflect.Method mInUse;

    private static boolean shadersActive() {
        try {
            if (available == null) {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                mGetInstance = api.getMethod("getInstance");
                mInUse = api.getMethod("isShaderPackInUse");
                available = Boolean.TRUE;
            }
            if (!available) return false;
            Object inst = mGetInstance.invoke(null);
            Object r = mInUse.invoke(inst);
            return (r instanceof Boolean b) && b;
        } catch (Throwable t) {
            available = Boolean.FALSE; // Iris/Oculus ausente → nunca tenta de novo
            return false;
        }
    }
}
