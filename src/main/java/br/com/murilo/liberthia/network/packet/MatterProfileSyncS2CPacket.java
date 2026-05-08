package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.matter.ClientMatterProfileCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C — sincroniza valores de matéria do server pro client. O cliente armazena
 * num cache estático que o HUD lê.
 */
public record MatterProfileSyncS2CPacket(float dark, float white, float yellow) {

    public static void encode(MatterProfileSyncS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.dark);
        buf.writeFloat(pkt.white);
        buf.writeFloat(pkt.yellow);
    }

    public static MatterProfileSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new MatterProfileSyncS2CPacket(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(MatterProfileSyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientMatterProfileCache.update(pkt.dark, pkt.white, pkt.yellow);
                }));
        ctx.get().setPacketHandled(true);
    }
}
