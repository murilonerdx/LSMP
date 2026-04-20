package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.config.DevMode;
import br.com.murilo.liberthia.data.InfectionToggleData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.S2CInfectionTogglePacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Comando para ligar/desligar a proliferação da infecção.
 * O estado é salvo no mundo e persiste entre reinícios do servidor.
 *
 * /liberthia infection on  — liga a infecção (blocos proliferam)
 * /liberthia infection off — desliga (blocos podem ser colocados mas não proliferam)
 * /liberthia infection     — mostra o estado atual
 */
public class InfectionToggleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("infection")
                        .then(Commands.literal("on")
                                .executes(ctx -> setInfection(ctx.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> setInfection(ctx.getSource(), false)))
                        .executes(ctx -> showStatus(ctx.getSource()))
                )
        );
    }

    private static int setInfection(CommandSourceStack source, boolean enabled) {
        ServerLevel level = source.getLevel();
        InfectionToggleData data = InfectionToggleData.get(level);
        data.setInfectionEnabled(enabled);

        // Update the in-memory flag (DevMode.ACTIVE = !enabled)
        DevMode.ACTIVE = !enabled;

        // Sync to ALL online players
        S2CInfectionTogglePacket packet = new S2CInfectionTogglePacket(enabled);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(player, packet);
        }

        // broadcastToAdmins=false: silent to everyone except the executor.
        if (enabled) {
            source.sendSuccess(() -> Component.literal(
                    "§a[Liberthia] §fInfecção §aLIGADA§f — blocos de infecção agora proliferam."), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "§c[Liberthia] §fInfecção §cDESLIGADA§f — blocos podem ser colocados mas não proliferam."), false);
        }

        return 1;
    }

    private static int showStatus(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        InfectionToggleData data = InfectionToggleData.get(level);
        boolean enabled = data.isInfectionEnabled();

        if (enabled) {
            source.sendSuccess(() -> Component.literal(
                    "§d[Liberthia] §fEstado da infecção: §aLIGADA"), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "§d[Liberthia] §fEstado da infecção: §cDESLIGADA"), false);
        }

        return 1;
    }
}
