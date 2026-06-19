package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C — sincroniza o estado da Lua do Medo (ativa + cor RGB) pro cliente.
 * Atualiza {@code ClientFearMoonState} (lido pelo render da lua/tint do céu).
 */
public class FearMoonSyncS2CPacket {
    public final boolean active;
    public final int color;

    public FearMoonSyncS2CPacket(boolean active, int color) {
        this.active = active;
        this.color = color;
    }

    public static void encode(FearMoonSyncS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.active);
        buf.writeInt(p.color);
    }

    public static FearMoonSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new FearMoonSyncS2CPacket(buf.readBoolean(), buf.readInt());
    }

    public static void handle(FearMoonSyncS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> br.com.murilo.liberthia.client.ClientFearMoonState.update(msg.active, msg.color)));
        ctx.get().setPacketHandled(true);
    }
}
