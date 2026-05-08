package br.com.murilo.liberthia.matter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Comandos {@code /liberthia matter ...} pra debug e testes.
 *
 * <ul>
 *   <li>{@code /liberthia matter set <player> dark|white|yellow <value>}</li>
 *   <li>{@code /liberthia matter add <player> dark|white|yellow <value>}</li>
 *   <li>{@code /liberthia matter get <player>}</li>
 *   <li>{@code /liberthia matter clear <player>}</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = br.com.murilo.liberthia.LiberthiaMod.MODID)
public final class MatterCommand {

    private MatterCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("liberthia")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("matter")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("dark")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                        .executes(c -> set(c, "dark"))))
                                        .then(Commands.literal("white")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                        .executes(c -> set(c, "white"))))
                                        .then(Commands.literal("yellow")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                        .executes(c -> set(c, "yellow"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("dark")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                                        .executes(c -> add(c, "dark"))))
                                        .then(Commands.literal("white")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                                        .executes(c -> add(c, "white"))))
                                        .then(Commands.literal("yellow")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                                        .executes(c -> add(c, "yellow"))))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(MatterCommand::get)))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(MatterCommand::clear))));
        dispatcher.register(root);
    }

    private static int set(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String type)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        float v = FloatArgumentType.getFloat(ctx, "value");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            switch (type) {
                case "dark"   -> profile.setDark(v);
                case "white"  -> profile.setWhite(v);
                case "yellow" -> profile.setYellow(v);
            }
            MatterProfileEvents.syncTo(p);
        });
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Matéria " + type + " de " + p.getName().getString() + " = " + v)
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int add(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String type)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        float v = FloatArgumentType.getFloat(ctx, "value");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            switch (type) {
                case "dark"   -> profile.addDark(v);
                case "white"  -> profile.addWhite(v);
                case "yellow" -> profile.addYellow(v);
            }
            MatterProfileEvents.syncTo(p);
        });
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Matéria " + type + " " + (v >= 0 ? "+" : "") + v + " em " + p.getName().getString())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int get(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "%s — DM:%.1f WM:%.1f YM:%.1f → %s",
                    p.getName().getString(), profile.getDark(), profile.getWhite(),
                    profile.getYellow(), profile.getActiveType().name()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        });
        return 1;
    }

    private static int clear(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            profile.setDark(0); profile.setWhite(0); profile.setYellow(0);
            MatterProfileEvents.syncTo(p);
        });
        ctx.getSource().sendSuccess(() -> Component.literal("Perfil zerado")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }
}
