package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r187 — S2C broadcast do estado de Astaron (radiação 0-100, nº de cósmicos vivos, cor do céu RGB).
 * Atualiza o {@code AstaronClientState} no cliente.
 */
public class S2CAstaronSyncPacket {
    public final int radiation;
    public final int cosmicCount;
    public final int skyColor;

    public S2CAstaronSyncPacket(int radiation, int cosmicCount, int skyColor) {
        this.radiation = radiation; this.cosmicCount = cosmicCount; this.skyColor = skyColor;
    }

    public static void encode(S2CAstaronSyncPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.radiation); buf.writeInt(p.cosmicCount); buf.writeInt(p.skyColor);
    }

    public static S2CAstaronSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CAstaronSyncPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(S2CAstaronSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> br.com.murilo.liberthia.client.AstaronClientState.update(msg.radiation, msg.cosmicCount, msg.skyColor)));
        ctx.get().setPacketHandled(true);
    }
}
