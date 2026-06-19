package br.com.murilo.liberthia.observation.network;

import br.com.murilo.liberthia.observation.client.SourceHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r71: S2C — atualiza posição do HUD do Source no client.
 */
public class HudPositionS2CPacket {

    private final String position;

    public HudPositionS2CPacket(String position) {
        this.position = position;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(position);
    }

    public static HudPositionS2CPacket decode(FriendlyByteBuf buf) {
        return new HudPositionS2CPacket(buf.readUtf());
    }

    public static void handle(HudPositionS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    SourceHud.position = SourceHud.HudPosition.valueOf(msg.position);
                } catch (IllegalArgumentException ignored) {}
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
