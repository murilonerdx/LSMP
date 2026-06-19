package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * S2C: instrui o cliente a redirecionar a câmera para o player com UUID
 * {@code targetUuid} (Vision Swap). O ClientHandler resolve a entidade no
 * Level corrente e chama {@code Minecraft.setCameraEntity(target)}.
 *
 * <p>Se o cliente não conseguir resolver a entidade (ex.: target em outro
 * dim ou ainda não loaded), o ClientHandler é no-op e o swap fica visualmente
 * inerte; o server mantém o estado e re-tenta a cada vez que o packet for
 * reenviado. Por enquanto não há retry — caller deve garantir mesma dim.
 */
public class StartVisionSwapS2CPacket {

    private final UUID targetUuid;

    public StartVisionSwapS2CPacket(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public static void encode(StartVisionSwapS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.targetUuid);
    }

    public static StartVisionSwapS2CPacket decode(FriendlyByteBuf buf) {
        return new StartVisionSwapS2CPacket(buf.readUUID());
    }

    public static void handle(StartVisionSwapS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.VisionSwapClient.start(pkt.targetUuid)));
        context.setPacketHandled(true);
    }
}
