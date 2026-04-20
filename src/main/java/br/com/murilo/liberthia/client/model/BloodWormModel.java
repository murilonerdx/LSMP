package br.com.murilo.liberthia.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Custom segmented worm model — NOT a silverfish.
 * 5 body segments that wiggle sinusoidally. Single 32x16 texture atlas.
 */
public class BloodWormModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("liberthia", "blood_worm"), "main");

    private final ModelPart head;
    private final ModelPart seg1;
    private final ModelPart seg2;
    private final ModelPart seg3;
    private final ModelPart tail;

    public BloodWormModel(ModelPart root) {
        this.head = root.getChild("head");
        this.seg1 = root.getChild("seg1");
        this.seg2 = root.getChild("seg2");
        this.seg3 = root.getChild("seg3");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        // UV atlas 32x16. Each segment uses a slice.
        r.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -2F, -4F, 5F, 4F, 4F),
                PartPose.offset(0F, 22F, -4F));
        r.addOrReplaceChild("seg1", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-2F, -1.8F, -2F, 4F, 3.5F, 4F),
                PartPose.offset(0F, 22.2F, -1F));
        r.addOrReplaceChild("seg2", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-2F, -1.8F, -2F, 4F, 3.5F, 4F),
                PartPose.offset(0F, 22.2F, 2F));
        r.addOrReplaceChild("seg3", CubeListBuilder.create()
                .texOffs(0, 8).addBox(-1.6F, -1.4F, -2F, 3F, 3F, 4F),
                PartPose.offset(0F, 22.5F, 5F));
        r.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(16, 8).addBox(-1F, -1F, 0F, 2F, 2F, 4F),
                PartPose.offset(0F, 23F, 8F));
        return LayerDefinition.create(m, 32, 16);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float w = (float) Math.sin(ageInTicks * 0.35F + limbSwing * 0.5F) * 0.35F;
        // slither side-to-side with phase offset per segment
        head.yRot = w;
        seg1.yRot = (float) Math.sin(ageInTicks * 0.35F + 0.6F) * 0.3F;
        seg2.yRot = (float) Math.sin(ageInTicks * 0.35F + 1.2F) * 0.3F;
        seg3.yRot = (float) Math.sin(ageInTicks * 0.35F + 1.8F) * 0.3F;
        tail.yRot = (float) Math.sin(ageInTicks * 0.35F + 2.4F) * 0.35F;
        // slight vertical wave
        float v = (float) Math.sin(ageInTicks * 0.3F) * 0.3F;
        head.y = 22F + v;
        tail.y = 23F - v * 0.5F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buf, int light, int overlay, float r, float g, float b, float a) {
        head.render(pose, buf, light, overlay, r, g, b, a);
        seg1.render(pose, buf, light, overlay, r, g, b, a);
        seg2.render(pose, buf, light, overlay, r, g, b, a);
        seg3.render(pose, buf, light, overlay, r, g, b, a);
        tail.render(pose, buf, light, overlay, r, g, b, a);
    }
}
