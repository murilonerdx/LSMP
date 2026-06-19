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
 * r187 — <b>Inseto/Ácaro Cósmico</b>: modelo 3D REAL para os pequenos mobs-inseto
 * (Ácaro do Sussurro, Carrapato do Vazio, Larva Gritante, Cria do Espelho,
 * Verme Dimensional) e — com {@code hasWings} — a Mariposa do Breu.
 * Corpo quitinoso + cabeça + 2 antenas que mexem + 6 patinhas com marcha +
 * 2 asas (opcionais) que batem. Textura UV única (64×64) por tools/gen_mite.py.
 */
public class MiteModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("liberthia", "mite"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart antennaL;
    private final ModelPart antennaR;
    private final ModelPart wingL;
    private final ModelPart wingR;
    private final ModelPart[] legs = new ModelPart[6];
    private final boolean hasWings;

    public MiteModel(ModelPart root, boolean hasWings) {
        this.hasWings = hasWings;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.antennaL = head.getChild("antenna_l");
        this.antennaR = head.getChild("antenna_r");
        this.wingL = body.getChild("wing_l");
        this.wingR = body.getChild("wing_r");
        for (int i = 0; i < 6; i++) this.legs[i] = body.getChild("leg" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // corpo 6x4x7 @ (0,0)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3F, -2F, -3.5F, 6F, 4F, 7F),
                PartPose.offset(0F, 20F, 0F));

        // cabeça 4x3x3 @ (0,12)
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 12).addBox(-2F, -1.5F, -3F, 4F, 3F, 3F),
                PartPose.offset(0F, -0.5F, -3.5F));

        // antenas 1x4x1 @ (28,6) (sobem da frente da cabeça)
        head.addOrReplaceChild("antenna_l",
                CubeListBuilder.create().texOffs(28, 6).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F),
                PartPose.offsetAndRotation(-1F, -1.5F, -2.5F, -0.4F, 0F, -0.2F));
        head.addOrReplaceChild("antenna_r",
                CubeListBuilder.create().texOffs(28, 6).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F),
                PartPose.offsetAndRotation(1F, -1.5F, -2.5F, -0.4F, 0F, 0.2F));

        // 6 patas 1x4x1 @ (28,0)
        float[] zs = {-2F, 0F, 2F};
        for (int i = 0; i < 6; i++) {
            boolean left = i < 3;
            float z = zs[i % 3];
            float x = left ? 2.5F : -2.5F;
            float splay = left ? 0.7F : -0.7F;
            body.addOrReplaceChild("leg" + i,
                    CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, 0F, -0.5F, 1F, 4F, 1F),
                    PartPose.offsetAndRotation(x, 1.5F, z, 0F, 0F, splay));
        }

        // asas 8x1x6 @ (34,0) — direita +x, esquerda espelhada -x
        body.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(34, 0).addBox(0F, -0.5F, -3F, 8F, 1F, 6F),
                PartPose.offset(2.5F, -1.8F, 0F));
        body.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(34, 0).mirror().addBox(-8F, -0.5F, -3F, 8F, 1F, 6F),
                PartPose.offset(-2.5F, -1.8F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float age, float netHeadYaw, float headPitch) {
        body.y = 20F + Mth.sin(age * 0.18F) * (hasWings ? 0.9F : 0.3F);

        head.yRot = Mth.clamp(netHeadYaw * ((float) Math.PI / 180F), -0.7F, 0.7F);
        head.xRot = Mth.clamp(headPitch * ((float) Math.PI / 180F), -0.5F, 0.5F);

        // antenas tateando
        antennaL.xRot = -0.4F + Mth.sin(age * 0.25F) * 0.25F;
        antennaR.xRot = -0.4F + Mth.sin(age * 0.25F + 0.5F) * 0.25F;

        // marcha das patas (defasada)
        for (int i = 0; i < 6; i++) {
            float phase = (i % 2 == 0) ? 0F : (float) Math.PI;
            legs[i].xRot = Mth.cos(limbSwing * 1.1F + phase) * 0.7F * limbSwingAmount
                    + Mth.sin(age * 0.12F + i) * 0.05F;
        }

        // asas
        wingL.visible = hasWings;
        wingR.visible = hasWings;
        if (hasWings) {
            float flap = Mth.sin(age * 0.9F) * 0.9F;   // batida rápida
            wingR.zRot = -0.2F - flap;
            wingL.zRot = 0.2F + flap;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buf, int light, int overlay,
                               float r, float g, float b, float a) {
        body.render(pose, buf, light, overlay, r, g, b, a);
    }
}
