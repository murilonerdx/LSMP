package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.registry.ModCapabilities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("immune")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    
                                    player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                                        data.setImmune(enabled);
                                        if (enabled) {
                                            data.setInfection(0);
                                            data.setPermanentHealthPenalty(0);
                                            InfectionLogic.applyDerivedEffects(player, data);
                                        }
                                        InfectionLogic.sync(player, data);
                                    });

                                    context.getSource().sendSuccess(() -> Component.literal("§dLiberthia: §fImunidade definida para: " + (enabled ? "§aLigado" : "§cDesligado")), true);
                                    return 1;
                                }))
                )
        );
    }
}
