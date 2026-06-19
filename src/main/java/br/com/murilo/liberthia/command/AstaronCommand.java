package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.S2CAstaronSyncPacket;
import br.com.murilo.liberthia.world.AstaronData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * r187 — /liberthia astaron — controle admin da radiação dimensional, alvo de caos e cor do céu.
 */
public final class AstaronCommand {
    private AstaronCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("liberthia")
                .then(Commands.literal("astaron")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("radiation")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                .executes(c -> setRadiation(c, IntegerArgumentType.getInteger(c, "value")))))
                                .then(Commands.literal("get").executes(AstaronCommand::getRadiation)))
                        .then(Commands.literal("chaos")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("target", IntegerArgumentType.integer(0, 200))
                                                .executes(c -> setChaos(c, IntegerArgumentType.getInteger(c, "target"))))))
                        .then(Commands.literal("skycolor")
                                .then(Commands.argument("rgb", IntegerArgumentType.integer(0, 0xFFFFFF))
                                        .executes(c -> setSkyColor(c, IntegerArgumentType.getInteger(c, "rgb")))))
                        .then(Commands.literal("reset").executes(AstaronCommand::reset))
                        .then(Commands.literal("status").executes(AstaronCommand::status))));
    }

    private static int setRadiation(CommandContext<CommandSourceStack> c, int v) {
        MinecraftServer s = c.getSource().getServer();
        AstaronData d = AstaronData.get(s); d.setRadiation(v); broadcast(s, d);
        c.getSource().sendSuccess(() -> Component.literal("§a✓ Radiação Dimensional: §e" + v + "/100"), true);
        return 1;
    }

    private static int setChaos(CommandContext<CommandSourceStack> c, int v) {
        MinecraftServer s = c.getSource().getServer();
        AstaronData d = AstaronData.get(s); d.setChaosTarget(v); broadcast(s, d);
        c.getSource().sendSuccess(() -> Component.literal("§a✓ Alvo de caos (cósmicos): §e" + v), true);
        return 1;
    }

    private static int setSkyColor(CommandContext<CommandSourceStack> c, int rgb) {
        MinecraftServer s = c.getSource().getServer();
        AstaronData d = AstaronData.get(s); d.setSkyColor(rgb); broadcast(s, d);
        c.getSource().sendSuccess(() -> Component.literal(String.format("§a✓ Cor do céu: §f#%06X", rgb)), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        AstaronData d = AstaronData.get(s); d.reset(); broadcast(s, d);
        c.getSource().sendSuccess(() -> Component.literal("§a✓ Astaron resetado (radiação 0, sem caos)."), true);
        return 1;
    }

    private static int getRadiation(CommandContext<CommandSourceStack> c) {
        AstaronData d = AstaronData.get(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.literal("§7Radiação Dimensional: §e" + d.getRadiation() + "/100"), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        MinecraftServer s = c.getSource().getServer();
        AstaronData d = AstaronData.get(s);
        int count = d.countCosmicEntities(s);
        c.getSource().sendSuccess(() -> Component.literal(
                "§5✦ Astaron — §7Radiação §f" + d.getRadiation() + "/100§7, Caos-alvo §f" + d.getChaosTarget()
                        + "§7, Cósmicos vivos §d" + count + "§7, Céu §f" + String.format("#%06X", d.getSkyColor())), false);
        return 1;
    }

    private static void broadcast(MinecraftServer s, AstaronData d) {
        ModNetwork.sendToAll(s, new S2CAstaronSyncPacket(d.getRadiation(), d.countCosmicEntities(s), d.getSkyColor()));
    }
}
