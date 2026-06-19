package br.com.murilo.liberthia.observation.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r65: S2C — abre a Composition Screen no client com o preset atual
 * do Grimório.
 */
public class OpenCompositionScreenS2CPacket {

    private final int currentPreset;

    public OpenCompositionScreenS2CPacket(int currentPreset) {
        this.currentPreset = currentPreset;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(currentPreset);
    }

    public static OpenCompositionScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenCompositionScreenS2CPacket(buf.readInt());
    }

    public static void handle(OpenCompositionScreenS2CPacket msg,
                               Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                br.com.murilo.liberthia.observation.client.SpellBookScreen
                    .openNow(msg.currentPreset));
        });
        ctx.get().setPacketHandled(true);
    }
}
