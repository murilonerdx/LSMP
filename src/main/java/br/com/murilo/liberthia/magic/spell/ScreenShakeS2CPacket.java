package br.com.murilo.liberthia.magic.spell;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

/**
 * v0.1.145 r113: <b>ScreenShakeS2CPacket</b> — server avisa cliente pra
 * sacudir a câmera por N ticks com intensidade I.
 *
 * <p>Cliente lê via {@link ScreenShakeClient#trigger}. A intensidade
 * decai linearmente por tick. Aplicado em {@link ScreenShakeClient#onComputeFov}
 * (modifica yRot/xRot da camera).
 *
 * <p>Original code — padrão Forge SimpleChannel packet, escrito do zero.
 */
public class ScreenShakeS2CPacket {

    public final float intensity;
    public final int durationTicks;

    public ScreenShakeS2CPacket(float intensity, int durationTicks) {
        this.intensity = Math.max(0F, Math.min(2.0F, intensity));
        this.durationTicks = Math.max(1, Math.min(40, durationTicks));
    }

    public static void encode(ScreenShakeS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.intensity);
        buf.writeInt(pkt.durationTicks);
    }

    public static ScreenShakeS2CPacket decode(FriendlyByteBuf buf) {
        return new ScreenShakeS2CPacket(buf.readFloat(), buf.readInt());
    }

    public static void handle(ScreenShakeS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ScreenShakeClient.trigger(pkt.intensity, pkt.durationTicks));
        });
        ctx.get().setPacketHandled(true);
    }
}
