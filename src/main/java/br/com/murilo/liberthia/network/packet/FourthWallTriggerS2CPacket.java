package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.fourthwall.FourthWallFeature;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client: dispara UM efeito de quarta parede client-side.
 *
 * <ul>
 *   <li>{@code feature} = ordinal de {@link FourthWallFeature}</li>
 *   <li>{@code text} = texto auxiliar (ex.: causa de morte críptica); pode ser vazio</li>
 *   <li>{@code duration} = ticks do efeito (ou -1 = caso especial, ex.: abrir tela de
 *       morte AGORA no teste)</li>
 * </ul>
 */
public class FourthWallTriggerS2CPacket {

    public final int feature;
    public final String text;
    public final int duration;

    public FourthWallTriggerS2CPacket(FourthWallFeature f, String text, int duration) {
        this(f.ordinal(), text, duration);
    }

    public FourthWallTriggerS2CPacket(int feature, String text, int duration) {
        this.feature = feature;
        this.text = text == null ? "" : text;
        this.duration = duration;
    }

    public static void encode(FourthWallTriggerS2CPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.feature);
        buf.writeUtf(p.text);
        buf.writeVarInt(p.duration);
    }

    public static FourthWallTriggerS2CPacket decode(FriendlyByteBuf buf) {
        return new FourthWallTriggerS2CPacket(buf.readVarInt(), buf.readUtf(), buf.readVarInt());
    }

    public static void handle(FourthWallTriggerS2CPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.fourthwall.FourthWallClient.onTrigger(
                                p.feature, p.text, p.duration)));
        ctx.get().setPacketHandled(true);
    }
}
