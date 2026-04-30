package br.com.murilo.liberthia.network;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.packet.KirikoBookTeleportPacket;
import br.com.murilo.liberthia.packet.OpenKirikoBookScreenPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class KirikoBookNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LiberthiaMod.MODID, "book_red_kiriko"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private KirikoBookNetworking() {
    }

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(
                id++,
                OpenKirikoBookScreenPacket.class,
                OpenKirikoBookScreenPacket::encode,
                OpenKirikoBookScreenPacket::decode,
                OpenKirikoBookScreenPacket::handle
        );

        CHANNEL.registerMessage(
                id++,
                KirikoBookTeleportPacket.class,
                KirikoBookTeleportPacket::encode,
                KirikoBookTeleportPacket::decode,
                KirikoBookTeleportPacket::handle
        );
    }
}
