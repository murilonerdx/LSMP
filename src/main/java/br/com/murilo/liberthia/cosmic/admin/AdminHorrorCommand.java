package br.com.murilo.liberthia.cosmic.admin;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicHorrorManager;
import br.com.murilo.liberthia.cosmic.CosmicHorrorPhase;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r40: <b>Admin Horror Tools</b> — comandos pra forçar hallucinations,
 * setar insanity/corruption/etc., resetar.
 *
 * <h2>Comandos</h2>
 * <pre>
 * /liberthia horror inject &lt;player&gt; &lt;type&gt; [intensity] [duration]
 *   Injeta uma hallucination específica num player.
 *
 * /liberthia horror sanity set &lt;player&gt; &lt;stat&gt; &lt;value&gt;
 *   stat: insanity|corruption|paranoia|obsession|cosmic|knowledge
 *   value: 0-100
 *
 * /liberthia horror sanity get &lt;player&gt;
 *   Mostra snapshot completo das stats.
 *
 * /liberthia horror sanity add &lt;player&gt; &lt;stat&gt; &lt;delta&gt;
 *   Adiciona delta (pode ser negativo).
 *
 * /liberthia horror sanity reset &lt;player&gt;
 *   Zera todas as stats psicológicas.
 *
 * /liberthia horror phase &lt;player&gt; &lt;phase&gt;
 *   phase: dormant|subtle|corruption|rupture|manifestation
 *   Força uma phase específica do cosmic horror.
 *
 * /liberthia horror trigger &lt;player&gt;
 *   Inicia o auto-escalation (igual Forbidden Tome).
 *
 * /liberthia horror reset &lt;player&gt;
 *   Para o cosmic horror ativo.
 *
 * /liberthia horror chat &lt;player&gt; &lt;message&gt;
 *   Manda mensagem fake só pro player (FAKE_CHAT_MESSAGE).
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AdminHorrorCommand {

    private AdminHorrorCommand() {}

    private static final String[] STAT_NAMES = {
            "insanity", "corruption", "paranoia", "obsession", "cosmic", "knowledge"
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STATS =
            (ctx, builder) -> {
                for (String s : STAT_NAMES) builder.suggest(s);
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TYPES =
            (ctx, builder) -> {
                for (HallucinationType t : HallucinationType.values()) {
                    builder.suggest(t.name().toLowerCase());
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PHASES =
            (ctx, builder) -> {
                builder.suggest("dormant");
                builder.suggest("subtle");
                builder.suggest("corruption");
                builder.suggest("rupture");
                builder.suggest("manifestation");
                return builder.buildFuture();
            };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("liberthia")
                .then(Commands.literal("horror")
                        // /liberthia horror inject <player> <type> [intensity] [duration]
                        .then(Commands.literal("inject")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(SUGGEST_TYPES)
                                                .executes(ctx -> inject(ctx, 1.0F, 60))
                                                .then(Commands.argument("intensity", FloatArgumentType.floatArg(0F, 1F))
                                                        .executes(ctx -> inject(ctx,
                                                                FloatArgumentType.getFloat(ctx, "intensity"), 60))
                                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1200))
                                                                .executes(ctx -> inject(ctx,
                                                                        FloatArgumentType.getFloat(ctx, "intensity"),
                                                                        IntegerArgumentType.getInteger(ctx, "duration"))))))))

                        // /liberthia horror sanity ...
                        .then(Commands.literal("sanity")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("stat", StringArgumentType.word())
                                                        .suggests(SUGGEST_STATS)
                                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                                .executes(ctx -> sanitySet(ctx))))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("stat", StringArgumentType.word())
                                                        .suggests(SUGGEST_STATS)
                                                        .then(Commands.argument("delta", IntegerArgumentType.integer(-100, 100))
                                                                .executes(ctx -> sanityAdd(ctx))))))
                                .then(Commands.literal("get")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> sanityGet(ctx))))
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> sanityReset(ctx)))))

                        // /liberthia horror phase <player> <phase>
                        .then(Commands.literal("phase")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("phase", StringArgumentType.word())
                                                .suggests(SUGGEST_PHASES)
                                                .executes(ctx -> setPhase(ctx)))))

                        // /liberthia horror trigger <player>
                        .then(Commands.literal("trigger")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> trigger(ctx))))

                        // /liberthia horror reset <player>
                        .then(Commands.literal("reset")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> resetHorror(ctx))))

                        // /liberthia horror chat <player> <message>
                        .then(Commands.literal("chat")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(ctx -> fakeChat(ctx)))))));
    }

    // ────────── Implementations ──────────

    private static int inject(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                               float intensity, int duration) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String typeStr = StringArgumentType.getString(ctx, "type").toUpperCase();
        HallucinationType type;
        try { type = HallucinationType.valueOf(typeStr); }
        catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§cTipo inválido: " + typeStr));
            return 0;
        }
        HallucinationManager.force(target, type, intensity, duration, "");
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Injetado §d" + type.name() + "§a em §e" + target.getName().getString()), true);
        return 1;
    }

    private static int sanitySet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String stat = StringArgumentType.getString(ctx, "stat").toLowerCase();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        applyStat(target, stat, value, false);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ " + stat + " = " + value + " em " + target.getName().getString()), true);
        return 1;
    }

    private static int sanityAdd(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String stat = StringArgumentType.getString(ctx, "stat").toLowerCase();
        int delta = IntegerArgumentType.getInteger(ctx, "delta");
        applyStat(target, stat, delta, true);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ " + stat + " += " + delta + " em " + target.getName().getString()), true);
        return 1;
    }

    private static int sanityGet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§7[" + target.getName().getString() + "] §f" + InsanityData.snapshot(target)), false);
        return 1;
    }

    private static int sanityReset(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        InsanityData.resetAll(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Sanity stats resetadas em " + target.getName().getString()), true);
        return 1;
    }

    private static int setPhase(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String phaseStr = StringArgumentType.getString(ctx, "phase").toLowerCase();
        CosmicHorrorPhase phase = switch (phaseStr) {
            case "dormant" -> CosmicHorrorPhase.DORMANT;
            case "subtle" -> CosmicHorrorPhase.SUBTLE_PRESENCE;
            case "corruption" -> CosmicHorrorPhase.DIMENSIONAL_CORRUPTION;
            case "rupture" -> CosmicHorrorPhase.REALITY_RUPTURE;
            case "manifestation" -> CosmicHorrorPhase.ENTITY_MANIFESTATION;
            default -> null;
        };
        if (phase == null) {
            ctx.getSource().sendFailure(Component.literal("§cPhase inválida"));
            return 0;
        }
        CosmicHorrorManager.setPhase(target, phase);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Phase " + phase + " setada em " + target.getName().getString()), true);
        return 1;
    }

    private static int trigger(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        CosmicHorrorManager.trigger(target, "admin_command");
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Cosmic horror iniciado em " + target.getName().getString()), true);
        return 1;
    }

    private static int resetHorror(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        CosmicHorrorManager.reset(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Horror resetado em " + target.getName().getString()), true);
        return 1;
    }

    private static int fakeChat(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String msg = StringArgumentType.getString(ctx, "message");
        HallucinationManager.force(target, HallucinationType.FAKE_CHAT_MESSAGE, 1F, 1, msg);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Mensagem fake enviada pra " + target.getName().getString()), true);
        return 1;
    }

    // ────────── helpers ──────────

    private static void applyStat(ServerPlayer p, String stat, int v, boolean add) {
        switch (stat) {
            case "insanity"   -> { if (add) InsanityData.addInsanity(p, v); else InsanityData.setInsanity(p, v); }
            case "corruption" -> { if (add) InsanityData.addCorruption(p, v); else InsanityData.setCorruption(p, v); }
            case "paranoia"   -> { if (add) InsanityData.addParanoia(p, v); else InsanityData.setParanoia(p, v); }
            case "obsession"  -> { if (add) InsanityData.addObsession(p, v); else InsanityData.setObsession(p, v); }
            case "cosmic"     -> { if (add) InsanityData.addCosmicInfluence(p, v); else InsanityData.setCosmicInfluence(p, v); }
            case "knowledge"  -> { if (add) InsanityData.addForbiddenKnowledge(p, v); else InsanityData.setForbiddenKnowledge(p, v); }
            default -> {}
        }
    }
}
