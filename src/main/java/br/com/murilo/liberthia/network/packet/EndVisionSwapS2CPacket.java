package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: instrui o cliente a restaurar sua câmera local (final de Vision Swap).
 * Aciona {@code Minecraft.setCameraEntity(localPlayer)} e libera os flags
 * de input lock.
 */
public class EndVisionSwapS2CPacket {

    public EndVisionSwapS2CPacket() {}

    public static void encode(EndVisionSwapS2CPacket pkt, FriendlyByteBuf buf) {
        // sem payload
    }

    public static EndVisionSwapS2CPacket decode(FriendlyByteBuf buf) {
        return new EndVisionSwapS2CPacket();
    }

    public static void handle(EndVisionSwapS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.VisionSwapClient.end()));
        context.setPacketHandled(true);
    }
}
