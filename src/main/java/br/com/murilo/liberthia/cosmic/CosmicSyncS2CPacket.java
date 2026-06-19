package br.com.murilo.liberthia.cosmic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r35: Sync server→client do estado Cosmic Horror.
 *
 * <p>Server manda phase ordinal + intensity (float 0-1). Client armazena em
 * {@link CosmicClientState} pra ser usado por todos os renderers.
 */
public class CosmicSyncS2CPacket {

    public final int phaseOrdinal;
    public final float intensity;

    public CosmicSyncS2CPacket(int phaseOrdinal, float intensity) {
        this.phaseOrdinal = phaseOrdinal;
        this.intensity = intensity;
    }

    public static void encode(CosmicSyncS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.phaseOrdinal);
        buf.writeFloat(pkt.intensity);
    }

    public static CosmicSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new CosmicSyncS2CPacket(buf.readInt(), buf.readFloat());
    }

    public static void handle(CosmicSyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                CosmicClientState.update(
                        CosmicHorrorPhase.byOrdinal(pkt.phaseOrdinal),
                        pkt.intensity);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[Cosmic] sync handler error: {}", t.toString());
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
