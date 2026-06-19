package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C → possessor: avisa o cliente do possessor para começar a redirecionar
 * a câmera para a entidade {@code targetEntityId} (ID inteiro do network),
 * e marcar flags de possession ativa.
 *
 * <p>Usamos entity ID (int) ao invés de UUID porque o lookup client-side
 * por ID via {@code Level.getEntity(int)} é mais rápido e cobre mob e player
 * sem custo extra.
 */
public class StartPossessionS2CPacket {

    private final int targetEntityId;

    public StartPossessionS2CPacket(int entityId) {
        this.targetEntityId = entityId;
    }

    public static void encode(StartPossessionS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.targetEntityId);
    }

    public static StartPossessionS2CPacket decode(FriendlyByteBuf buf) {
        return new StartPossessionS2CPacket(buf.readVarInt());
    }

    public static void handle(StartPossessionS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.PossessionClient.startAsPossessor(pkt.targetEntityId)));
        context.setPacketHandled(true);
    }
}
