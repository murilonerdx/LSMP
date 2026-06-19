package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r24: S2C — sincroniza sanidade atual pro client renderizar HUD bar.
 */
public class SanitySyncS2CPacket {

    private final int sanity;

    public SanitySyncS2CPacket(int sanity) {
        this.sanity = sanity;
    }

    public static void encode(SanitySyncS2CPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.sanity);
    }

    public static SanitySyncS2CPacket decode(FriendlyByteBuf buf) {
        return new SanitySyncS2CPacket(buf.readVarInt());
    }

    public static void handle(SanitySyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.SpiritWorldClient.onSanitySync(pkt.sanity)));
        ctx.get().setPacketHandled(true);
    }
}
