package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C — manda o CLIENTE capturar a tela atual e salvá-la como a foto {@code photoId}
 * (ver {@code PhotoStore.capture}). Disparado quando o jogador usa a {@link
 * br.com.murilo.liberthia.cosmic.idol.CameraItem}. A captura é client-side porque só
 * o cliente tem o framebuffer renderizado.
 */
public class CapturePhotoS2CPacket {

    private final String photoId;

    public CapturePhotoS2CPacket(String photoId) {
        this.photoId = photoId;
    }

    public static void encode(CapturePhotoS2CPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.photoId);
    }

    public static CapturePhotoS2CPacket decode(FriendlyByteBuf buf) {
        return new CapturePhotoS2CPacket(buf.readUtf());
    }

    public static void handle(CapturePhotoS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.capture(pkt)));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void capture(CapturePhotoS2CPacket pkt) {
            net.minecraft.client.Minecraft.getInstance().execute(() ->
                    br.com.murilo.liberthia.cosmic.idol.PhotoStore.capture(pkt.photoId));
        }
    }
}
