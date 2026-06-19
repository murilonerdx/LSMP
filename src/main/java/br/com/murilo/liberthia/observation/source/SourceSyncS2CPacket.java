package br.com.murilo.liberthia.observation.source;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.161 r136: <b>SourceSyncS2CPacket</b> — server avisa cliente quando
 * Source / MaxSource do player muda. Sem isso, o HUD do client mostra
 * valor desatualizado (porque getPersistentData() nao sincroniza auto).
 */
public class SourceSyncS2CPacket {

    public final int source;
    public final int maxSource;

    public SourceSyncS2CPacket(int source, int maxSource) {
        this.source = source;
        this.maxSource = maxSource;
    }

    public static void encode(SourceSyncS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.source);
        buf.writeInt(pkt.maxSource);
    }

    public static SourceSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new SourceSyncS2CPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(SourceSyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.apply(pkt)));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void apply(SourceSyncS2CPacket pkt) {
            var mc = Minecraft.getInstance();
            if (mc.player != null) {
                SourceData.set(mc.player, pkt.source);
                SourceData.setMax(mc.player, pkt.maxSource);
            }
        }
    }
}
