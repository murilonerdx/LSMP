package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.block.MatterReactorBlock;
import br.com.murilo.liberthia.block.entity.MatterReactorBlockEntity;
import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * r181 — renderer do <b>Reator de Matéria Escura</b>: desenha um ORBE de matéria escura
 * (item active_dark_matter) flutuando, balançando e GIRANDO por cima do bloco, com brilho
 * total. Quando o reator está aceso (queimando), o orbe gira mais rápido. Client-only —
 * registrado no {@code ClientModEvents} (RegisterRenderers); nunca carregado no servidor.
 */
public class MatterReactorRenderer implements BlockEntityRenderer<MatterReactorBlockEntity> {

    private final ItemStack orb = new ItemStack(ModItems.ACTIVE_DARK_MATTER.get());

    public MatterReactorRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(MatterReactorBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (be.getLevel() == null) return;
        boolean lit = be.getBlockState().hasProperty(MatterReactorBlock.LIT) && be.getBlockState().getValue(MatterReactorBlock.LIT);

        float time = (float) (be.getLevel().getGameTime() % 100000L) + partialTick;
        float bob = (float) Math.sin(time * 0.08F) * 0.05F;
        float spin = time * (lit ? 6.0F : 2.0F);
        float scale = lit ? 0.62F : 0.5F;
        int light = lit ? 0xF000F0 : packedLight;

        pose.pushPose();
        pose.translate(0.5D, 1.28D + bob, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.mulPose(Axis.XP.rotationDegrees(22.5F));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                orb, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                pose, buffer, be.getLevel(), 0);
        pose.popPose();
    }

    /** Sempre renderiza (orbe visível mesmo a alguma distância). */
    @Override public boolean shouldRenderOffScreen(MatterReactorBlockEntity be) { return false; }
    @Override public int getViewDistance() { return 96; }
}
