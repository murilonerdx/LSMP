package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenWorkerTeleporterScreenS2CPacket;
import br.com.murilo.liberthia.util.MarkedPlayerEntry;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Worker Teleporter — right-click opens a player-picker screen. The caller
 * chooses the target from the list of online players and is teleported to
 * that player's current position. See {@link WorkerTeleporterTargetC2SPacket}
 * import below (resolved at runtime through the packet class).
 */
public class WorkerTeleporterItem extends Item {
    public WorkerTeleporterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        List<ServerPlayer> online = sp.server.getPlayerList().getPlayers();
        List<MarkedPlayerEntry> entries = new ArrayList<>();
        for (ServerPlayer p : online) {
            if (p == sp) continue;
            entries.add(new MarkedPlayerEntry(p.getUUID(), p.getGameProfile().getName()));
        }

        if (entries.isEmpty()) {
            sp.displayClientMessage(
                    Component.literal("Nenhum outro jogador online.").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResultHolder.pass(stack);
        }

        ModNetwork.sendToPlayer(sp, new OpenWorkerTeleporterScreenS2CPacket(entries));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Clique: escolher jogador para teleportar").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Cooldown: 20s após cada uso").withStyle(ChatFormatting.DARK_GRAY));
    }
}
