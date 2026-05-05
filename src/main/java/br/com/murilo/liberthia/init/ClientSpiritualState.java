package br.com.murilo.liberthia.init;

import br.com.murilo.liberthia.item.SpiritualConnectionItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSpiritualState {

    private static final Map<UUID, String> ACTIVE_SPIRITUAL_PLAYERS = new ConcurrentHashMap<>();

    private ClientSpiritualState() {
    }

    public static void setActivePlayers(Map<UUID, String> activePlayers) {
        ACTIVE_SPIRITUAL_PLAYERS.clear();
        ACTIVE_SPIRITUAL_PLAYERS.putAll(activePlayers);
    }

    public static String getChannelFor(UUID ownerUuid) {
        return ACTIVE_SPIRITUAL_PLAYERS.get(ownerUuid);
    }

    public static boolean canLocalPlayerSee(LocalPlayer viewer, UUID ownerUuid, String channelId) {
        if (viewer == null) {
            return false;
        }

        if (viewer.getUUID().equals(ownerUuid)) {
            return true;
        }

        for (ItemStack stack : viewer.getInventory().items) {
            if (isMatchingLink(stack, ownerUuid, channelId)) {
                return true;
            }
        }

        for (ItemStack stack : viewer.getInventory().offhand) {
            if (isMatchingLink(stack, ownerUuid, channelId)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isMatchingLink(ItemStack stack, UUID ownerUuid, String channelId) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!stack.is(ModItems.SPIRITUAL_LINK.get())) {
            return false;
        }

        if (!stack.hasTag() || stack.getTag() == null) {
            return false;
        }

        String stackChannelId = stack.getTag().getString(SpiritualConnectionItem.TAG_CHANNEL_ID);
        String stackOwnerUuid = stack.getTag().getString(SpiritualConnectionItem.TAG_OWNER_UUID);

        return channelId.equals(stackChannelId) && ownerUuid.toString().equals(stackOwnerUuid);
    }
}