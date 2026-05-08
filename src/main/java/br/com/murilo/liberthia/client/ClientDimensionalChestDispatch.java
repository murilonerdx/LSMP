package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.client.screen.DimensionalChestChannelScreen;
import br.com.murilo.liberthia.network.packet.OpenDimensionalChannelScreenS2CPacket;
import net.minecraft.client.Minecraft;

public final class ClientDimensionalChestDispatch {
    private ClientDimensionalChestDispatch() {}

    public static void openChannelScreen(OpenDimensionalChannelScreenS2CPacket msg) {
        Minecraft.getInstance().setScreen(
                new DimensionalChestChannelScreen(msg.getPos(), msg.getChannel()));
    }
}
