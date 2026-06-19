package br.com.murilo.liberthia.magic.mageclass;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: client → server. Player escolheu uma classe na UI ou clicou
 * "Remover Classe".
 */
public class SelectClassC2SPacket {

    /** Nome da classe escolhida ou string vazia pra remover. */
    public final String className;

    public SelectClassC2SPacket(String className) {
        this.className = className == null ? "" : className;
    }

    public static void encode(SelectClassC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.className, 32);
    }

    public static SelectClassC2SPacket decode(FriendlyByteBuf buf) {
        return new SelectClassC2SPacket(buf.readUtf(32));
    }

    public static void handle(SelectClassC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;

            if (pkt.className.isEmpty()) {
                MageClassData.set(sp, null);
                sp.sendSystemMessage(Component.literal("§7Classe de mago §lremovida§r§7."));
                return;
            }

            MageClass chosen;
            try {
                chosen = MageClass.valueOf(pkt.className);
            } catch (IllegalArgumentException e) {
                return;  // classe inválida — ignora silenciosamente
            }
            MageClassData.set(sp, chosen);
            int lv = MageClassData.getLevel(sp);
            sp.sendSystemMessage(Component.literal(
                    chosen.colorCode + "§l✦ Você é agora um " + chosen.displayName + " §r"
                            + "§7(nível §e" + lv + "§7/10, §a+" + chosen.dmgBonusAt(lv) + "%§7 dano)"));
        });
        ctx.get().setPacketHandled(true);
    }
}
