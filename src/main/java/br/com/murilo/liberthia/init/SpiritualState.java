package br.com.murilo.liberthia.init;

import br.com.murilo.liberthia.item.SpiritualConnectionItem;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.SpiritualNetwork;
import br.com.murilo.liberthia.network.packet.ClientboundSpiritualSyncPacket;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpiritualState {

    private static final Map<UUID, String> ACTIVE_SPIRITUAL_PLAYERS = new ConcurrentHashMap<>();

    private SpiritualState() {
    }

    public static boolean isActive(UUID playerUuid) {
        return ACTIVE_SPIRITUAL_PLAYERS.containsKey(playerUuid);
    }

    public static Map<UUID, String> snapshot() {
        return Collections.unmodifiableMap(ACTIVE_SPIRITUAL_PLAYERS);
    }

    public static int toggle(ServerPlayer player) {
        UUID playerUuid = player.getUUID();

        if (ACTIVE_SPIRITUAL_PLAYERS.containsKey(playerUuid)) {
            ACTIVE_SPIRITUAL_PLAYERS.remove(playerUuid);

            player.sendSystemMessage(
                    Component.literal("Você saiu da conexão espiritual.")
                            .withStyle(ChatFormatting.GRAY)
            );

            syncAll(player.server);
            return 1;
        }

        ItemStack connectionStack = findValidConnectionItem(player);

        if (connectionStack.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal("Você precisa manter a Conexão Espiritual no inventário.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        String channelId = connectionStack.getTag().getString(SpiritualConnectionItem.TAG_CHANNEL_ID);

        ACTIVE_SPIRITUAL_PLAYERS.put(playerUuid, channelId);

        player.sendSystemMessage(
                Component.literal("Você entrou na conexão espiritual.")
                        .withStyle(ChatFormatting.DARK_PURPLE)
        );

        syncAll(player.server);
        return 1;
    }

    public static void forceDisable(ServerPlayer player) {
        if (ACTIVE_SPIRITUAL_PLAYERS.remove(player.getUUID()) != null) {
            player.sendSystemMessage(
                    Component.literal("A conexão espiritual foi quebrada.")
                            .withStyle(ChatFormatting.RED)
            );

            syncAll(player.server);
        }
    }

    public static void syncAll(MinecraftServer server) {
        ModNetwork.sendToAll(
                server,
                new ClientboundSpiritualSyncPacket(ACTIVE_SPIRITUAL_PLAYERS)
        );
    }


    public static void syncTo(ServerPlayer player) {
        ModNetwork.sendToPlayer(
                player,
                new ClientboundSpiritualSyncPacket(ACTIVE_SPIRITUAL_PLAYERS)
        );
    }

    public static ItemStack findValidConnectionItem(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isValidConnectionItemForOwner(stack, player.getUUID())) {
                return stack;
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (isValidConnectionItemForOwner(stack, player.getUUID())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean isValidConnectionItemForOwner(ItemStack stack, UUID ownerUuid) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!stack.is(ModItems.SPIRITUAL_CONNECTION.get())) {
            return false;
        }

        if (!stack.hasTag() || stack.getTag() == null) {
            return false;
        }

        if (!stack.getTag().contains(SpiritualConnectionItem.TAG_CHANNEL_ID)) {
            return false;
        }

        if (!stack.getTag().contains(SpiritualConnectionItem.TAG_OWNER_UUID)) {
            return false;
        }

        return ownerUuid.toString().equals(stack.getTag().getString(SpiritualConnectionItem.TAG_OWNER_UUID));
    }
}
