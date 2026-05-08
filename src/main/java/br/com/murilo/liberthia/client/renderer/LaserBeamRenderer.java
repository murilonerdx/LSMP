package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.block.entity.LaserEmitterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderiza um feixe roxo pra cada direção ativa do laser.
 *
 * <p>Estratégia de transformação (importante porque {@link BeaconRenderer#renderBeaconBeam}
 * adiciona um translate(0.5, 0, 0.5) interno para centrar a coluna):
 * <ol>
 *   <li>Translada para o CENTRO da face do bloco em coordenadas locais (0..1).</li>
 *   <li>Rotaciona para que o eixo +Y local aponte na direção {@code d}.</li>
 *   <li>Translada (-0.5, 0, -0.5) em LOCAL — cancela o offset interno do BeaconRenderer.</li>
 *   <li>Chama o BeaconRenderer.</li>
 * </ol>
 * Resultado: o feixe começa exatamente no centro da face do bloco e estende
 * {@code length} blocos para fora.
 */
public class LaserBeamRenderer implements BlockEntityRenderer<LaserEmitterBlockEntity> {

    private static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation("textures/entity/beacon_beam.png");
    private static final float[] COLOR = { 0.85f, 0.30f, 1.00f };

    public LaserBeamRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override public boolean shouldRenderOffScreen(LaserEmitterBlockEntity be) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void render(LaserEmitterBlockEntity be, float partial, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        if (be.getActiveBeams().isEmpty()) return;
        long gameTime = be.getLevel() == null ? 0 : be.getLevel().getGameTime();

        for (var entry : be.getActiveBeams().entrySet()) {
            Direction d = entry.getKey();
            int length = entry.getValue();
            if (length <= 0) continue;

            ps.pushPose();
            // 1. Translada para o centro da face (em coords locais do bloco 0..1)
            float[] face = faceCenter(d);
            ps.translate(face[0], face[1], face[2]);
            // 2. Rotaciona para que +Y local aponte em d
            rotateForFacing(ps, d);
            // 3. Cancela o offset interno (0.5, 0, 0.5) do BeaconRenderer
            ps.translate(-0.5, 0, -0.5);
            // 4. Renderiza
            BeaconRenderer.renderBeaconBeam(
                    ps, buf, BEAM_TEXTURE,
                    partial, 1.0f,
                    gameTime,
                    0, length,
                    COLOR,
                    0.18f, 0.30f);

            ps.popPose();
        }
    }

    /** Centro da face em coords locais (0..1) do bloco. */
    private static float[] faceCenter(Direction d) {
        return switch (d) {
            case UP    -> new float[]{0.5f, 1.0f, 0.5f};
            case DOWN  -> new float[]{0.5f, 0.0f, 0.5f};
            case NORTH -> new float[]{0.5f, 0.5f, 0.0f};
            case SOUTH -> new float[]{0.5f, 0.5f, 1.0f};
            case EAST  -> new float[]{1.0f, 0.5f, 0.5f};
            case WEST  -> new float[]{0.0f, 0.5f, 0.5f};
        };
    }

    /** Rotaciona PoseStack para que +Y local aponte em d. */
    private static void rotateForFacing(PoseStack ps, Direction d) {
        switch (d) {
            case UP    -> { /* já é +Y */ }
            case DOWN  -> ps.mulPose(Axis.XP.rotationDegrees(180f));
            case NORTH -> ps.mulPose(Axis.XP.rotationDegrees(-90f));
            case SOUTH -> ps.mulPose(Axis.XP.rotationDegrees(90f));
            case EAST  -> ps.mulPose(Axis.ZP.rotationDegrees(-90f));
            case WEST  -> ps.mulPose(Axis.ZP.rotationDegrees(90f));
        }
    }
}
