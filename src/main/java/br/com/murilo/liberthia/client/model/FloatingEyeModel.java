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
 * r187 — <b>Olho Flutuante</b>: modelo 3D REAL (não mais billboard plano) para a
 * família de mobs-olho cósmicos (Colossal, Parasita, Podre, Orbe do Pavor,
 * Sanguessuga). Esfera-olho (cubo 10³) + íris frontal que <i>rastreia o player</i>
 * + 6 tentáculos/nervos pendurados que ondulam + flutuação + pulso + piscada
 * (squash vertical do olho). Textura UV única (64×64) gerada por tools/gen_floating_eye.py.
 */
public class FloatingEyeModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("liberthia", "floating_eye"), "main");

    private final ModelPart body;
    private final ModelPart eyeball;
    private final ModelPart[] tentacles = new ModelPart[6];

    public FloatingEyeModel(ModelPart root) {
        this.body = root.getChild("body");
        this.eyeball = body.getChild("eyeball");
        for (int i = 0; i < 6; i++) this.tentacles[i] = body.getChild("tentacle" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create(), PartPose.offset(0F, 14F, 0F));

        // Globo ocular 10x10x10 — texOffs(0,0) ocupa 40x20 no atlas
        PartDefinition eyeball = body.addOrReplaceChild("eyeball",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5F, -5F, -5F, 10F, 10F, 10F),
                PartPose.ZERO);

        // Íris/pupila — placa fina protrusa na FRENTE (-Z, direção que o mob encara)
        eyeball.addOrReplaceChild("iris",
                CubeListBuilder.create().texOffs(0, 22).addBox(-3F, -3F, -6F, 6F, 6F, 1F),
                PartPose.ZERO);

        // 6 tentáculos/nervos pendurados em círculo sob o olho
        for (int i = 0; i < 6; i++) {
            double ang = Math.PI * 2.0 * i / 6.0;
            float tx = (float) Math.cos(ang) * 3.6F;
            float tz = (float) Math.sin(ang) * 3.6F;
            body.addOrReplaceChild("tentacle" + i,
                    CubeListBuilder.create().texOffs(44, 22).addBox(-1F, 0F, -1F, 2F, 9F, 2F),
                    PartPose.offset(tx, 4.6F, tz));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float age, float netHeadYaw, float headPitch) {
        // flutuação vertical lenta
        body.y = 14F + Mth.sin(age * 0.08F) * 1.3F;

        // rastreamento do olhar — íris/olho gira na direção do alvo
        eyeball.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.85F;
        eyeball.xRot = headPitch * ((float) Math.PI / 180F) * 0.85F;

        // piscada: a cada ~140 ticks o globo "achata" verticalmente por ~7 ticks
        float phase = age % 140F;
        float blink = phase < 7F ? Mth.sin(phase / 7F * (float) Math.PI) : 0F;
        eyeball.yScale = 1F - blink * 0.85F;
        eyeball.xScale = 1F + blink * 0.08F;

        // pulso sutil do globo (respira)
        float pulse = 1F + Mth.sin(age * 0.10F) * 0.03F;
        eyeball.zScale = pulse;

        // tentáculos ondulando com defasagem
        for (int i = 0; i < 6; i++) {
            tentacles[i].xRot = Mth.sin(age * 0.13F + i * 1.05F) * 0.32F;
            tentacles[i].zRot = Mth.cos(age * 0.11F + i * 1.37F) * 0.26F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buf, int light, int overlay,
                               float r, float g, float b, float a) {
        body.render(pose, buf, light, overlay, r, g, b, a);
    }
}
