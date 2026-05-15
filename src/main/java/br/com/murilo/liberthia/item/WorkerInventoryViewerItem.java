package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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

/**
 * Worker Inventory Viewer — manda uma lista clicável de players online no chat.
 * O user escolhe qual inventário abrir, e pode mexer items normalmente (chest menu
 * 9x4 wrap em cima do inventário real do target).
 */
public class WorkerInventoryViewerItem extends Item {

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
        if (players.size() <= 1) {
            sp.sendSystemMessage(Component.literal("§cNenhum outro jogador online."));
            return InteractionResultHolder.fail(stack);
        }

        sp.sendSystemMessage(Component.literal("§8§m                              "));
        sp.sendSystemMessage(Component.literal("§6§l Worker Inventory Viewer").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        sp.sendSystemMessage(Component.literal("§7Escolha um jogador pra abrir o inventário:"));
        sp.sendSystemMessage(Component.literal(""));

        for (ServerPlayer target : players) {
            if (target == sp) continue;
            String name = target.getGameProfile().getName();
            MutableComponent line = Component.literal("§a● §f" + name + " ");
            MutableComponent openBtn = Component.literal("§b[Abrir Inventário]")
                    .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/liberthia invview " + target.getUUID()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Abrir inv de " + name))));
            line.append(openBtn);
            sp.sendSystemMessage(line);
        }
        sp.sendSystemMessage(Component.literal("§8§m                              "));

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /**
     * Abre o inventário do target em chest menu 9x4. Slots editáveis: o user pode
     * arrastar items dentro/fora pra modificar o inventário real do target.
     */
    public static void openInventoryFor(ServerPlayer viewer, ServerPlayer target) {
        viewer.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x4, id, inv, target.getInventory(), 4),
                Component.literal("Inv: " + target.getGameProfile().getName())));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Clique: lista players online pra escolher").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Mexer items modifica o inventário real").withStyle(ChatFormatting.DARK_GRAY));
    }
}
