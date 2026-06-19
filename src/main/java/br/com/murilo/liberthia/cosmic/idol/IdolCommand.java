package br.com.murilo.liberthia.cosmic.idol;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r178: <b>/liberthia idol</b> — controla O Ídolo.
 * <pre>
 * /liberthia idol spawn &lt;alvos&gt;     invoca agora
 * /liberthia idol stop  &lt;alvos&gt;     remove o ativo
 * /liberthia idol rate  &lt;minutos&gt;   intervalo de spawn (slow burn)
 * /liberthia idol on | off | status
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class IdolCommand {

    private IdolCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("idol")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> {
                                            int n = 0;
                                            for (ServerPlayer p : EntityArgument.getPlayers(c, "alvos")) {
                                                if (IdolManager.forceSpawn(p)) n++;
                                            }
                                            final int fn = n;
                                            c.getSource().sendSuccess(() -> Component.literal(
                                                    "§5✦ O Ídolo invocado p/ " + fn + " player(s)."), false);
                                            return n;
                                        })))
                        .then(Commands.literal("stop")
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> {
                                            for (ServerPlayer p : EntityArgument.getPlayers(c, "alvos")) IdolManager.stop(p);
                                            c.getSource().sendSuccess(() -> Component.literal("§7✦ Ídolo removido."), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("rate")
                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 600))
                                        .executes(c -> {
                                            int m = IntegerArgumentType.getInteger(c, "minutos");
                                            IdolManager.setIntervalMinutes(m);
                                            c.getSource().sendSuccess(() -> Component.literal(
                                                    "§5✦ O Ídolo aparece a cada §f" + m + " §7min de jogo."), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("on").executes(c -> {
                            IdolManager.setEnabled(true);
                            c.getSource().sendSuccess(() -> Component.literal("§a✦ Ídolo ATIVADO."), false);
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(c -> {
                            IdolManager.setEnabled(false);
                            c.getSource().sendSuccess(() -> Component.literal("§c✦ Ídolo DESATIVADO."), false);
                            return 1;
                        }))
                        .then(Commands.literal("mode")
                                .then(Commands.literal("psico").executes(c -> {
                                    IdolManager.setAggressive(false);
                                    c.getSource().sendSuccess(() -> Component.literal(
                                            "§5✦ Ídolo: modo §dPsicológico §7(só assombra, não mata)."), false);
                                    return 1;
                                }))
                                .then(Commands.literal("agressivo").executes(c -> {
                                    IdolManager.setAggressive(true);
                                    c.getSource().sendSuccess(() -> Component.literal(
                                            "§c✦ Ídolo: modo §4Agressivo §7(mata no contato)!"), false);
                                    return 1;
                                })))
                        .then(Commands.literal("status").executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("§5✦ Ídolo: "
                                    + (IdolManager.isEnabled() ? "§aON" : "§cOFF")
                                    + " §7| spawn §f" + IdolManager.getIntervalMinutes() + "min"
                                    + " §7| modo " + (IdolManager.isAggressive() ? "§4Agressivo" : "§dPsicológico")), false);
                            return 1;
                        }))));
    }
}
