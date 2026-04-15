package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.ClonePlayerEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders a {@link ClonePlayerEntity} as a real player — PlayerModel + armor layer + the
 * owner's actual skin (resolved asynchronously, falling back to the default Steve/Alex skin).
 */
public class ClonePlayerRenderer extends LivingEntityRenderer<ClonePlayerEntity, PlayerModel<ClonePlayerEntity>> {

    private static final Map<UUID, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();

    public ClonePlayerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(ClonePlayerEntity entity) {
        UUID uuid = entity.getOwnerUuid();
        if (uuid == null) {
            return DefaultPlayerSkin.getDefaultSkin(UUID.randomUUID());
        }

        ResourceLocation cached = SKIN_CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }

        String name = entity.getOwnerName();
        if (name != null && !name.isEmpty()) {
            try {
                GameProfile profile = new GameProfile(uuid, name);
                SkinManager skinManager = Minecraft.getInstance().getSkinManager();

                skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN && location != null) {
                        SKIN_CACHE.put(uuid, location);
                    }
                }, false);
            } catch (Throwable ignored) {
            }
        }

        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    @Override
    protected void setupRotations(ClonePlayerEntity entity, PoseStack poseStack,
                                  float bob, float bodyYaw, float partialTick) {
        super.setupRotations(entity, poseStack, bob, bodyYaw, partialTick);
    }

    @Override
    public void render(ClonePlayerEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        PlayerModel<ClonePlayerEntity> model = this.getModel();

        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        model.rightArmPose = mainHand.isEmpty()
                ? HumanoidModel.ArmPose.EMPTY
                : HumanoidModel.ArmPose.ITEM;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        model.crouching = entity.isCrouching();

        super.render(entity, yaw, partialTick, poseStack, bufferSource, light);
    }
}