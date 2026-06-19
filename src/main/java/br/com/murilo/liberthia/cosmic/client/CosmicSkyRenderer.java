package br.com.murilo.liberthia.cosmic.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * v0.1.22 r35: <b>Sky Rift Renderer + Eyes in Sky</b>.
 *
 * <p>Hook em {@link RenderLevelStageEvent} stage AFTER_SKY — desenha quads
 * direto no céu pra simular "rasgo dimensional" + olhos flutuantes em volta.
 *
 * <h2>Sky Rift</h2>
 * Cone de quads roxos translúcidos desenhados em volta do player com
 * scale baseado em {@link CosmicClientState#currentSkyRiftAlpha}. Posição
 * elevada (sky dome), shape vertical comprido (rasgo).
 *
 * <h2>Eyes in Sky</h2>
 * {@link CosmicClientState#currentEyesCount} olhos flutuando em órbita
 * lenta em volta do player a alta altitude. Cada olho = quad com texture
 * eye (procedural via cor sólida + shape).
 *
 * <p>OPTIMIZATION: renderiza apenas se {@link CosmicClientState#isActive()}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class CosmicSkyRenderer {

    private static final Random RNG = new Random();

    private CosmicSkyRenderer() {}

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (!CosmicClientState.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float skyAlpha = CosmicClientState.currentSkyRiftAlpha;
        int eyesCount = CosmicClientState.currentEyesCount;
        if (skyAlpha < 0.01F && eyesCount == 0) return;

        PoseStack pose = event.getPoseStack();
        var cam = event.getCamera();
        float partial = event.getPartialTick();
        double camX = cam.getPosition().x;
        double camY = cam.getPosition().y;
        double camZ = cam.getPosition().z;

        pose.pushPose();
        pose.translate(-camX, -camY, -camZ);

        // Render sky rift (cone vertical)
        if (skyAlpha > 0.01F) {
            renderSkyRift(pose, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    skyAlpha);
        }

        // Render eyes in sky
        if (eyesCount > 0) {
            renderEyesInSky(pose, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    eyesCount, cam);
        }

        pose.popPose();
        RenderSystem.disableBlend();
    }

    /** Renderiza um rasgo VERTICAL roxo no céu. */
    private static void renderSkyRift(PoseStack pose, double px, double py, double pz,
                                       float alpha) {
        pose.pushPose();
        pose.translate(px, py + 80, pz); // sky height
        // Anim rotation
        float t = CosmicClientState.animTime;
        pose.mulPose(Axis.YP.rotationDegrees(t * 5));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f mat = pose.last().pose();
        // 6 quads radiais formando "cone" vertical rasgado
        float width = 8 + (float) Math.sin(t * 0.3) * 2;
        float height = 60;
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI / 3);
            float dx = (float) Math.cos(angle) * width;
            float dz = (float) Math.sin(angle) * width;
            // Quad vertical no eixo radial
            int r = 80;
            int gg = 0;
            int b = 180;
            int a = (int) (alpha * 200);
            // top
            buf.vertex(mat, dx * 0.3F, height, dz * 0.3F).color(r, gg, b, 0).endVertex();
            buf.vertex(mat, dx * 0.3F, height - 10, dz * 0.3F).color(r, gg, b, a / 3).endVertex();
            // bottom
            buf.vertex(mat, dx, 0, dz).color(r, gg, b, a).endVertex();
            buf.vertex(mat, dx, height * 0.5F, dz).color(r, gg, b, a / 2).endVertex();
        }
        tess.end();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    /** Renderiza N olhos brancos pulsando no céu em órbita. */
    private static void renderEyesInSky(PoseStack pose, double px, double py, double pz,
                                          int count, net.minecraft.client.Camera cam) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float t = CosmicClientState.animTime;

        for (int i = 0; i < count; i++) {
            double angle = (t * 0.05) + i * (Math.PI * 2 / Math.max(1, count));
            double r = 50 + Math.sin(t * 0.3 + i) * 8;
            double ex = px + Math.cos(angle) * r;
            double ey = py + 35 + Math.sin(t * 0.2 + i) * 5;
            double ez = pz + Math.sin(angle) * r;

            // Pulsing alpha
            float pulse = (float) (0.5 + Math.sin(t * 0.8 + i * 2) * 0.5);
            int a = (int) (pulse * 220);

            // Eye = quad branco com "pupila" central preta
            float size = 2.5F + pulse * 1.5F;
            // Outer white quad
            drawBillboardQuad(buf, pose.last().pose(), (float) (ex - px), (float) (ey - py),
                    (float) (ez - pz), size, 255, 255, 240, a, cam);
            // Inner black pupil
            drawBillboardQuad(buf, pose.last().pose(), (float) (ex - px), (float) (ey - py),
                    (float) (ez - pz), size * 0.4F, 0, 0, 0, a, cam);
        }
        tess.end();
        RenderSystem.enableDepthTest();
    }

    private static void drawBillboardQuad(BufferBuilder buf, Matrix4f mat,
                                            float cx, float cy, float cz, float size,
                                            int r, int g, int b, int a,
                                            net.minecraft.client.Camera cam) {
        // Simple billboarded quad — facing camera approximately
        float h = size / 2;
        buf.vertex(mat, cx - h, cy - h, cz).color(r, g, b, a).endVertex();
        buf.vertex(mat, cx + h, cy - h, cz).color(r, g, b, a).endVertex();
        buf.vertex(mat, cx + h, cy + h, cz).color(r, g, b, a).endVertex();
        buf.vertex(mat, cx - h, cy + h, cz).color(r, g, b, a).endVertex();
    }
}
