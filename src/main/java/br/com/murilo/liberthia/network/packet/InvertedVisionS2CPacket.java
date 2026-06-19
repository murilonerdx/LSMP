package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → UM player: ativa a "visão invertida" do {@code Mapa Invertido} por
 * {@code durationTicks} ticks. O cliente carrega o post-shader vanilla
 * {@code shaders/post/invert.json} e o desliga sozinho quando o tempo acaba.
 */
public class InvertedVisionS2CPacket {

    public final int durationTicks;

    public InvertedVisionS2CPacket(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public static void encode(InvertedVisionS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.durationTicks);
    }

    public static InvertedVisionS2CPacket decode(FriendlyByteBuf buf) {
        return new InvertedVisionS2CPacket(buf.readInt());
    }

    public static void handle(InvertedVisionS2CPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.ClientInvertedVision.activate(p.durationTicks)));
        ctx.get().setPacketHandled(true);
    }

    public static void send(ServerPlayer player, int durationTicks) {
        ModNetwork.sendToPlayer(player, new InvertedVisionS2CPacket(durationTicks));
    }
}
