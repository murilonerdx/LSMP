package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: opens the LiberScript editor on the client, prefilled with the source. */
public class OpenScriptTabletScreenS2CPacket {
    private final String source;
    private final String label;
    private final boolean verbose;

    public OpenScriptTabletScreenS2CPacket(String source, String label, boolean verbose) {
        this.source = source == null ? "" : source;
        this.label = label == null ? "" : label;
        this.verbose = verbose;
    }

    public String getSource() { return source; }
    public String getLabel() { return label; }
    public boolean isVerbose() { return verbose; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(source, 16_000);
        buf.writeUtf(label, 64);
        buf.writeBoolean(verbose);
    }

    public static OpenScriptTabletScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenScriptTabletScreenS2CPacket(
                buf.readUtf(16_000), buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(OpenScriptTabletScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.ClientScriptTabletDispatch.open(msg)));
        ctx.get().setPacketHandled(true);
    }
}
