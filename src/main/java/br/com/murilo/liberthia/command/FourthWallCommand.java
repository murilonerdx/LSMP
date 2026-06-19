package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.fourthwall.FourthWallData;
import br.com.murilo.liberthia.fourthwall.FourthWallFeature;
import br.com.murilo.liberthia.fourthwall.FourthWallManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /liberthia fourthwall ...} — controla o pacote de quarta parede.
 * OP (permission 2), silencioso. Default: MASTER OFF.
 *
 * <ul>
 *   <li>{@code on|off} — liga/desliga geral</li>
 *   <li>{@code feature <id> on|off} — liga/desliga um efeito</li>
 *   <li>{@code intensity low|med|high} — frequência dos sustos</li>
 *   <li>{@code test <id>} — dispara um efeito em você agora (preview)</li>
 *   <li>{@code status} — lista o estado</li>
 * </ul>
 * Efeitos: {@code crosshair relogio f3 crash tab morte placa}.
 */
public final class FourthWallCommand {

    private FourthWallCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("fourthwall")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("on").executes(c -> setMaster(c.getSource(), true)))
                        .then(Commands.literal("off").executes(c -> setMaster(c.getSource(), false)))
                        .then(Commands.literal("status").executes(c -> status(c.getSource())))
                        .then(Commands.literal("intensity")
                                .then(Commands.literal("low").executes(c -> setIntensity(c.getSource(), 0)))
                                .then(Commands.literal("med").executes(c -> setIntensity(c.getSource(), 1)))
                                .then(Commands.literal("high").executes(c -> setIntensity(c.getSource(), 2))))
                        .then(Commands.literal("feature")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.literal("on").executes(c ->
                                                setFeature(c.getSource(), StringArgumentType.getString(c, "id"), true)))
                                        .then(Commands.literal("off").executes(c ->
                                                setFeature(c.getSource(), StringArgumentType.getString(c, "id"), false)))))
                        .then(Commands.literal("test")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(c -> test(c.getSource(), StringArgumentType.getString(c, "id")))))));
    }

    private static int setMaster(CommandSourceStack src, boolean on) {
        FourthWallData.get(src.getLevel()).setMaster(on);
        src.sendSuccess(() -> Component.literal("§5Liberthia: §fQuarta-parede §" + (on ? "a LIGADA" : "c DESLIGADA")), false);
        return 1;
    }

    private static int setIntensity(CommandSourceStack src, int i) {
        FourthWallData.get(src.getLevel()).setIntensity(i);
        String name = i == 0 ? "baixa" : i == 2 ? "alta" : "média";
        src.sendSuccess(() -> Component.literal("§5Liberthia: §fIntensidade → §b" + name), false);
        return 1;
    }

    private static int setFeature(CommandSourceStack src, String id, boolean on) {
        FourthWallFeature f = FourthWallFeature.byId(id);
        if (f == null) {
            src.sendFailure(Component.literal("§cEfeito inválido: §f" + id
                    + "§c. Use: crosshair relogio f3 crash tab morte placa"));
            return 0;
        }
        FourthWallData.get(src.getLevel()).setFeature(f, on);
        src.sendSuccess(() -> Component.literal("§5Liberthia: §f" + f.id + " → " + (on ? "§aon" : "§coff")), false);
        return 1;
    }

    private static int status(CommandSourceStack src) {
        FourthWallData d = FourthWallData.get(src.getLevel());
        src.sendSuccess(() -> Component.literal("§5§lQuarta-parede: "
                + (d.isMaster() ? "§aLIGADA" : "§cDESLIGADA")
                + " §7(intensidade " + (d.getIntensity() == 0 ? "baixa" : d.getIntensity() == 2 ? "alta" : "média") + ")"), false);
        StringBuilder sb = new StringBuilder("§7");
        for (FourthWallFeature f : FourthWallFeature.values()) {
            sb.append(d.rawOn(f) ? "§a" : "§c").append(f.id).append(" §7");
        }
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int test(CommandSourceStack src, String id) {
        FourthWallFeature f = FourthWallFeature.byId(id);
        if (f == null) {
            src.sendFailure(Component.literal("§cEfeito inválido: §f" + id));
            return 0;
        }
        ServerPlayer sp = src.getPlayer();
        if (sp == null) {
            src.sendFailure(Component.literal("§cPrecisa ser um player (o efeito é visual)."));
            return 0;
        }
        FourthWallManager.test(src.getServer(), sp, f);
        src.sendSuccess(() -> Component.literal("§5Liberthia: §7testando §f" + f.id + "§7 em você…"), false);
        return 1;
    }
}
