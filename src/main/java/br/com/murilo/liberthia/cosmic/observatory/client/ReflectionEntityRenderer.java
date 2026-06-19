package br.com.murilo.liberthia.cosmic.observatory.client;

import br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r49: Renderer EXATO da {@link ReflectionEntity} — copia
 * skin, armor, mainhand/offhand pose, crouching, walking animations
 * do player target.
 *
 * <h2>r49 melhorias</h2>
 * <ul>
 *   <li>SkinManager.registerSkins async (robusto contra offline/cracked)</li>
 *   <li>Armor layer renderiza qualquer armor que copiamos no spawn</li>
 *   <li>Mainhand E offhand pose dynamic</li>
 *   <li>Crouching state preserved</li>
 *   <li>Nametag dinâmico com o nome do dono</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class ReflectionEntityRenderer
        extends LivingEntityRenderer<ReflectionEntity, PlayerModel<ReflectionEntity>> {

    private static final Map<UUID, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();

    public ReflectionEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        // Armor layer — desenha armor pieces dos slots da entity
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(ReflectionEntity entity) {
        UUID uuid = entity.getOwnerUuid();
        if (uuid == null) {
            return DefaultPlayerSkin.getDefaultSkin(UUID.randomUUID());
        }
        ResourceLocation cached = SKIN_CACHE.get(uuid);
        if (cached != null) return cached;

        String name = entity.getOwnerName();
        if (name != null && !name.isEmpty()) {
            try {
                GameProfile profile = new GameProfile(uuid, name);
                SkinManager skinManager = Minecraft.getInstance().getSkinManager();
                // Async lookup — chama callback quando skin estiver pronta
                skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN && location != null) {
                        SKIN_CACHE.put(uuid, location);
                    }
                }, false);
            } catch (Throwable ignored) {}
        }
        // Fallback Steve/Alex enquanto skin não baixa
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    @Override
    public void render(ReflectionEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        PlayerModel<ReflectionEntity> model = this.getModel();

        // Setup pose: mainhand, offhand, crouching, young
        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offHand = entity.getItemBySlot(EquipmentSlot.OFFHAND);
        model.rightArmPose = mainHand.isEmpty()
                ? HumanoidModel.ArmPose.EMPTY
                : HumanoidModel.ArmPose.ITEM;
        model.leftArmPose = offHand.isEmpty()
                ? HumanoidModel.ArmPose.EMPTY
                : HumanoidModel.ArmPose.ITEM;
        model.crouching = entity.isCrouching();
        model.young = false;

        super.render(entity, yaw, partialTick, poseStack, bufferSource, light);
    }

    /**
     * r49: nametag dinâmico — usa o {@code ownerName} ao invés do nome
     * default da entity ("Reflection Entity").
     */
    @Override
    protected boolean shouldShowName(ReflectionEntity entity) {
        // Show nametag if entity has owner name (mesma lógica de player vanilla)
        return entity.getOwnerName() != null && !entity.getOwnerName().isEmpty()
                && entity.shouldShowName();
    }

    @Override
    protected void renderNameTag(ReflectionEntity entity, Component name, PoseStack poseStack,
                                   MultiBufferSource buffer, int light) {
        Component nameTag = Component.literal(entity.getOwnerName());
        super.renderNameTag(entity, nameTag, poseStack, buffer, light);
    }
}
