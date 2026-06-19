package br.com.murilo.liberthia.observation.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r65: S2C — server fala "casta animação no player local".
 *
 * <p>Pattern AN's PacketOneShotAnimation. Cliente recebe e:
 * <ul>
 *   <li>Faz swing de braço</li>
 *   <li>Spawn glow particles em volta do player local</li>
 * </ul>
 */
public class CastAnimationS2CPacket {

    private final int color;

    public CastAnimationS2CPacket(int color) {
        this.color = color;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(color);
    }

    public static CastAnimationS2CPacket decode(FriendlyByteBuf buf) {
        return new CastAnimationS2CPacket(buf.readInt());
    }

    public static void handle(CastAnimationS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                br.com.murilo.liberthia.observation.client.CastAnimationClient.play(msg.color));
        });
        ctx.get().setPacketHandled(true);
    }
}
