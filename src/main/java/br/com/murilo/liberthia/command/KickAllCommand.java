package br.com.murilo.liberthia.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /liberthia kickall [motivo]} — desconecta TODOS os jogadores do server.
 * OP (permission 2). Motivo opcional (texto livre) vira a tela de desconexão.
 */
public final class KickAllCommand {

    private KickAllCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("kickall")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> kick(ctx.getSource(),
                                "§cO servidor foi encerrado pelo administrador."))
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                .executes(ctx -> kick(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "motivo"))))));
    }

    private static int kick(CommandSourceStack src, String reason) {
        if (src.getServer() == null) return 0;
        List<ServerPlayer> players = new ArrayList<>(src.getServer().getPlayerList().getPlayers());
        final int count = players.size();
        // feedback ANTES de kickar (caso o próprio admin esteja na lista)
        src.sendSuccess(() -> Component.literal(
                "§5Liberthia: §fkickando §a" + count + "§f jogador(es)."), true);
        Component msg = Component.literal(reason);
        for (ServerPlayer p : players) {
            p.connection.disconnect(msg);
        }
        return count;
    }
}
