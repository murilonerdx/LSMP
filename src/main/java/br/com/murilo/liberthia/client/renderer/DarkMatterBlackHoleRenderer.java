package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.DarkMatterBlackHoleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** r186 — renderer mínimo: o visual do Buraco Negro vem todo das partículas (server). */
public class DarkMatterBlackHoleRenderer extends EntityRenderer<DarkMatterBlackHoleEntity> {
    public DarkMatterBlackHoleRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override public ResourceLocation getTextureLocation(DarkMatterBlackHoleEntity e) {
        return new ResourceLocation("liberthia", "textures/entity/dark_matter_black_hole.png");
    }

    @Override public void render(DarkMatterBlackHoleEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        super.render(e, yaw, pt, ps, buf, light);
    }
}
