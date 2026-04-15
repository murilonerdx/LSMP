package br.com.murilo.liberthia.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Pacote servidor→cliente para sincronizar o estado on/off da infecção.
 * O client usa isso para saber se deve renderizar overlays e partículas.
 */
public record S2CInfectionTogglePacket(boolean enabled) {

    public static void encode(S2CInfectionTogglePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static S2CInfectionTogglePacket decode(FriendlyByteBuf buf) {
        return new S2CInfectionTogglePacket(buf.readBoolean());
    }

    public static void handle(S2CInfectionTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Update the client-side flag
            br.com.murilo.liberthia.config.DevMode.ACTIVE = !msg.enabled;
        });
        ctx.get().setPacketHandled(true);
    }
}
