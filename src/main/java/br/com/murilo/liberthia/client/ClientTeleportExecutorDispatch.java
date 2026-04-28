package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.network.packet.OpenTeleportExecutorScreenS2CPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-only dispatch. Loaded exclusively via DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...).
 * MUST NOT be referenced from any common-side class body, to avoid RuntimeDistCleaner
 * triggering on the dedicated server.
 */
public final class ClientTeleportExecutorDispatch {
    private ClientTeleportExecutorDispatch() {}

    public static void open(OpenTeleportExecutorScreenS2CPacket msg) {
        Minecraft.getInstance().setScreen(new TeleportExecutorScreen(msg.getAnchor(), msg.getEntries()));
    }
}
