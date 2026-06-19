package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C → target player de uma possessão: liga/desliga o flag
 * {@code PossessionClient.locked}. Enquanto ligado, o
 * {@code MovementInputUpdateEvent} no cliente target zera todos os inputs.
 */
public class LockedInputS2CPacket {

    private final boolean locked;

    public LockedInputS2CPacket(boolean locked) {
        this.locked = locked;
    }

    public static void encode(LockedInputS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.locked);
    }

    public static LockedInputS2CPacket decode(FriendlyByteBuf buf) {
        return new LockedInputS2CPacket(buf.readBoolean());
    }

    public static void handle(LockedInputS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.PossessionClient.setLocked(pkt.locked)));
        context.setPacketHandled(true);
    }
}
