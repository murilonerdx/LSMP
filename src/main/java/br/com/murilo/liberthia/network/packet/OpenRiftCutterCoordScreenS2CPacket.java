package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r185 — S2C: abre a GUI de coordenadas da Adaga Corta-Fendas, pré-preenchida com o que já
 * está salvo no NBT. O screen-opening é roteado por dispatch client-only (server-safe).
 */
public class OpenRiftCutterCoordScreenS2CPacket {
    private final int x, y, z;
    private final String dim;

    public OpenRiftCutterCoordScreenS2CPacket(int x, int y, int z, String dim) {
        this.x = x; this.y = y; this.z = z; this.dim = dim;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getDim() { return dim; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); buf.writeUtf(dim);
    }

    public static OpenRiftCutterCoordScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenRiftCutterCoordScreenS2CPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf());
    }

    public static void handle(OpenRiftCutterCoordScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.ClientRiftCutterDispatch.openCoordScreen(msg)));
        ctx.get().setPacketHandled(true);
    }
}
