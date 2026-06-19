package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r23: S2C — Mirror of Insanity ativo no client por {@code ticks}.
 * Overlay "QUEM SOU EU?" flashea + outros players são renderizados com a
 * skin do própio player (render override).
 */
public class MirrorInsanityS2CPacket {

    private final int ticks;

    public MirrorInsanityS2CPacket(int ticks) {
        this.ticks = ticks;
    }

    public static void encode(MirrorInsanityS2CPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.ticks);
    }

    public static MirrorInsanityS2CPacket decode(FriendlyByteBuf buf) {
        return new MirrorInsanityS2CPacket(buf.readVarInt());
    }

    public static void handle(MirrorInsanityS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.MadnessClient.onMirrorActive(pkt.ticks)));
        ctx.get().setPacketHandled(true);
    }
}
