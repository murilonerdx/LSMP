package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.client.gui.CommandTabletScreen;
import br.com.murilo.liberthia.network.packet.OpenCommandTabletScreenS2CPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-only dispatch for {@link OpenCommandTabletScreenS2CPacket}. Loaded
 * exclusively via DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...).
 */
public final class ClientCommandTabletDispatch {
    private ClientCommandTabletDispatch() {}

    public static void open(OpenCommandTabletScreenS2CPacket msg) {
        Minecraft.getInstance().setScreen(new CommandTabletScreen(
                msg.getCommands(), msg.getLabel(),
                msg.getTargetMode(), msg.getTargetName(),
                msg.isVerbose()));
    }
}
