package br.com.murilo.liberthia.cosmic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r38: <b>Force Rotation</b> — server diz pro client adicionar
 * N graus à rotação (yaw/pitch) do player.
 *
 * <p>Usado pelas phases altas do Cosmic Horror pra simular "mouse girando
 * sozinho" como parte da paranoia. Aplica delta na rotação SEM resetar
 * a posição — só fala "spin sua câmera X graus".
 *
 * <p>Server-side: chamar via {@code ModNetwork.sendToPlayer(sp, new
 * CosmicForceRotationS2CPacket(yawDelta, pitchDelta))}.
 *
 * <p>Client recebe e aplica via {@link Minecraft#player#setYRot/setXRot}.
 */
public class CosmicForceRotationS2CPacket {

    public final float yawDelta;
    public final float pitchDelta;

    public CosmicForceRotationS2CPacket(float yawDelta, float pitchDelta) {
        this.yawDelta = yawDelta;
        this.pitchDelta = pitchDelta;
    }

    public static void encode(CosmicForceRotationS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.yawDelta);
        buf.writeFloat(pkt.pitchDelta);
    }

    public static CosmicForceRotationS2CPacket decode(FriendlyByteBuf buf) {
        return new CosmicForceRotationS2CPacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(CosmicForceRotationS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.apply(pkt)));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void apply(CosmicForceRotationS2CPacket pkt) {
            try {
                var mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.setYRot(mc.player.getYRot() + pkt.yawDelta);
                    mc.player.setXRot(Math.max(-90F, Math.min(90F,
                            mc.player.getXRot() + pkt.pitchDelta)));
                }
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[Cosmic] rotation handler error: {}", t.toString());
            }
        }
    }
}
