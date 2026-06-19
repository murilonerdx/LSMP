package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r23: S2C — disparar alucinação cosmética no client do alvo da
 * Madness Aura. Tipo:
 * <ul>
 *   <li><b>0</b>: SCREEN_FLASH — chars random aparecem brevemente em texto vermelho</li>
 *   <li><b>1</b>: FAKE_SOUND — som de mob aleatório (zombie/skeleton/etc) em volume baixo</li>
 *   <li><b>2</b>: WHISPER — partículas de "sussurro" + som soul_escape</li>
 *   <li><b>3</b>: VOID_FLASH — tela escurece por 200ms (blackout brief)</li>
 * </ul>
 *
 * <p>{@code seed} é random pra variar o conteúdo (texto exibido, etc).
 */
public class MadnessHallucinationS2CPacket {

    private final int type;
    private final int seed;

    public MadnessHallucinationS2CPacket(int type, int seed) {
        this.type = type;
        this.seed = seed;
    }

    public static void encode(MadnessHallucinationS2CPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.type);
        buf.writeVarInt(p.seed);
    }

    public static MadnessHallucinationS2CPacket decode(FriendlyByteBuf buf) {
        return new MadnessHallucinationS2CPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MadnessHallucinationS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.MadnessClient.onHallucination(pkt.type, pkt.seed)));
        ctx.get().setPacketHandled(true);
    }
}
