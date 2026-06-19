package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: instrui o cliente do possessor a desfazer o redirecionamento de câmera
 * e limpar o flag {@code active} do {@code PossessionClient}.
 */
public class EndPossessionS2CPacket {

    public EndPossessionS2CPacket() {}

    public static void encode(EndPossessionS2CPacket pkt, FriendlyByteBuf buf) {}

    public static EndPossessionS2CPacket decode(FriendlyByteBuf buf) {
        return new EndPossessionS2CPacket();
    }

    public static void handle(EndPossessionS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.PossessionClient.endAsPossessor()));
        context.setPacketHandled(true);
    }
}
