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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * r187 — <b>Amalgamado Cego</b>: modelo 3D REAL (substitui o billboard plano
 * gigante). Massa de carne fundida — torso disforme que <i>respira</i> + cabeça
 * afundada que pende + 2 braços assimétricos + 2 pernas atarracadas + 2
 * tentáculos que se contorcem nas costas. Textura UV (64×64) por tools/gen_amalgam.py.
 */
public class AmalgamModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("liberthia", "amalgam"), "main");

    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart armBig;
    private final ModelPart armSmall;
    private final ModelPart[] tendrils = new ModelPart[2];

    public AmalgamModel(ModelPart root) {
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.armBig = torso.getChild("arm_big");
        this.armSmall = torso.getChild("arm_small");
        this.tendrils[0] = torso.getChild("tendril0");
        this.tendrils[1] = torso.getChild("tendril1");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // pernas atarracadas (plantadas no chão, y16..24) — 4x8x4 @ (40,34)
        root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(40, 34)
                .addBox(-2F, 0F, -2F, 4F, 8F, 4F), PartPose.offset(3F, 16F, 0F));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(40, 34)
                .addBox(-2F, 0F, -2F, 4F, 8F, 4F), PartPose.offset(-3F, 16F, 0F));

        // torso disforme 10x14x8 @ (0,0) (world y2..16)
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5F, -14F, -4F, 10F, 14F, 8F), PartPose.offset(0F, 16F, 0F));

        // cabeça afundada 7x6x7 @ (0,24) — no topo do torso
        torso.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 24)
                .addBox(-3.5F, -6F, -3.5F, 7F, 6F, 7F), PartPose.offset(0F, -14F, -1F));

        // braço grande 4x12x4 @ (40,0) — ombro direito
        torso.addOrReplaceChild("arm_big", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-2F, 0F, -2F, 4F, 12F, 4F), PartPose.offset(6F, -13F, 0F));
        // braço pequeno 3x9x3 @ (40,18) — ombro esquerdo
        torso.addOrReplaceChild("arm_small", CubeListBuilder.create().texOffs(40, 18)
                .addBox(-1.5F, 0F, -1.5F, 3F, 9F, 3F), PartPose.offset(-6F, -12F, 0F));

        // tentáculos nas costas 2x7x2 @ (0,40)
        torso.addOrReplaceChild("tendril0", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offset(3F, -13F, 4F));
        torso.addOrReplaceChild("tendril1", CubeListBuilder.create().texOffs(0, 40)
                .addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offset(-3F, -13F, 4F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float age, float netHeadYaw, float headPitch) {
        // torso: balanço pesado + respiração + heave vertical
        torso.zRot = Mth.sin(age * 0.05F) * 0.06F;
        torso.y = 16F + Mth.sin(age * 0.07F) * 0.5F;
        float breathe = 1F + Mth.sin(age * 0.08F) * 0.04F;
        torso.xScale = breathe;
        torso.zScale = breathe;

        // cabeça pende e segue vagamente o alvo
        head.zRot = Mth.sin(age * 0.06F) * 0.15F;
        head.yRot = Mth.sin(age * 0.045F) * 0.15F
                + Mth.clamp(netHeadYaw * ((float) Math.PI / 180F), -0.4F, 0.4F);
        head.xRot = 0.15F + Mth.clamp(headPitch * ((float) Math.PI / 180F), -0.3F, 0.3F);

        // braços: balanço de caminhada + idle
        armBig.xRot = Mth.cos(limbSwing * 0.6F) * 0.5F * limbSwingAmount + Mth.sin(age * 0.05F) * 0.1F;
        armSmall.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.5F * limbSwingAmount
                + Mth.sin(age * 0.05F + 1F) * 0.12F;
        armBig.zRot = 0.08F;
        armSmall.zRot = -0.1F;

        // pernas arrastando
        legL.xRot = Mth.cos(limbSwing * 0.6F) * 0.4F * limbSwingAmount;
        legR.xRot = Mth.cos(limbSwing * 0.6F + (float) Math.PI) * 0.4F * limbSwingAmount;

        // tentáculos contorcendo
        for (int i = 0; i < 2; i++) {
            tendrils[i].xRot = Mth.sin(age * 0.10F + i * 1.6F) * 0.4F;
            tendrils[i].zRot = Mth.cos(age * 0.08F + i * 2.1F) * 0.3F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buf, int light, int overlay,
                               float r, float g, float b, float a) {
        legL.render(pose, buf, light, overlay, r, g, b, a);
        legR.render(pose, buf, light, overlay, r, g, b, a);
        torso.render(pose, buf, light, overlay, r, g, b, a);
    }
}
