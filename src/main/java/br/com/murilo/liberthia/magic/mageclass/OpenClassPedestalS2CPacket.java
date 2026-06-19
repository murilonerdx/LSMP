package br.com.murilo.liberthia.magic.mageclass;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: server → client pra abrir a tela de seleção de classe quando o
 * player right-clicka no Class Pedestal.
 *
 * <p>O packet carrega o nome da classe atual (se houver) e o level atual
 * pra mostrar como "selecionada" na UI.
 */
public class OpenClassPedestalS2CPacket {

    /** Nome da classe atual ou string vazia. */
    public final String currentClass;
    /** Level da classe atual (0-10). */
    public final int currentLevel;

    public OpenClassPedestalS2CPacket(String currentClass, int currentLevel) {
        this.currentClass = currentClass == null ? "" : currentClass;
        this.currentLevel = currentLevel;
    }

    public static void encode(OpenClassPedestalS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.currentClass, 32);
        buf.writeInt(pkt.currentLevel);
    }

    public static OpenClassPedestalS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenClassPedestalS2CPacket(buf.readUtf(32), buf.readInt());
    }

    public static void handle(OpenClassPedestalS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.open(pkt)));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void open(OpenClassPedestalS2CPacket pkt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new br.com.murilo.liberthia.magic.mageclass.client.ClassPedestalScreen(
                        pkt.currentClass, pkt.currentLevel));
            }
        }
    }
}
