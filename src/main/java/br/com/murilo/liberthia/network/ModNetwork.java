package br.com.murilo.liberthia.network;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(LiberthiaMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(packetId++,
                S2CInfectionSyncPacket.class,
                S2CInfectionSyncPacket::encode,
                S2CInfectionSyncPacket::decode,
                S2CInfectionSyncPacket::handle);

        CHANNEL.registerMessage(packetId++,
                S2CMatterEnergySyncPacket.class,
                S2CMatterEnergySyncPacket::encode,
                S2CMatterEnergySyncPacket::decode,
                S2CMatterEnergySyncPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
