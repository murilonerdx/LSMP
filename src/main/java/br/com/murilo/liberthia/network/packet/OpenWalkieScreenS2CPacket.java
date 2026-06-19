package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C — abre a tela do Walkie Talkie (código secreto + on/off) no cliente.
 */
public class OpenWalkieScreenS2CPacket {

    private final String freq;
    private final boolean on;

    public OpenWalkieScreenS2CPacket(String freq, boolean on) {
        this.freq = freq == null ? "" : freq;
        this.on = on;
    }

    public String getFreq() { return freq; }
    public boolean isOn() { return on; }

    public static void encode(OpenWalkieScreenS2CPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.freq, 32);
        buf.writeBoolean(p.on);
    }

    public static OpenWalkieScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenWalkieScreenS2CPacket(buf.readUtf(32), buf.readBoolean());
    }

    public static void handle(OpenWalkieScreenS2CPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.gui.WalkieTalkieScreen.openNow(p.freq, p.on)));
        ctx.get().setPacketHandled(true);
    }
}
