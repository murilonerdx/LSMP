package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entry.TrackerC2SPacket;
import br.com.murilo.liberthia.entry.TrackerScreen;
import br.com.murilo.liberthia.logic.TrackerManager;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Rastreador de Expedição.
 * Clique em player: marca como rastreado.
 * Shift+Clique: abre GUI com posição do alvo.
 * Sem alvo: rastreia a si mesmo (pra teste).
 */
public class ExpeditionTrackerItem extends Item {
    private static final String TAG_TARGET_UUID = "tracker_target";
    private static final String TAG_TARGET_NAME = "tracker_name";

    public ExpeditionTrackerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer)) return InteractionResult.PASS;
        if (!(target instanceof ServerPlayer targetPlayer)) return InteractionResult.PASS;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_TARGET_UUID, targetPlayer.getUUID());
        tag.putString(TAG_TARGET_NAME, targetPlayer.getName().getString());
        TrackerManager.track(targetPlayer.getUUID(), targetPlayer.getName().getString());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();

        if (level.isClientSide) {
        }

        if (player.isShiftKeyDown() && tag.hasUUID(TAG_TARGET_UUID)) {
            // Abre GUI de rastreamento no client
            if (level.isClientSide) {
                UUID targetId = tag.getUUID(TAG_TARGET_UUID);
                String name = tag.getString(TAG_TARGET_NAME);
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        net.minecraft.client.Minecraft.getInstance().setScreen(new TrackerScreen(targetId, name)));
                ModNetwork.CHANNEL.sendToServer(new TrackerC2SPacket(targetId));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Clique normal no server: marca alvo via raycast ou si mesmo
        if (!level.isClientSide && player instanceof ServerPlayer admin) {
            Vec3 eye = admin.getEyePosition();
            Vec3 look = admin.getLookAngle();
            ServerPlayer closest = null;
            double closestDist = Double.MAX_VALUE;
            for (ServerPlayer sp : admin.serverLevel().players()) {
                if (sp == admin) continue;
                Vec3 toTarget = sp.position().add(0, sp.getEyeHeight() * 0.5, 0).subtract(eye);
                double dist = toTarget.length();
                if (dist > 50) continue;
                double dot = toTarget.normalize().dot(look.normalize());
                if (dot > 0.85 && dist < closestDist) {
                    closestDist = dist;
                    closest = sp;
                }
            }

            ServerPlayer target = closest != null ? closest : admin; // sem alvo = si mesmo
            tag.putUUID(TAG_TARGET_UUID, target.getUUID());
            tag.putString(TAG_TARGET_NAME, target.getName().getString());
            TrackerManager.track(target.getUUID(), target.getName().getString());
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Rastreador de Expedição").withStyle(ChatFormatting.AQUA));
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(TAG_TARGET_NAME)) {
            tooltip.add(Component.literal("Alvo: " + tag.getString(TAG_TARGET_NAME)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal("Clique: marcar alvo").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal("Shift+Clique: ver posição").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
