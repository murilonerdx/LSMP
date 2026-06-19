package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.fog.FogZone;
import br.com.murilo.liberthia.fog.FogZoneData;
import br.com.murilo.liberthia.fog.PersonalFogManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * {@code /liberthia fog ...} — controla zonas de neblina (estilo terror) por
 * coordenada. Só OP (permission 2), silencioso (só quem digita vê).
 *
 * <ul>
 *   <li>{@code fog add <x y z> <raio> <cor> [densidade] [opacidade] [intensidade] [distancia]}</li>
 *   <li>{@code fog here <raio> <cor> [densidade] [opacidade] [intensidade] [distancia]}</li>
 *   <li>{@code fog set <id> <densidade|opacidade|transparencia|intensidade|distancia|raio> <valor>}</li>
 *   <li>{@code fog set <id> cor <cor>} — troca a cor</li>
 *   <li>{@code fog remove <id>} — remove por id</li>
 *   <li>{@code fog clear} — apaga todas</li>
 *   <li>{@code fog list} — lista as zonas com todos os parâmetros</li>
 * </ul>
 *
 * <p>Cores: nomes ({@code preto vermelho verde roxo azul branco amarelo laranja cinza})
 * ou hex ({@code #RRGGBB} / {@code RRGGBB}).
 * <br>Densidade 0..2 (qtd partículas) · Opacidade 0..1 (0=some, 1=densa) ·
 * Intensidade 0.5..6 (tamanho dos puffs) · Distância 8..120 (alcance de render).
 */
public final class FogCommand {

    private FogCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("fog")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .then(Commands.argument("raio", FloatArgumentType.floatArg(1f, 256f))
                                                .then(corChain(ctx -> Vec3Argument.getVec3(ctx, "pos"))))))
                        .then(Commands.literal("here")
                                .then(Commands.argument("raio", FloatArgumentType.floatArg(1f, 256f))
                                        .then(corChain(ctx -> ctx.getSource().getPosition()))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .then(setNumeric("densidade", 0f, 2f))
                                        .then(setNumeric("opacidade", 0f, 1f))
                                        .then(setNumeric("transparencia", 0f, 1f))
                                        .then(setNumeric("intensidade", 0.5f, 6f))
                                        .then(setNumeric("distancia", 8f, 120f))
                                        .then(setNumeric("raio", 1f, 256f))
                                        .then(Commands.literal("cor")
                                                .then(Commands.argument("cor", StringArgumentType.word())
                                                        .executes(ctx -> doSetColor(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "cor")))))))
                        .then(Commands.literal("player")
                                .then(Commands.argument("alvo", EntityArgument.player())
                                        .then(Commands.literal("off").executes(FogCommand::doPlayerFogOff))
                                        .then(Commands.argument("densidade", FloatArgumentType.floatArg(0.05f, 2f))
                                                .then(Commands.argument("cor", StringArgumentType.word())
                                                        .then(Commands.argument("segundos", IntegerArgumentType.integer(1, 86400))
                                                                .executes(FogCommand::doPlayerFog))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(ctx -> doRemove(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "id")))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> doClear(ctx.getSource())))
                        .then(Commands.literal("list")
                                .executes(ctx -> doList(ctx.getSource())))));
    }

    /**
     * Monta o nó {@code <cor>} com a cadeia de parâmetros opcionais
     * (densidade → opacidade → intensidade → distancia), cada nível executável.
     * {@code posFn} resolve o centro da zona (coordenada do "add" ou posição do "here").
     */
    private static RequiredArgumentBuilder<CommandSourceStack, String> corChain(
            Function<CommandContext<CommandSourceStack>, Vec3> posFn) {
        return Commands.argument("cor", StringArgumentType.word())
                .executes(ctx -> doAdd(ctx, posFn,
                        FogZone.DEFAULT_DENSITY, FogZone.DEFAULT_OPACITY,
                        FogZone.DEFAULT_THICKNESS, (float) FogZone.DEFAULT_VIEW))
                .then(Commands.argument("densidade", FloatArgumentType.floatArg(0f, 2f))
                        .executes(ctx -> doAdd(ctx, posFn,
                                f(ctx, "densidade"), FogZone.DEFAULT_OPACITY,
                                FogZone.DEFAULT_THICKNESS, (float) FogZone.DEFAULT_VIEW))
                        .then(Commands.argument("opacidade", FloatArgumentType.floatArg(0f, 1f))
                                .executes(ctx -> doAdd(ctx, posFn,
                                        f(ctx, "densidade"), f(ctx, "opacidade"),
                                        FogZone.DEFAULT_THICKNESS, (float) FogZone.DEFAULT_VIEW))
                                .then(Commands.argument("intensidade", FloatArgumentType.floatArg(0.5f, 6f))
                                        .executes(ctx -> doAdd(ctx, posFn,
                                                f(ctx, "densidade"), f(ctx, "opacidade"),
                                                f(ctx, "intensidade"), (float) FogZone.DEFAULT_VIEW))
                                        .then(Commands.argument("distancia", FloatArgumentType.floatArg(8f, 120f))
                                                .executes(ctx -> doAdd(ctx, posFn,
                                                        f(ctx, "densidade"), f(ctx, "opacidade"),
                                                        f(ctx, "intensidade"), f(ctx, "distancia")))))));
    }

    /** Nó {@code <prop> <valor>} pro subcomando set. */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> setNumeric(
            String prop, float min, float max) {
        return Commands.literal(prop)
                .then(Commands.argument("valor", FloatArgumentType.floatArg(min, max))
                        .executes(ctx -> doSet(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "id"),
                                prop, FloatArgumentType.getFloat(ctx, "valor"))));
    }

    private static float f(CommandContext<CommandSourceStack> ctx, String name) {
        return FloatArgumentType.getFloat(ctx, name);
    }

    private static int doAdd(CommandContext<CommandSourceStack> ctx,
                             Function<CommandContext<CommandSourceStack>, Vec3> posFn,
                             float densidade, float opacidade, float intensidade, float distancia) {
        CommandSourceStack src = ctx.getSource();
        Vec3 pos = posFn.apply(ctx);
        float raio = FloatArgumentType.getFloat(ctx, "raio");
        String corStr = StringArgumentType.getString(ctx, "cor");

        int color = parseColor(corStr);
        if (color < 0) {
            src.sendFailure(Component.literal("§cCor inválida: §f" + corStr
                    + "§c. Use nome (preto/vermelho/verde/roxo/azul/branco/amarelo/laranja/cinza) ou hex #RRGGBB."));
            return 0;
        }
        ServerLevel level = src.getLevel();
        String dim = level.dimension().location().toString();
        int id = FogZoneData.get(level).add(src.getServer(), dim,
                pos.x, pos.y, pos.z, raio, color, densidade, opacidade, intensidade, distancia, false);
        src.sendSuccess(() -> Component.literal(String.format(
                "§5Liberthia: §fNeblina §a#%d§f criada em §e%.0f %.0f %.0f§f "
                        + "(raio §b%.0f§f, dens §b%.2f§f, opac §b%.2f§f, int §b%.1f§f, dist §b%.0f§f, cor §6%s§f).",
                id, pos.x, pos.y, pos.z, raio, densidade, opacidade, intensidade, distancia, corStr)), false);
        return 1;
    }

    private static int doSet(CommandSourceStack src, int id, String prop, float valor) {
        FogZone z = FogZoneData.get(src.getLevel()).editById(src.getServer(), id, prop, valor);
        if (z == null) {
            src.sendFailure(Component.literal("§cNenhuma neblina com id §f" + id
                    + "§c (ou propriedade inválida)."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(String.format(
                "§5Liberthia: §fNeblina §a#%d§f §7%s§f → §b%.2f §8| §7dens §b%.2f §7opac §b%.2f §7int §b%.1f §7dist §b%.0f §7raio §b%.0f",
                z.id, prop, valor, z.density, z.opacity, z.thickness, z.viewDistance, z.radius)), false);
        return 1;
    }

    private static int doSetColor(CommandSourceStack src, int id, String corStr) {
        int color = parseColor(corStr);
        if (color < 0) {
            src.sendFailure(Component.literal("§cCor inválida: §f" + corStr
                    + "§c. Use nome ou hex #RRGGBB."));
            return 0;
        }
        FogZone z = FogZoneData.get(src.getLevel()).editColorById(src.getServer(), id, color);
        if (z == null) {
            src.sendFailure(Component.literal("§cNenhuma neblina com id §f" + id));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(String.format(
                "§5Liberthia: §fNeblina §a#%d§f cor → §6%s §8(#%06X)", id, corStr, color)), false);
        return 1;
    }

    private static int doRemove(CommandSourceStack src, int id) {
        boolean ok = FogZoneData.get(src.getLevel()).removeById(src.getServer(), id);
        if (ok) {
            src.sendSuccess(() -> Component.literal("§5Liberthia: §fNeblina §a#" + id + "§f removida."), false);
            return 1;
        }
        src.sendFailure(Component.literal("§cNenhuma neblina com id §f" + id));
        return 0;
    }

    private static int doClear(CommandSourceStack src) {
        int n = FogZoneData.get(src.getLevel()).clear(src.getServer());
        src.sendSuccess(() -> Component.literal("§5Liberthia: §f" + n + " neblina(s) apagada(s)."), false);
        return 1;
    }

    private static int doList(CommandSourceStack src) {
        List<FogZone> zones = FogZoneData.get(src.getLevel()).getZones();
        if (zones.isEmpty()) {
            src.sendSuccess(() -> Component.literal("§7Nenhuma neblina ativa."), false);
            return 1;
        }
        src.sendSuccess(() -> Component.literal("§5§lNeblinas ativas (" + zones.size() + "):"), false);
        for (FogZone z : zones) {
            src.sendSuccess(() -> Component.literal(String.format(
                    "§7#%d §f%s §7@ §e%.0f %.0f %.0f §7r=§b%.0f §7dens=§b%.2f §7opac=§b%.2f §7int=§b%.1f §7dist=§b%.0f §7cor=§6#%06X %s",
                    z.id, shortDim(z.dim), z.x, z.y, z.z, z.radius, z.density, z.opacity,
                    z.thickness, z.viewDistance, z.color, z.fromItem ? "§8(item)" : "")), false);
        }
        return 1;
    }

    private static String shortDim(String dim) {
        int i = dim.indexOf(':');
        return i >= 0 ? dim.substring(i + 1) : dim;
    }

    /** Névoa pessoal: segue o player-alvo (só ele vê) por N segundos. */
    private static int doPlayerFog(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        float dens = FloatArgumentType.getFloat(ctx, "densidade");
        String corStr = StringArgumentType.getString(ctx, "cor");
        int secs = IntegerArgumentType.getInteger(ctx, "segundos");
        int color = parseColor(corStr);
        if (color < 0) {
            ctx.getSource().sendFailure(Component.literal("§cCor inválida: §f" + corStr
                    + "§c. Use nome (preto/vermelho/verde/roxo/azul/branco/...) ou hex #RRGGBB."));
            return 0;
        }
        PersonalFogManager.apply(target, color, dens, secs);
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "§5Liberthia: §fNévoa pessoal em §a%s§f por §b%ds§f (dens §b%.2f§f, cor §6%s§f). Só ele vê e ela o segue.",
                target.getGameProfile().getName(), secs, dens, corStr)), false);
        return 1;
    }

    private static int doPlayerFogOff(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "alvo");
        PersonalFogManager.clear(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§5Liberthia: §fNévoa pessoal removida de §a" + target.getGameProfile().getName()), false);
        return 1;
    }

    /** Nome de cor ou hex → 0xRRGGBB. Retorna -1 se inválido. */
    public static int parseColor(String s) {
        if (s == null) return -1;
        String c = s.trim().toLowerCase();
        switch (c) {
            case "preto": case "black": case "void": case "vazio": return 0x05050A;
            case "vermelho": case "red": case "sangue": case "blood": return 0x6E0A0A;
            case "verde": case "green": case "toxic": case "toxico": return 0x0A3A12;
            case "roxo": case "purple": case "magenta": return 0x2A0A3A;
            case "azul": case "blue": case "gelo": return 0x0A1A3A;
            case "branco": case "white": case "fog": return 0xCFCFD6;
            case "amarelo": case "yellow": return 0x3A3208;
            case "laranja": case "orange": return 0x3A1C05;
            case "cinza": case "gray": case "grey": return 0x202024;
            default: break;
        }
        // hex: #RRGGBB, 0xRRGGBB ou RRGGBB
        String hex = c.startsWith("#") ? c.substring(1)
                : c.startsWith("0x") ? c.substring(2) : c;
        if (hex.length() == 6 && hex.matches("[0-9a-f]{6}")) {
            try {
                return Integer.parseInt(hex, 16);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }
}
