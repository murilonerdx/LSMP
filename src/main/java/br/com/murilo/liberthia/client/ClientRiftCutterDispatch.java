package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.client.screen.RiftCutterCoordScreen;
import br.com.murilo.liberthia.network.packet.OpenRiftCutterCoordScreenS2CPacket;
import net.minecraft.client.Minecraft;

/**
 * Dispatch client-only (carregado só via DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)).
 * NUNCA referenciar do corpo de classe comum (evita crash no servidor dedicado).
 */
public final class ClientRiftCutterDispatch {
    private ClientRiftCutterDispatch() {}

    public static void openCoordScreen(OpenRiftCutterCoordScreenS2CPacket msg) {
        Minecraft.getInstance().setScreen(new RiftCutterCoordScreen(msg));
    }
}
