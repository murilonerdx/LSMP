package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.storage.PotionJarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.joml.Matrix4f;

/**
 * r173: <b>Potion Jar BER</b> — renderiza o líquido <b>animado</b> enchendo por
 * dentro do jarro 3D.
 *
 * <p>Quando o jarro guarda <b>Source</b>, o líquido é roxo mágico e <b>brilha</b>
 * (full-bright). A superfície ondula suavemente (seno em função do gameTime) e a
 * cor pulsa de leve — dá a sensação de Source vivo enchendo o vidro. Se em vez de
 * Source houver uma poção armazenada, usa a cor da poção (compat com o uso antigo).
 *
 * <p>Mesma técnica do {@link MatterTankRenderer}: quads das paredes + topo, altura
 * proporcional ao fillage. O jarro é renderizado na layer translucent (vidro).
 */
public class PotionJarRenderer implements BlockEntityRenderer<PotionJarBlockEntity> {

    // Região interna do corpo do jarro (modelo: body de [3,2,3] a [13,11,13]).
    private static final float XZ_MIN = 0.27f;
    private static final float XZ_MAX = 0.73f;
    private static final float Y_MIN  = 0.15f;
    private static final float Y_MAX  = 0.63f;

    private static final int SOURCE_COLOR = 0x9B3CE6; // roxo Source vivo

    public PotionJarRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PotionJarBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        // Decide conteúdo: Source tem prioridade.
        int srcAmt = be.getSource();
        int color;
        int capacity;
        int amount;
        boolean glow;
        if (srcAmt > 0) {
            color = SOURCE_COLOR;
            capacity = be.getSourceCapacity();
            amount = srcAmt;
            glow = true;
        } else if (be.getAmount() > 0 && be.getPotion() != Potions.EMPTY) {
            color = PotionUtils.getColor(be.getPotion());
            capacity = PotionJarBlockEntity.CAPACITY;
            amount = be.getAmount();
            glow = false;
        } else {
            return; // vazio — nada a desenhar
        }

        float pct = Math.min(1.0f, (float) amount / (float) capacity);
        if (pct <= 0.001f) return;

        // Animação: tempo contínuo p/ ondular a superfície e pulsar a cor.
        double time = (be.getLevel() == null ? 0 : be.getLevel().getGameTime()) + partialTick;

        float baseTop = Y_MIN + (Y_MAX - Y_MIN) * pct;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        // pulso de brilho sutil
        float pulse = glow ? (0.85f + 0.15f * (float) Math.sin(time * 0.12)) : 1.0f;
        r = Math.min(1f, r * pulse);
        g = Math.min(1f, g * pulse);
        b = Math.min(1f, b * pulse);
        float a = glow ? 0.80f : 0.72f;

        int light = glow ? 0xF000F0 : packedLight; // Source brilha no escuro

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
        pose.pushPose();
        Matrix4f m = pose.last().pose();

        float x1 = XZ_MIN, x2 = XZ_MAX, z1 = XZ_MIN, z2 = XZ_MAX;
        float yB = Y_MIN;

        // Ondulação da superfície: 4 cantos com fase distinta (amplitude minúscula).
        float amp = 0.012f;
        float yTL = baseTop + amp * (float) Math.sin(time * 0.15 + 0.0);
        float yTR = baseTop + amp * (float) Math.sin(time * 0.15 + 1.6);
        float yBR = baseTop + amp * (float) Math.sin(time * 0.15 + 3.2);
        float yBL = baseTop + amp * (float) Math.sin(time * 0.15 + 4.8);

        // TOP (superfície ondulada, brilho cheio)
        quad(vc, m, x1, yTL, z1, x2, yTR, z1, x2, yBR, z2, x1, yBL, z2, r, g, b, a, light);

        // Paredes laterais (um pouco mais escuras pra dar volume)
        float r2 = r * 0.82f, g2 = g * 0.82f, b2 = b * 0.82f;
        // NORTH (z=z1)
        quad(vc, m, x1, yB, z1, x1, yTL, z1, x2, yTR, z1, x2, yB, z1, r2, g2, b2, a, light);
        // SOUTH (z=z2)
        quad(vc, m, x2, yB, z2, x2, yBR, z2, x1, yBL, z2, x1, yB, z2, r2, g2, b2, a, light);
        // WEST (x=x1)
        quad(vc, m, x1, yB, z2, x1, yBL, z2, x1, yTL, z1, x1, yB, z1, r2, g2, b2, a, light);
        // EAST (x=x2)
        quad(vc, m, x2, yB, z1, x2, yTR, z1, x2, yBR, z2, x2, yB, z2, r2, g2, b2, a, light);

        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float r, float g, float b, float a, int light) {
        vertex(vc, m, x1, y1, z1, r, g, b, a, light);
        vertex(vc, m, x2, y2, z2, r, g, b, a, light);
        vertex(vc, m, x3, y3, z3, r, g, b, a, light);
        vertex(vc, m, x4, y4, z4, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z,
                               float r, float g, float b, float a, int light) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(0, 0)
                .overlayCoords(0).uv2(light).normal(0, 1, 0).endVertex();
    }
}
