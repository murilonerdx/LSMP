package br.com.murilo.liberthia.cosmic.hallucination;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r40: <b>Hallucination S2C Packet</b> — server injeta uma
 * hallucination específica no client target.
 *
 * <h2>Payload</h2>
 * <ul>
 *   <li>type ordinal (byte)</li>
 *   <li>x, y, z (float ×3) — pos relativa/absoluta</li>
 *   <li>duration (short) — ticks</li>
 *   <li>intensity (float) — 0-1</li>
 *   <li>variant (byte) — sub-tipo</li>
 *   <li>aux (string) — texto extra (chat msg, sound id, entity name)</li>
 * </ul>
 *
 * <p>Handler client-side delega pro {@link HallucinationClientHandler} que
 * sabe renderizar/tocar cada tipo.
 */
public class HallucinationS2CPacket {

    public final HallucinationType type;
    public final float x, y, z;
    public final int duration;
    public final float intensity;
    public final int variant;
    public final String aux;

    public HallucinationS2CPacket(HallucinationType type, float x, float y, float z,
                                   int duration, float intensity, int variant, String aux) {
        this.type = type;
        this.x = x; this.y = y; this.z = z;
        this.duration = duration;
        this.intensity = intensity;
        this.variant = variant;
        this.aux = aux == null ? "" : aux;
    }

    /** Convenience: hallucination simples sem posição. */
    public HallucinationS2CPacket(HallucinationType type, int duration, float intensity, String aux) {
        this(type, 0, 0, 0, duration, intensity, 0, aux);
    }

    public static void encode(HallucinationS2CPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.type.ord());
        buf.writeFloat(p.x);
        buf.writeFloat(p.y);
        buf.writeFloat(p.z);
        buf.writeShort(Math.min(p.duration, Short.MAX_VALUE));
        buf.writeFloat(p.intensity);
        buf.writeByte(p.variant);
        buf.writeUtf(p.aux == null ? "" : p.aux, 256);
    }

    public static HallucinationS2CPacket decode(FriendlyByteBuf buf) {
        HallucinationType t = HallucinationType.byOrdinal(buf.readByte());
        float x = buf.readFloat(), y = buf.readFloat(), z = buf.readFloat();
        int dur = buf.readShort();
        float intensity = buf.readFloat();
        int variant = buf.readByte();
        String aux = buf.readUtf(256);
        return new HallucinationS2CPacket(t, x, y, z, dur, intensity, variant, aux);
    }

    public static void handle(HallucinationS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            try {
                HallucinationClientHandler.dispatch(pkt);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.warn("[Hallucination] dispatch error: {}", t.toString());
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
