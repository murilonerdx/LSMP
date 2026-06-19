package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r55: comando pra ajustar posição da HUD de sanidade.
 *
 * <pre>
 * /liberthia hud sanity show
 *   → mostra posição atual e config
 *
 * /liberthia hud sanity anchor &lt;0|1|2|3&gt;
 *   → seta anchor (0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right)
 *
 * /liberthia hud sanity move &lt;x&gt; &lt;y&gt;
 *   → seta offset X e Y relativo ao anchor
 *
 * /liberthia hud sanity toggle
 *   → liga/desliga a HUD
 *
 * /liberthia hud sanity reset
 *   → volta pro default (anchor=3, x=92, y=50)
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SanityHudCommand {

    private SanityHudCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("liberthia")
                .then(Commands.literal("hud")
                        .then(Commands.literal("sanity")
                                .then(Commands.literal("show").executes(SanityHudCommand::showInfo))
                                .then(Commands.literal("anchor")
                                        .then(Commands.argument("anchor", IntegerArgumentType.integer(0, 3))
                                                .executes(SanityHudCommand::setAnchor)))
                                .then(Commands.literal("move")
                                        .then(Commands.argument("x", IntegerArgumentType.integer(0, 4000))
                                                .then(Commands.argument("y", IntegerArgumentType.integer(0, 4000))
                                                        .executes(SanityHudCommand::setPosition))))
                                .then(Commands.literal("toggle").executes(SanityHudCommand::toggle))
                                .then(Commands.literal("reset").executes(SanityHudCommand::reset)))));
    }

    private static int showInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var cfg = br.com.murilo.liberthia.config.LiberthiaConfig.CLIENT;
        int anchor = cfg.sanityAnchor.get();
        int x = cfg.sanityX.get();
        int y = cfg.sanityY.get();
        boolean visible = cfg.sanityHudVisible.get();
        String anchorName = switch (anchor) {
            case 0 -> "top-left";
            case 1 -> "top-right";
            case 2 -> "bottom-left";
            default -> "bottom-right";
        };
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§5§lSanidade HUD§r: anchor=§e" + anchorName
                        + "§r, x=§e" + x + "§r, y=§e" + y
                        + "§r, visible=§e" + visible), false);
        return 1;
    }

    private static int setAnchor(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        int a = IntegerArgumentType.getInteger(ctx, "anchor");
        br.com.murilo.liberthia.config.LiberthiaConfig.CLIENT.sanityAnchor.set(a);
        br.com.murilo.liberthia.config.LiberthiaConfig.CLIENT.sanityAnchor.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Sanity anchor = " + a), false);
        return 1;
    }

    private static int setPosition(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int y = IntegerArgumentType.getInteger(ctx, "y");
        var cfg = br.com.murilo.liberthia.config.LiberthiaConfig.CLIENT;
        cfg.sanityX.set(x);
        cfg.sanityY.set(y);
        cfg.sanityX.save();
        cfg.sanityY.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Sanity HUD posicionada em x=" + x + " y=" + y), false);
        return 1;
    }

    private static int toggle(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var cfg = br.com.murilo.liberthia.config.LiberthiaConfig.CLIENT;
        boolean newVal = !cfg.sanityHudVisible.get();
        cfg.sanityHudVisible.set(newVal);
        cfg.sanityHudVisible.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Sanity HUD = " + (newVal ? "VISÍVEL" : "OCULTA")), false);
        return 1;
    }

    private static int reset(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var cfg = br.com.murilo.liberthia.config.LiberthiaConfig.CLIENT;
        cfg.sanityAnchor.set(3);
        cfg.sanityX.set(92);
        cfg.sanityY.set(50);
        cfg.sanityHudVisible.set(true);
        cfg.sanityAnchor.save();
        cfg.sanityX.save();
        cfg.sanityY.save();
        cfg.sanityHudVisible.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Sanity HUD resetada pro default"), false);
        return 1;
    }
}
