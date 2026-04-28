package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.client.gui.ScriptTabletScreen;
import br.com.murilo.liberthia.network.packet.OpenScriptTabletScreenS2CPacket;
import net.minecraft.client.Minecraft;

public final class ClientScriptTabletDispatch {
    private ClientScriptTabletDispatch() {}

    public static void open(OpenScriptTabletScreenS2CPacket msg) {
        Minecraft.getInstance().setScreen(
                new ScriptTabletScreen(msg.getSource(), msg.getLabel(), msg.isVerbose()));
    }
}
