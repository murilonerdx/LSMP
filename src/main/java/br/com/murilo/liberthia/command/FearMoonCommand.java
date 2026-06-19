package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.event.FearMoonEvents;
import br.com.murilo.liberthia.world.FearMoonData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * {@code /liberthia fearmoon <start|stop|status|forcenext>} — controla/testa a
 * Lua do Medo ({@link FearMoonEvents}). OP nível 2.
 */
public final class FearMoonCommand {

    private FearMoonCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("liberthia").then(Commands.literal("fearmoon")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("start").executes(ctx -> {
                    CommandSourceStack s = ctx.getSource();
                    FearMoonEvents.startFearMoon(s.getServer(), s.getServer().overworld());
                    s.sendSuccess(() -> Component.literal("§4§lLua do Medo INICIADA."), true);
                    return 1;
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    CommandSourceStack s = ctx.getSource();
                    FearMoonEvents.endFearMoon(s.getServer(), s.getServer().overworld());
                    s.sendSuccess(() -> Component.literal("§7Lua do Medo encerrada."), true);
                    return 1;
                }))
                .then(Commands.literal("forcenext").executes(ctx -> {
                    CommandSourceStack s = ctx.getSource();
                    FearMoonData.get(s.getServer()).setForceNext(true);
                    s.sendSuccess(() -> Component.literal("§5A próxima noite será uma Lua do Medo."), true);
                    return 1;
                }))
                .then(Commands.literal("color")
                        .then(Commands.argument("r", IntegerArgumentType.integer(0, 255))
                                .then(Commands.argument("g", IntegerArgumentType.integer(0, 255))
                                        .then(Commands.argument("b", IntegerArgumentType.integer(0, 255))
                                                .executes(ctx -> {
                                                    CommandSourceStack s = ctx.getSource();
                                                    int r = IntegerArgumentType.getInteger(ctx, "r");
                                                    int g = IntegerArgumentType.getInteger(ctx, "g");
                                                    int b = IntegerArgumentType.getInteger(ctx, "b");
                                                    int color = (r << 16) | (g << 8) | b;
                                                    FearMoonData.get(s.getServer()).setColor(color);
                                                    FearMoonEvents.syncAll(s.getServer());
                                                    s.sendSuccess(() -> Component.literal(String.format(
                                                            "§5Cor da Lua do Medo definida: §fR%d G%d B%d", r, g, b)), true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("status").executes(ctx -> {
                    CommandSourceStack s = ctx.getSource();
                    FearMoonData data = FearMoonData.get(s.getServer());
                    boolean a = data.isActive();
                    boolean f = data.isForceNext();
                    String hex = String.format("#%06X", data.getColor());
                    s.sendSuccess(() -> Component.literal(
                            "§5Lua do Medo: " + (a ? "§4§lATIVA" : "§7inativa")
                                    + " §8| próxima forçada: " + (f ? "§asim" : "§7não")
                                    + " §8| cor: §f" + hex), false);
                    return 1;
                }))
        ));
    }
}
