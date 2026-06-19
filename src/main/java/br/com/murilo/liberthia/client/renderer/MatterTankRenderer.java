package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.block.entity.MatterTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4f;

/**
 * BlockEntityRenderer pro Matter Tank — desenha um cubo de fluido com altura
 * proporcional ao fillage dentro do bloco de vidro. Cor depende do tipo de matter:
 *
 * <ul>
 *   <li>Dark Matter → violeta escuro (#5a2080, alpha 220)</li>
 *   <li>Clear Matter → branco perolado (#e0e8ff, alpha 200)</li>
 *   <li>Yellow Matter → dourado quente (#ffd040, alpha 220)</li>
 * </ul>
 *
 * <p><b>v0.1.39</b>: substitui o sistema antigo de blockstate level 0-4. User
 * reclamou que "mudava de modelo de bloco" — agora renderiza o líquido REAL
 * dentro do cubo, com altura contínua.
 */
public class MatterTankRenderer implements BlockEntityRenderer<MatterTankBlockEntity> {

    /** Margem interna do cubo de fluido — não cola nas paredes de vidro. */
    private static final float MARGIN = 0.08f;
    /** Y mínimo do fluido (acima da base de metal). */
    private static final float Y_MIN = 0.08f;
    /** Y máximo do fluido (abaixo do topo de metal). */
    private static final float Y_MAX = 0.92f;

    public MatterTankRenderer(BlockEntityRendererProvider.Context ctx) {
        // Sem deps — renderer puro.
    }

    @Override
    public void render(MatterTankBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getTank() == null) return;
        int amount = be.getTank().getFluidAmount();
        if (amount <= 0) return;
        int capacity = be.getTank().getCapacity();
        if (capacity <= 0) return;

        // Calcula altura proporcional do fluido
        float pct = (float) amount / (float) capacity;
        float height = Y_MIN + (Y_MAX - Y_MIN) * pct;

        // Determina cor pelo tipo de fluido
        Fluid fluid = be.getTank().getFluid().getFluid();
        int color = colorForFluid(fluid);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        // Renderiza cubo do fluido — quads das 4 paredes laterais + topo.
        // Base não precisa (player não vê de baixo normalmente).
        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());
        pose.pushPose();
        Matrix4f matrix = pose.last().pose();

        float x1 = MARGIN, x2 = 1.0f - MARGIN;
        float z1 = MARGIN, z2 = 1.0f - MARGIN;
        float yBottom = Y_MIN;
        float yTop = height;

        // TOP (face superior do fluido — visível através do vidro)
        quad(vc, matrix, x1, yTop, z1, x2, yTop, z1, x2, yTop, z2, x1, yTop, z2,
                r, g, b, a, packedLight);

        // 4 paredes laterais (mesma cor, ligeiramente mais escura pra sensação de volume)
        float r2 = r * 0.85f, g2 = g * 0.85f, b2 = b * 0.85f;
        // NORTH face (z = z1)
        quad(vc, matrix, x1, yBottom, z1, x1, yTop, z1, x2, yTop, z1, x2, yBottom, z1,
                r2, g2, b2, a, packedLight);
        // SOUTH face (z = z2)
        quad(vc, matrix, x2, yBottom, z2, x2, yTop, z2, x1, yTop, z2, x1, yBottom, z2,
                r2, g2, b2, a, packedLight);
        // WEST face (x = x1)
        quad(vc, matrix, x1, yBottom, z2, x1, yTop, z2, x1, yTop, z1, x1, yBottom, z1,
                r2, g2, b2, a, packedLight);
        // EAST face (x = x2)
        quad(vc, matrix, x2, yBottom, z1, x2, yTop, z1, x2, yTop, z2, x2, yBottom, z2,
                r2, g2, b2, a, packedLight);

        pose.popPose();
    }

    /** Desenha um quad translúcido com 4 vertices + cor (rgba). */
    private static void quad(VertexConsumer vc, Matrix4f matrix,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3,
                              float x4, float y4, float z4,
                              float r, float g, float b, float a, int light) {
        vertex(vc, matrix, x1, y1, z1, r, g, b, a, light);
        vertex(vc, matrix, x2, y2, z2, r, g, b, a, light);
        vertex(vc, matrix, x3, y3, z3, r, g, b, a, light);
        vertex(vc, matrix, x4, y4, z4, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f matrix,
                                float x, float y, float z,
                                float r, float g, float b, float a, int light) {
        vc.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .uv(0, 0)
                .overlayCoords(0)
                .uv2(light)
                .normal(0, 1, 0)
                .endVertex();
    }

    /** Cor ARGB pra cada tipo de fluido. Default cinza translúcido se desconhecido. */
    private static int colorForFluid(Fluid fluid) {
        var modFluids = br.com.murilo.liberthia.registry.ModFluids.class;
        try {
            if (fluid == br.com.murilo.liberthia.registry.ModFluids.DARK_MATTER.get()
                    || fluid == br.com.murilo.liberthia.registry.ModFluids.FLOWING_DARK_MATTER.get()) {
                return 0xDC5A2080; // ARGB: violeta escuro
            }
            if (fluid == br.com.murilo.liberthia.registry.ModFluids.CLEAR_MATTER.get()
                    || fluid == br.com.murilo.liberthia.registry.ModFluids.FLOWING_CLEAR_MATTER.get()) {
                return 0xC8E0E8FF; // ARGB: branco perolado
            }
            if (fluid == br.com.murilo.liberthia.registry.ModFluids.YELLOW_MATTER.get()
                    || fluid == br.com.murilo.liberthia.registry.ModFluids.FLOWING_YELLOW_MATTER.get()) {
                return 0xDCFFD040; // ARGB: dourado
            }
        } catch (Throwable ignored) {}
        return 0xA0808080;
    }
}
