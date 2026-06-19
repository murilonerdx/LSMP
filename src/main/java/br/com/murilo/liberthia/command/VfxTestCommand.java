package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.magic.spell.vfx.Pack2VfxCatalog;
import br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * r166: <b>/liberthia vfx</b> — testing command for the new sprite-based VFX
 * framework (Pixel FX Designer pack).
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>{@code /liberthia vfx list} — list all 20 effect types</li>
 *   <li>{@code /liberthia vfx spawn <type>} — spawn effect at look-target</li>
 *   <li>{@code /liberthia vfx spawn <type> <scale>} — with custom scale</li>
 *   <li>{@code /liberthia vfx self <type>} — spawn on self (rides player)</li>
 * </ul>
 *
 * <p>Type names are the lowercase enum names: {@code fire}, {@code vortex},
 * {@code protectioncircle}, {@code felspell}, etc. Tab-completion lists them.
 */
public final class VfxTestCommand {

    private VfxTestCommand() {}

    private static final SuggestionProvider<CommandSourceStack> VFX_NAMES =
            (ctx, builder) -> {
                String remaining = builder.getRemaining().toLowerCase();
                for (SpriteVfxRegistry.Type t : SpriteVfxRegistry.Type.values()) {
                    String name = t.name().toLowerCase();
                    if (name.startsWith(remaining)) builder.suggest(name);
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> PACK2_NAMES =
            (ctx, builder) -> {
                String remaining = builder.getRemaining().toLowerCase();
                for (Pack2VfxCatalog.Type t : Pack2VfxCatalog.Type.values()) {
                    String name = t.name().toLowerCase();
                    if (name.startsWith(remaining)) builder.suggest(name);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("vfx")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("list").executes(VfxTestCommand::doList))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(VFX_NAMES)
                                        .executes(c -> doSpawn(c, 1.0F))
                                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.1F, 8F))
                                                .executes(c -> doSpawn(c,
                                                        FloatArgumentType.getFloat(c, "scale"))))))
                        .then(Commands.literal("self")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(VFX_NAMES)
                                        .executes(VfxTestCommand::doSelf)))
                        // r167: Pack 2 subcommands
                        .then(Commands.literal("pack2")
                                .then(Commands.literal("list").executes(VfxTestCommand::doPack2List))
                                .then(Commands.literal("spawn")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(PACK2_NAMES)
                                                .executes(c -> doPack2Spawn(c, 0, 1.0F))
                                                .then(Commands.argument("color", IntegerArgumentType.integer(0, 11))
                                                        .executes(c -> doPack2Spawn(c,
                                                                IntegerArgumentType.getInteger(c, "color"), 1.0F))
                                                        .then(Commands.argument("scale", FloatArgumentType.floatArg(0.1F, 8F))
                                                                .executes(c -> doPack2Spawn(c,
                                                                        IntegerArgumentType.getInteger(c, "color"),
                                                                        FloatArgumentType.getFloat(c, "scale")))))))
                                .then(Commands.literal("self")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(PACK2_NAMES)
                                                .executes(c -> doPack2Self(c, 0))
                                                .then(Commands.argument("color", IntegerArgumentType.integer(0, 11))
                                                        .executes(c -> doPack2Self(c,
                                                                IntegerArgumentType.getInteger(c, "color")))))))));
    }

    // ── list ────────────────────────────────────────────────────────────────

    private static int doList(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSystemMessage(Component.literal(
                "§l§dSprite VFX — " + SpriteVfxRegistry.Type.values().length + " effects loaded:"));
        for (SpriteVfxRegistry.Type t : SpriteVfxRegistry.Type.values()) {
            SpriteVfxRegistry.Effect e = t.effect;
            src.sendSystemMessage(Component.literal(
                    "  §7• §f" + t.name().toLowerCase()
                            + " §8[" + e.frameCount + " frames, " + e.lifetimeTicks + "t, "
                            + e.orientation.name().toLowerCase()
                            + (e.loop ? ", loop" : "") + "]"));
        }
        return SpriteVfxRegistry.Type.values().length;
    }

    // ── spawn ───────────────────────────────────────────────────────────────

    private static int doSpawn(CommandContext<CommandSourceStack> ctx, float scale) {
        var src = ctx.getSource();
        String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
        SpriteVfxRegistry.Type type;
        try { type = SpriteVfxRegistry.Type.valueOf(typeName); }
        catch (Throwable t) {
            src.sendFailure(Component.literal("§cTipo VFX desconhecido: " + typeName.toLowerCase()));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer sp)) {
            src.sendFailure(Component.literal("§cPrecisa ser executado por um player"));
            return 0;
        }
        // Pick at look-target up to 24 blocks
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        var hit = sp.pick(24, 0, false);
        Vec3 spawnPos = hit.getLocation();
        if (spawnPos.distanceTo(eye) < 1.5) {
            // Looking at face — push forward
            spawnPos = eye.add(look.scale(4.0));
        }
        ServerLevel sl = (ServerLevel) sp.level();
        SpriteVfxRegistry.spawn(sl, spawnPos, sp, type, scale);
        src.sendSystemMessage(Component.literal(
                "§a✦ Spawnou §f" + typeName.toLowerCase()
                        + "§a (scale " + scale + ") em §7"
                        + String.format("%.1f, %.1f, %.1f", spawnPos.x, spawnPos.y, spawnPos.z)));
        return 1;
    }

    // ── self (ride caster) ──────────────────────────────────────────────────

    private static int doSelf(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
        SpriteVfxRegistry.Type type;
        try { type = SpriteVfxRegistry.Type.valueOf(typeName); }
        catch (Throwable t) {
            src.sendFailure(Component.literal("§cTipo VFX desconhecido"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer sp)) {
            src.sendFailure(Component.literal("§cPrecisa ser executado por um player"));
            return 0;
        }
        ServerLevel sl = (ServerLevel) sp.level();
        var ent = new br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxEntity(
                sl, sp, sp.position(), type, 1.0F);
        ent.startRiding(sp, true);
        sl.addFreshEntity(ent);
        src.sendSystemMessage(Component.literal(
                "§a✦ §f" + typeName.toLowerCase()
                        + " §aativo em você por §e"
                        + (type.effect.lifetimeTicks / 20F) + "s"));
        return 1;
    }

    // ════════════════════════════════════════════════════════════════════════
    // r167: Pack 2 subcommands
    // ════════════════════════════════════════════════════════════════════════

    private static int doPack2List(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSystemMessage(Component.literal(
                "§l§dPack-2 VFX — " + Pack2VfxCatalog.Type.values().length
                        + " effects × " + Pack2VfxCatalog.COLORS + " colors:"));
        for (Pack2VfxCatalog.Type t : Pack2VfxCatalog.Type.values()) {
            src.sendSystemMessage(Component.literal(
                    "  §7• §f" + t.name().toLowerCase()
                            + " §8[" + t.frameCount + " frames, " + t.lifetimeTicks + "t"
                            + (t.loop ? ", loop" : "") + "]"));
        }
        src.sendSystemMessage(Component.literal("§7Uso: §f/liberthia vfx pack2 spawn <type> [color 0-11] [scale]"));
        return Pack2VfxCatalog.Type.values().length;
    }

    private static int doPack2Spawn(CommandContext<CommandSourceStack> ctx, int colorIdx, float scale) {
        var src = ctx.getSource();
        String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
        Pack2VfxCatalog.Type type;
        try { type = Pack2VfxCatalog.Type.valueOf(typeName); }
        catch (Throwable t) {
            src.sendFailure(Component.literal("§cTipo VFX desconhecido: " + typeName.toLowerCase()));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer sp)) {
            src.sendFailure(Component.literal("§cPrecisa ser executado por um player"));
            return 0;
        }
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        var hit = sp.pick(24, 0, false);
        Vec3 spawnPos = hit.getLocation();
        if (spawnPos.distanceTo(eye) < 1.5) spawnPos = eye.add(look.scale(4.0));
        ServerLevel sl = (ServerLevel) sp.level();
        Pack2VfxCatalog.spawn(sl, spawnPos, sp, type, colorIdx, scale);
        src.sendSystemMessage(Component.literal(
                "§a✦ Pack-2 §f" + typeName.toLowerCase()
                        + " §a(color " + colorIdx + ", scale " + scale + ")"));
        return 1;
    }

    private static int doPack2Self(CommandContext<CommandSourceStack> ctx, int colorIdx) {
        var src = ctx.getSource();
        String typeName = StringArgumentType.getString(ctx, "type").toUpperCase();
        Pack2VfxCatalog.Type type;
        try { type = Pack2VfxCatalog.Type.valueOf(typeName); }
        catch (Throwable t) {
            src.sendFailure(Component.literal("§cTipo VFX desconhecido"));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer sp)) {
            src.sendFailure(Component.literal("§cPrecisa ser executado por um player"));
            return 0;
        }
        ServerLevel sl = (ServerLevel) sp.level();
        var ent = new br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxEntity(
                sl, sp, sp.position(), type, colorIdx, 1.0F);
        ent.startRiding(sp, true);
        sl.addFreshEntity(ent);
        src.sendSystemMessage(Component.literal(
                "§a✦ Pack-2 §f" + typeName.toLowerCase()
                        + " §a(color " + colorIdx + ") ativo em você §e"
                        + (type.lifetimeTicks / 20F) + "s"));
        return 1;
    }
}
