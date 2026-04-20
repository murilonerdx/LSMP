package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenTeleportExecutorScreenS2CPacket;
import br.com.murilo.liberthia.util.MarkedPlayerEntry;
import br.com.murilo.liberthia.util.TeleportAnchor;
import br.com.murilo.liberthia.util.TeleportToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class TeleportExecutorStickItem extends Item {
    public TeleportExecutorStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            Optional<TeleportAnchor> anchor = TeleportToolData.getAnchor(serverPlayer);
            if (anchor.isEmpty()) {
                serverPlayer.displayClientMessage(Component.literal("Primeiro marque um local com o Bastão Marcador.").withStyle(ChatFormatting.YELLOW), true);
                return InteractionResultHolder.fail(stack);
            }

            List<MarkedPlayerEntry> entries = TeleportToolData.getMarkedEntries(serverPlayer);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new OpenTeleportExecutorScreenS2CPacket(anchor.get(), entries));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
