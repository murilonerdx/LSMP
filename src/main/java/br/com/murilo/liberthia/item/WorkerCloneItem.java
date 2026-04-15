package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.WorkerCloneManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Worker Clone:
 *  - right-click (no clone): spawn idle clone
 *  - shift+right-click (clone exists): start recording owner movements
 *  - right-click (recording): start replay of recorded movements
 *  - right-click (idle/replaying): remove clone
 */
public class WorkerCloneItem extends Item {

    public WorkerCloneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        boolean active = WorkerCloneManager.isActive(sp.getUUID());

        if (!active) {
            WorkerCloneManager.spawn(sp);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (player.isShiftKeyDown()) {
            WorkerCloneManager.startRecording(sp.getUUID());
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        WorkerCloneManager.State st = WorkerCloneManager.getState(sp.getUUID());
        if (st == WorkerCloneManager.State.RECORDING) {
            WorkerCloneManager.startReplay(sp.getUUID());
        } else {
            WorkerCloneManager.removeFor(sp.getUUID());
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Direito: spawn / replay / remover").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift+Direito: gravar movimentos").withStyle(ChatFormatting.DARK_GRAY));
    }
}
