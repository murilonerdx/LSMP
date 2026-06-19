package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.client.screen.BlockInfoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * r164: server → client pra abrir uma {@link BlockInfoScreen} genérica com
 * informações dinâmicas sobre um bloco. Usado pelos blocos sem GUI dedicada
 * (Mage Cauldron, Auto Miner, Scroll Forge, etc.).
 */
public class OpenBlockInfoS2CPacket {

    public final String title;
    public final List<String> stats;
    public final List<String> instructions;
    /** Cor de acento (gold/red/blue/etc) em RGB sem alpha. */
    public final int accentColor;

    public OpenBlockInfoS2CPacket(String title, List<String> stats,
                                   List<String> instructions, int accentColor) {
        this.title = title;
        this.stats = stats;
        this.instructions = instructions;
        this.accentColor = accentColor;
    }

    public static void encode(OpenBlockInfoS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.title, 64);
        buf.writeInt(pkt.accentColor);
        buf.writeVarInt(pkt.stats.size());
        for (String s : pkt.stats) buf.writeUtf(s, 200);
        buf.writeVarInt(pkt.instructions.size());
        for (String s : pkt.instructions) buf.writeUtf(s, 200);
    }

    public static OpenBlockInfoS2CPacket decode(FriendlyByteBuf buf) {
        String title = buf.readUtf(64);
        int color = buf.readInt();
        int statCount = buf.readVarInt();
        List<String> stats = new ArrayList<>(statCount);
        for (int i = 0; i < statCount; i++) stats.add(buf.readUtf(200));
        int instrCount = buf.readVarInt();
        List<String> instrs = new ArrayList<>(instrCount);
        for (int i = 0; i < instrCount; i++) instrs.add(buf.readUtf(200));
        return new OpenBlockInfoS2CPacket(title, stats, instrs, color);
    }

    public static void handle(OpenBlockInfoS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.open(pkt)));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void open(OpenBlockInfoS2CPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new BlockInfoScreen(
                        pkt.title, pkt.stats, pkt.instructions, pkt.accentColor));
            }
        }
    }
}
