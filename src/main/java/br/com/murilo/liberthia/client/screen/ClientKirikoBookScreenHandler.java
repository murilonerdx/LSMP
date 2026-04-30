package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.packet.OpenKirikoBookScreenPacket;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ClientKirikoBookScreenHandler {

    private ClientKirikoBookScreenHandler() {
    }

    public static void open(
            List<OpenKirikoBookScreenPacket.DimensionEntry> dimensions,
            List<OpenKirikoBookScreenPacket.PlayerEntry> players
    ) {
        Minecraft.getInstance().setScreen(new KirikoBookScreen(dimensions, players));
    }
}
