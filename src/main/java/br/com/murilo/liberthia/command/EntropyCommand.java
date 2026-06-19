package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.logic.entropy.EntropyTracker;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * r183 — recuperação dos motores de entropia. Faz MERGE no root {@code /liberthia} existente.
 * <ul>
 *   <li>{@code /liberthia entropy list} — lista os ids mapeados.</li>
 *   <li>{@code /liberthia entropy revert <id>} — reverte um motor (lê o JSON da pasta do mundo).</li>
 *   <li>{@code /liberthia entropy revert all} — reverte TODOS (se o servidor caiu e bugou).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class EntropyCommand {
    private EntropyCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("entropy")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("list").executes(EntropyCommand::list))
                        .then(Commands.literal("revert")
                                .executes(ctx -> revert(ctx, "all"))
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(ctx -> revert(ctx, StringArgumentType.getString(ctx, "id")))))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        Set<String> ids = EntropyTracker.listIds(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("§5☣ Entropia: §f" + ids.size()
                + " §7motor(es) mapeado(s): §8" + (ids.isEmpty() ? "—" : String.join(", ", ids))), false);
        return ids.size();
    }

    private static int revert(CommandContext<CommandSourceStack> ctx, String id) {
        var server = ctx.getSource().getServer();
        if (id.equalsIgnoreCase("all")) {
            int n = EntropyTracker.revertAll(server);
            ctx.getSource().sendSuccess(() -> Component.literal("§a✔ Revertidos §f" + n + " §amotor(es) de entropia — mundo restaurado."), true);
            return n;
        }
        int c = Math.max(1, EntropyTracker.count(server, id));
        int n = EntropyTracker.revert(server, id);
        ctx.getSource().sendSuccess(() -> Component.literal("§a✔ Motor §f" + id + " §arevertido (§f" + Math.max(c, n) + "§a blocos)."), true);
        return Math.max(n, 1);
    }
}
