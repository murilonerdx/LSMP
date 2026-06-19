package br.com.murilo.liberthia.cosmic.observatory.console;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.22 r50: C2S — admin envia configuração da sessão pra server iniciar.
 *
 * <p>Payload:
 * <ul>
 *   <li>targetName: nome do target player</li>
 *   <li>intervalSec: intervalo entre mensagens (3-300s)</li>
 *   <li>durationSec: duração total (-1 = permanente)</li>
 *   <li>messages: lista resolvida (já com placeholders substituídos)</li>
 * </ul>
 */
public class StartCaretakerSessionC2SPacket {

    private final String targetName;
    private final int intervalSec;
    private final int durationSec;
    private final List<String> messages;

    public StartCaretakerSessionC2SPacket(String targetName, int intervalSec,
                                           int durationSec, List<String> messages) {
        this.targetName = targetName;
        this.intervalSec = intervalSec;
        this.durationSec = durationSec;
        this.messages = messages;
    }

    public static void encode(StartCaretakerSessionC2SPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.targetName, 64);
        buf.writeVarInt(p.intervalSec);
        buf.writeVarInt(p.durationSec);
        buf.writeVarInt(p.messages.size());
        for (String m : p.messages) buf.writeUtf(m, 256);
    }

    public static StartCaretakerSessionC2SPacket decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(64);
        int interval = buf.readVarInt();
        int duration = buf.readVarInt();
        int n = buf.readVarInt();
        List<String> msgs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) msgs.add(buf.readUtf(256));
        return new StartCaretakerSessionC2SPacket(name, interval, duration, msgs);
    }

    public static void handle(StartCaretakerSessionC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            // Admin check
            if (!sender.hasPermissions(2) && !sender.isCreative()) {
                sender.displayClientMessage(Component.literal(
                        "§cVocê não é Caretaker"), true);
                return;
            }

            // Acha target pelo nome
            ServerPlayer target = sender.server.getPlayerList().getPlayerByName(p.targetName);
            if (target == null) {
                sender.displayClientMessage(Component.literal(
                        "§cTarget '" + p.targetName + "' não encontrado online."), true);
                return;
            }

            if (p.messages.isEmpty()) {
                sender.displayClientMessage(Component.literal(
                        "§cSelecione pelo menos uma mensagem."), true);
                return;
            }

            int intervalTicks = Math.max(60, p.intervalSec * 20);
            int durationTicks = p.durationSec == -1 ? -1 : p.durationSec * 20;

            CaretakerSession session = new CaretakerSession(
                    target.getUUID(),
                    p.messages,
                    intervalTicks,
                    durationTicks,
                    sender.getUUID());

            CaretakerSessionManager.start(session);

            sender.displayClientMessage(Component.literal(
                    "§a§l✦ §r§aSessão iniciada em §6" + p.targetName
                            + "§a: §e" + p.messages.size() + "§a msgs, §6"
                            + p.intervalSec + "s§a interval, §6"
                            + (p.durationSec == -1 ? "permanente"
                                    : p.durationSec + "s")), false);
        });
        ctx.get().setPacketHandled(true);
    }
}
