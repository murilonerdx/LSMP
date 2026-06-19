package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.block.entity.SpreaderEngineBlockEntity;
import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.logic.entropy.EntropyTracker;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
 * r186 — controle do Motor de Matéria Escura. MERGE no root {@code /liberthia}.
 * <ul>
 *   <li>{@code /liberthia engine} — lista motores ativos.</li>
 *   <li>{@code /liberthia engine global enable|disable} — kill-switch global (config).</li>
 *   <li>{@code /liberthia engine speed|radius|crystalChance <v>} — ajusta a config global.</li>
 *   <li>{@code /liberthia engine <id> enable|disable|clear} — controla um motor específico.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class EngineCommand {
    private EngineCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("engine")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> list(ctx.getSource()))
                        .then(Commands.literal("global")
                                .then(Commands.literal("enable").executes(ctx -> setGlobal(ctx.getSource(), true)))
                                .then(Commands.literal("disable").executes(ctx -> setGlobal(ctx.getSource(), false))))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                                        .executes(EngineCommand::setSpeed)))
                        .then(Commands.literal("radius")
                                .then(Commands.argument("value", IntegerArgumentType.integer(8, 2048))
                                        .executes(EngineCommand::setRadius)))
                        .then(Commands.literal("crystalChance")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                                        .executes(EngineCommand::setCrystalChance)))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.literal("enable").executes(ctx -> setEnable(ctx, true)))
                                .then(Commands.literal("disable").executes(ctx -> setEnable(ctx, false)))
                                .then(Commands.literal("clear").executes(EngineCommand::clearEngine)))));
    }

    private static int list(CommandSourceStack src) {
        Set<String> ids = SpreaderEngineBlockEntity.activeIds();
        src.sendSuccess(() -> Component.literal("§5☣ Motores ativos: §f" + ids.size()
                + " §8" + (ids.isEmpty() ? "—" : String.join(", ", ids))), false);
        return ids.size();
    }

    private static int setGlobal(CommandSourceStack src, boolean enabled) {
        LiberthiaConfig.SERVER.blackMatterEngineEnabled.set(enabled);
        LiberthiaConfig.SERVER.blackMatterEngineEnabled.save();
        src.sendSuccess(() -> Component.literal("§5Motor global: " + (enabled ? "§aLIGADO" : "§cDESLIGADO")), true);
        return 1;
    }

    private static int setEnable(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        String id = StringArgumentType.getString(ctx, "id");
        SpreaderEngineBlockEntity be = SpreaderEngineBlockEntity.getActiveById(id);
        if (be == null) { ctx.getSource().sendFailure(Component.literal("§cMotor '" + id + "' não encontrado/descarregado.")); return 0; }
        be.setEnabled(enabled);
        ctx.getSource().sendSuccess(() -> Component.literal("§5Motor §f" + id + (enabled ? " §aativado" : " §cdesativado")), true);
        return 1;
    }

    private static int clearEngine(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        int n = EntropyTracker.revert(ctx.getSource().getServer(), id);
        ctx.getSource().sendSuccess(() -> Component.literal("§a✔ Motor §f" + id + " §alimpo (§f" + n + "§a blocos revertidos)."), true);
        return Math.max(n, 1);
    }

    private static int setSpeed(CommandContext<CommandSourceStack> ctx) {
        int v = IntegerArgumentType.getInteger(ctx, "value");
        LiberthiaConfig.SERVER.blackMatterEngineSpeedMultiplier.set(v);
        LiberthiaConfig.SERVER.blackMatterEngineSpeedMultiplier.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§5Velocidade: §f" + v + "x"), true);
        return v;
    }

    private static int setRadius(CommandContext<CommandSourceStack> ctx) {
        int v = IntegerArgumentType.getInteger(ctx, "value");
        LiberthiaConfig.SERVER.blackMatterEngineMaxRadius.set(v);
        LiberthiaConfig.SERVER.blackMatterEngineMaxRadius.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§5Raio máximo: §f" + v), true);
        return v;
    }

    private static int setCrystalChance(CommandContext<CommandSourceStack> ctx) {
        int v = IntegerArgumentType.getInteger(ctx, "value");
        LiberthiaConfig.SERVER.blackMatterCrystalChancePercent.set(v);
        LiberthiaConfig.SERVER.blackMatterCrystalChancePercent.save();
        ctx.getSource().sendSuccess(() -> Component.literal("§5Chance de cristal: §f" + v + "%"), true);
        return v;
    }
}
