package br.com.murilo.liberthia.cosmic.observatory.console;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r50: C2S — admin pede pra parar uma sessão ativa.
 */
public class StopCaretakerSessionC2SPacket {

    private final String targetName;

    public StopCaretakerSessionC2SPacket(String targetName) {
        this.targetName = targetName;
    }

    public static void encode(StopCaretakerSessionC2SPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.targetName, 64);
    }

    public static StopCaretakerSessionC2SPacket decode(FriendlyByteBuf buf) {
        return new StopCaretakerSessionC2SPacket(buf.readUtf(64));
    }

    public static void handle(StopCaretakerSessionC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            if (!sender.hasPermissions(2) && !sender.isCreative()) return;

            ServerPlayer target = sender.server.getPlayerList().getPlayerByName(p.targetName);
            if (target == null) {
                sender.displayClientMessage(Component.literal(
                        "§cTarget não encontrado"), true);
                return;
            }
            CaretakerSessionManager.stop(target.getUUID());
            sender.displayClientMessage(Component.literal(
                    "§a✓ Sessão de " + p.targetName + " parada"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
