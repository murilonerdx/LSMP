package br.com.murilo.liberthia.entry;

public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    public static void handlePlayerList(AdminPlayerListS2CPacket msg) {
        AdminClientState.updatePlayers(msg.getPlayers());
    }

    public static void handleInventory(AdminInventoryS2CPacket msg) {
        AdminClientState.openInventory(msg.getSnapshot());
    }
}
