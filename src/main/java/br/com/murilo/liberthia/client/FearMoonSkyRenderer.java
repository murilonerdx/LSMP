package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Renderização client da <b>Lua do Medo</b> (lido de {@link ClientFearMoonState}):
 * <ul>
 *   <li><b>Lua vermelha</b> — desenha um disco da lua tingido com a cor escolhida
 *       POR CIMA da lua vanilla (RenderLevelStageEvent AFTER_SKY, replicando a
 *       transform do céu vanilla).</li>
 *   <li><b>Céu tingido</b> — puxa a cor da névoa/céu pra cor escolhida à noite
 *       (ViewportEvent.ComputeFogColor).</li>
 * </ul>
 * A cor vem do comando {@code /liberthia fearmoon color <r> <g> <b>}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FearMoonSkyRenderer {

    private FearMoonSkyRenderer() {}

    private static final ResourceLocation MOON_LOCATION =
            new ResourceLocation("textures/environment/moon_phases.png");

    @SubscribeEvent
    public static void onRenderSky(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (!ClientFearMoonState.active) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        float partial = event.getPartialTick();
        float star = level.getStarBrightness(partial); // 0 de dia, ~0.5 de noite
        if (star < 0.02F) return; // lua só de noite
        float rain = 1.0F - level.getRainLevel(partial);

        int c = ClientFearMoonState.color;
        float r = ((c >> 16) & 0xFF) / 255.0F;
        float g = ((c >> 8) & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        float alpha = Math.min(1.0F, star * 2.4F + 0.35F) * rain;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Transform do céu vanilla pra posição da lua
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partial) * 360.0F));
        Matrix4f mat = pose.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, MOON_LOCATION);
        RenderSystem.setShaderColor(r, g, b, alpha);

        int phase = level.getMoonPhase();
        int px = phase % 4;
        int py = phase / 4 % 2;
        float u0 = px / 4.0F, v0 = py / 2.0F, u1 = (px + 1) / 4.0F, v1 = (py + 1) / 2.0F;
        float sz = 20.5F; // levemente maior que a vanilla (20) pra cobri-la

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.vertex(mat, -sz, -100.0F, sz).uv(u1, v1).endVertex();
        buf.vertex(mat, sz, -100.0F, sz).uv(u0, v1).endVertex();
        buf.vertex(mat, sz, -100.0F, -sz).uv(u0, v0).endVertex();
        buf.vertex(mat, -sz, -100.0F, -sz).uv(u1, v0).endVertex();
        tess.end();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    /** Tinge o céu/névoa em direção à cor da Lua do Medo, mais forte à noite. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (!ClientFearMoonState.active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float star = mc.level.getStarBrightness((float) e.getPartialTick());
        float blend = Math.min(0.5F, star * 1.2F); // tint só de noite, até ~50%
        if (blend <= 0.01F) return;

        int c = ClientFearMoonState.color;
        float tr = ((c >> 16) & 0xFF) / 255.0F;
        float tg = ((c >> 8) & 0xFF) / 255.0F;
        float tb = (c & 0xFF) / 255.0F;
        e.setRed(e.getRed() * (1 - blend) + tr * blend);
        e.setGreen(e.getGreen() * (1 - blend) + tg * blend);
        e.setBlue(e.getBlue() * (1 - blend) + tb * blend);
    }
}
