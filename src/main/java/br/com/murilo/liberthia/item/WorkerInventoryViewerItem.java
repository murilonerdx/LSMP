package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class WorkerInventoryViewerItem extends Item {
    private static final String NBT_INDEX = "WorkerInvTarget";

    public WorkerInventoryViewerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        List<ServerPlayer> players = sp.server.getPlayerList().getPlayers();
        if (players.isEmpty()) return InteractionResultHolder.pass(stack);

        CompoundTag tag = stack.getOrCreateTag();
        int idx = tag.getInt(NBT_INDEX);

        ServerPlayer target = null;
        int start = idx % players.size();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer candidate = players.get((start + i) % players.size());
            if (candidate != sp) {
                target = candidate;
                idx = (start + i + 1) % players.size();
                break;
            }
        }
        if (target == null) return InteractionResultHolder.pass(stack);
        tag.putInt(NBT_INDEX, idx);

        if (player.isShiftKeyDown()) return InteractionResultHolder.sidedSuccess(stack, false);

        final ServerPlayer finalTarget = target;
        sp.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x4, id, inv, finalTarget.getInventory(), 4),
                Component.literal("Inv: " + finalTarget.getName().getString())));

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Clique: abre inventario do proximo player").withStyle(ChatFormatting.GRAY));
    }
}
