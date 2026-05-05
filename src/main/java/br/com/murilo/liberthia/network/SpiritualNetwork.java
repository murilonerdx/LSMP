package br.com.murilo.liberthia.network;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.packet.ClientboundSpiritualSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class SpiritualNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LiberthiaMod.MODID, "spiritual_network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private SpiritualNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(
                        ClientboundSpiritualSyncPacket.class,
                        nextId(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(ClientboundSpiritualSyncPacket::encode)
                .decoder(ClientboundSpiritualSyncPacket::decode)
                .consumerMainThread(ClientboundSpiritualSyncPacket::handle)
                .add();
    }

    public static void sendToAll(MinecraftServer server, Object packet) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player, packet);
        }
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static int nextId() {
        return packetId++;
    }
}
