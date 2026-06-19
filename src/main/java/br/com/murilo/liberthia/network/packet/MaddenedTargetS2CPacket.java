package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r23: S2C — sinaliza que o alvo está sob Maddening Gaze. Client
 * exibe "VOCÊ ESTÁ ENLOUQUECENDO — FUJA" em vermelho piscando por ~2s.
 */
public class MaddenedTargetS2CPacket {

    public MaddenedTargetS2CPacket() {}

    public static void encode(MaddenedTargetS2CPacket p, FriendlyByteBuf buf) {}

    public static MaddenedTargetS2CPacket decode(FriendlyByteBuf buf) {
        return new MaddenedTargetS2CPacket();
    }

    public static void handle(MaddenedTargetS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.MadnessClient.onMaddenedTarget()));
        ctx.get().setPacketHandled(true);
    }
}
