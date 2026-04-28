package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.network.packet.OpenWorkerTeleporterScreenS2CPacket;
import net.minecraft.client.Minecraft;

/**
 * Client-only dispatch. Loaded exclusively via DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...).
 * MUST NOT be referenced from any common-side class body, to avoid RuntimeDistCleaner
 * triggering on the dedicated server.
 */
public final class ClientWorkerTeleporterDispatch {
    private ClientWorkerTeleporterDispatch() {}

    public static void open(OpenWorkerTeleporterScreenS2CPacket msg) {
        Minecraft.getInstance().setScreen(new WorkerTeleporterScreen(msg.getPlayers()));
    }
}
