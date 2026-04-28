package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenTeleportExecutorScreenS2CPacket;
import br.com.murilo.liberthia.util.MarkedPlayerEntry;
import br.com.murilo.liberthia.util.TeleportAnchor;
import br.com.murilo.liberthia.util.TeleportToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bastão Executor de Teleporte. Abre um menu onde o dono dispara o TP.
 *
 * UX corrigido:
 *  - Se o jogador ainda não marcou nenhuma âncora, usamos a posição atual dele
 *    como destino (implícito) — nada de "não foi marcado" bloqueando o menu.
 *  - Se não há jogadores marcados, listamos todos os jogadores online (exceto
 *    o próprio) para que possam ser teleportados direto pela tela.
 */
public class TeleportExecutorStickItem extends Item {
    public TeleportExecutorStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // 1) Resolve destination anchor — fall back to the user's current position.
        Optional<TeleportAnchor> anchorOpt = TeleportToolData.getAnchor(serverPlayer);
        TeleportAnchor anchor;
        if (anchorOpt.isPresent()) {
            anchor = anchorOpt.get();
        } else {
            ResourceKey<Level> dim = serverPlayer.level().dimension();
            anchor = new TeleportAnchor(dim,
                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ());
            serverPlayer.displayClientMessage(
                    Component.literal("Sem âncora marcada — usando sua posição atual como destino.")
                            .withStyle(ChatFormatting.YELLOW), true);
        }

        // 2) Resolve player list — fall back to every online player other than me.
        List<MarkedPlayerEntry> entries = TeleportToolData.getMarkedEntries(serverPlayer);
        if (entries.isEmpty()) {
            entries = new ArrayList<>();
            for (ServerPlayer other : serverPlayer.server.getPlayerList().getPlayers()) {
                if (other == serverPlayer) continue;
                entries.add(new MarkedPlayerEntry(other.getUUID(), other.getGameProfile().getName()));
            }
            if (entries.isEmpty()) {
                serverPlayer.displayClientMessage(
                        Component.literal("Nenhum jogador online para teleportar.")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                new OpenTeleportExecutorScreenS2CPacket(anchor, entries));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Clique: abrir menu de teleporte").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sem âncora? Usa sua posição atual.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Sem marcados? Lista todos online.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
