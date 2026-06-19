package br.com.murilo.liberthia.cosmic.emf;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

/**
 * r174: <b>/liberthia irregularity</b> — marca/desmarca players como
 * "irregularidade dimensional" (sem precisar do item). Os EMF Meters próximos
 * reagem ao marcado conforme ele se aproxima.
 *
 * <pre>
 * /liberthia irregularity            → alterna você mesmo
 * /liberthia irregularity on  &lt;alvos&gt;
 * /liberthia irregularity off &lt;alvos&gt;
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class IrregularityCommand {

    private IrregularityCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("irregularity")
                        .requires(s -> s.hasPermission(2))
                        .executes(IrregularityCommand::toggleSelf)
                        .then(Commands.literal("on")
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> set(c, true))))
                        .then(Commands.literal("off")
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> set(c, false))))));
    }

    private static int toggleSelf(CommandContext<CommandSourceStack> c) {
        ServerPlayer sp;
        try { sp = c.getSource().getPlayerOrException(); }
        catch (Exception e) { return 0; }
        boolean now = !EmfSource.hasFlag(sp);
        EmfSource.setFlag(sp, now);
        final boolean f = now;
        c.getSource().sendSuccess(() -> Component.literal(
                "§5✦ Você " + (f ? "§cÉ" : "§7não é mais") + " §5uma irregularidade dimensional."), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> c, boolean on) {
        Collection<ServerPlayer> targets;
        try { targets = EntityArgument.getPlayers(c, "alvos"); }
        catch (Exception e) { return 0; }
        for (ServerPlayer p : targets) EmfSource.setFlag(p, on);
        final int n = targets.size();
        c.getSource().sendSuccess(() -> Component.literal(
                "§5✦ " + n + " player(s) " + (on ? "§cmarcado(s)" : "§7desmarcado(s)")
                        + " §5como irregularidade."), false);
        return n;
    }
}
