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
 * r187 — <b>Rastejante</b>: modelo 3D REAL para os mobs cósmicos rastejantes
 * (Devora-Carne, Vigia de Carne, Hospedeiro, Parasita de Brasa, Tecelão Cego).
 * Corpo achatado + cabeça com <i>mandíbula que mastiga</i> + 6 patas com marcha
 * de tripé (limbSwing) + olhos. Textura UV única (64×64) por tools/gen_crawler.py.
 */
public class CrawlerModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("liberthia", "crawler"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart[] legs = new ModelPart[6];

    public CrawlerModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.upperJaw = head.getChild("upper_jaw");
        this.lowerJaw = head.getChild("lower_jaw");
        for (int i = 0; i < 6; i++) this.legs[i] = body.getChild("leg" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // corpo achatado 8x5x12 — texOffs(0,0)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4F, -2.5F, -6F, 8F, 5F, 12F),
                PartPose.offset(0F, 19F, 0F));

        // cabeça 6x5x5 na frente (-Z) — texOffs(0,18)
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3F, -2.5F, -5F, 6F, 5F, 5F),
                PartPose.offset(0F, 0F, -6F));

        // mandíbula superior 6x2x5 — texOffs(24,18) — pivot no topo da boca
        head.addOrReplaceChild("upper_jaw",
                CubeListBuilder.create().texOffs(24, 18).addBox(-3F, -2F, -5F, 6F, 2F, 5F),
                PartPose.offset(0F, -1F, -5F));
        // mandíbula inferior 6x2x5 — texOffs(24,26) — pivot na base
        head.addOrReplaceChild("lower_jaw",
                CubeListBuilder.create().texOffs(24, 26).addBox(-3F, 0F, -5F, 6F, 2F, 5F),
                PartPose.offset(0F, 1.5F, -5F));

        // 6 patas (3 por lado), texOffs(48,0) reutilizado
        float[] zs = {-4F, 0F, 4F};
        for (int i = 0; i < 6; i++) {
            boolean left = i < 3;
            float z = zs[i % 3];
            float x = left ? 4F : -4F;
            float splay = left ? 0.6F : -0.6F;
            body.addOrReplaceChild("leg" + i,
                    CubeListBuilder.create().texOffs(48, 0).addBox(-1F, 0F, -1F, 2F, 6F, 2F),
                    PartPose.offsetAndRotation(x, 1.5F, z, 0F, 0F, splay));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float age, float netHeadYaw, float headPitch) {
        // bob do corpo
        body.y = 19F + Mth.sin(age * 0.12F) * 0.4F;

        // cabeça rastreia o alvo (clampada)
        head.yRot = Mth.clamp(netHeadYaw * ((float) Math.PI / 180F), -0.6F, 0.6F);
        head.xRot = Mth.clamp(headPitch * ((float) Math.PI / 180F), -0.5F, 0.5F);

        // mandíbula mastigando
        float chomp = (Mth.sin(age * 0.18F) * 0.5F + 0.5F) * 0.55F;
        upperJaw.xRot = -chomp * 0.6F;
        lowerJaw.xRot = chomp;

        // marcha de tripé: pernas 0,2,4 em fase; 1,3,5 opostas
        for (int i = 0; i < 6; i++) {
            float phase = (i % 2 == 0) ? 0F : (float) Math.PI;
            float swing = Mth.cos(limbSwing * 0.9F + phase) * 0.6F * limbSwingAmount;
            float idle = Mth.sin(age * 0.10F + i) * 0.05F;
            legs[i].xRot = swing + idle;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buf, int light, int overlay,
                               float r, float g, float b, float a) {
        body.render(pose, buf, light, overlay, r, g, b, a);
    }
}
