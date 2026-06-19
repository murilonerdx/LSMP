package br.com.murilo.liberthia.cosmic.scare;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r173: S2C — injeta um efeito de 4ª parede no client do player alvo.
 */
public class ScareS2CPacket {

    public final ScareType type;
    public final int duration;   // ticks
    public final int variant;    // índice de imagem / sub-variante
    public final String text;    // texto custom (SHAKE/ALERT/KICK)

    public ScareS2CPacket(ScareType type, int duration, int variant, String text) {
        this.type = type;
        this.duration = duration;
        this.variant = variant;
        this.text = text == null ? "" : text;
    }

    public static void encode(ScareS2CPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.type.ordinal());
        buf.writeVarInt(p.duration);
        buf.writeVarInt(p.variant);
        buf.writeUtf(p.text, 512);
    }

    public static ScareS2CPacket decode(FriendlyByteBuf buf) {
        return new ScareS2CPacket(ScareType.byOrdinal(buf.readByte()),
                buf.readVarInt(), buf.readVarInt(), buf.readUtf(512));
    }

    public static void handle(ScareS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                ScareClient.receive(pkt);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[Scare] client error: {}", t.toString());
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
