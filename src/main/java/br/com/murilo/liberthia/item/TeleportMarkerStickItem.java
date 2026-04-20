package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.util.TeleportToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class TeleportMarkerStickItem extends Item {
    public TeleportMarkerStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            double x = context.getClickedPos().getX() + 0.5D;
            double y = context.getClickedPos().getY() + 1.0D;
            double z = context.getClickedPos().getZ() + 0.5D;
            TeleportToolData.setAnchor(serverPlayer, level.dimension(), x, y, z);
            serverPlayer.displayClientMessage(
                    Component.literal("Local de teleporte marcado em " + (int) x + ", " + (int) y + ", " + (int) z)
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, net.minecraft.world.InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().isClientSide) {
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }

        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!(target instanceof ServerPlayer targetPlayer)) {
            serverPlayer.displayClientMessage(Component.literal("Esse bastão só marca players.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        if (targetPlayer.getUUID().equals(serverPlayer.getUUID())) {
            serverPlayer.displayClientMessage(Component.literal("Você não pode marcar você mesmo.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        if (TeleportToolData.getAnchor(serverPlayer).isEmpty()) {
            serverPlayer.displayClientMessage(Component.literal("Primeiro marque um local com SHIFT + clique direito em um bloco.").withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.CONSUME;
        }

        boolean added = TeleportToolData.addMarkedPlayer(serverPlayer, targetPlayer.getUUID());
        if (added) {
            serverPlayer.displayClientMessage(Component.literal("Player marcado: " + targetPlayer.getGameProfile().getName()).withStyle(ChatFormatting.GREEN), true);
        } else {
            serverPlayer.displayClientMessage(Component.literal("Esse player já está marcado.").withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResult.CONSUME;
    }
}
