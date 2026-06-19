package br.com.murilo.liberthia.observation;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r60: <b>/liberthia observe test</b> — comando admin pra provar o
 * pipeline do Observation Casting.
 *
 * <p>Constrói um Spell exemplar (DirectGaze + Tendril + Amplify) e o resolve.
 *
 * <h2>Uso</h2>
 * <pre>
 * /liberthia observe test
 *   → casta "Direct Gaze + Tendril + Amplify" no que o player olha.
 *
 * /liberthia observe info
 *   → mostra quantas parts estão registradas.
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ObservationCommand {

    private ObservationCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("liberthia")
                .then(Commands.literal("observe")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("test")
                                .executes(ObservationCommand::executeTest))
                        .then(Commands.literal("info")
                                .executes(ObservationCommand::executeInfo))
                        .then(Commands.literal("cycle")
                                .executes(ObservationCommand::executeCycle))
                        .then(Commands.literal("presets")
                                .executes(ObservationCommand::executeListPresets))
                        .then(Commands.literal("preset")
                                .then(Commands.argument("index",
                                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 15))
                                        .executes(ObservationCommand::executeSelectPreset)))
                        .then(Commands.literal("source")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("amount",
                                                com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 1000))
                                                .executes(ObservationCommand::executeSourceAdd))))
                        .then(Commands.literal("level")
                                .executes(ObservationCommand::executeLevelInfo)
                                .then(Commands.literal("xp")
                                        .then(Commands.argument("amount",
                                                com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100000))
                                                .executes(ObservationCommand::executeAddXp))))
                        .then(Commands.literal("hud")
                                .then(Commands.argument("position",
                                        com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .executes(ObservationCommand::executeSetHudPos)))
                        .then(Commands.literal("manual")
                                .executes(ObservationCommand::executeGiveManual))));
    }

    /** r74: Dá o Patchouli manual pro player via item NBT. */
    private static int executeGiveManual(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var sp = ctx.getSource().getPlayerOrException();
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation("patchouli", "guide_book"));
            if (item == null) {
                ctx.getSource().sendFailure(Component.literal(
                    "§c⚠ Mod Patchouli não está instalado/carregado."));
                return 0;
            }
            var stack = new net.minecraft.world.item.ItemStack(item);
            stack.getOrCreateTag().putString("patchouli:book", "liberthia:liberthia_manual");
            if (!sp.getInventory().add(stack)) {
                sp.drop(stack, false);
            }
            sp.displayClientMessage(Component.literal(
                "§5§l✦ §rManual de Liberthia entregue."), false);
            return 1;
        } catch (Throwable t) {
            ctx.getSource().sendFailure(Component.literal("§cFalha: " + t.getMessage()));
            return 0;
        }
    }

    /** r71: Move o HUD do feitiço. Posições: bottom_left, bottom_right, top_left, top_right, center_top. */
    private static int executeSetHudPos(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            String pos = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "position");
            // Update on client via packet — simpler: set via static (dev mode singleplayer)
            // Real fix: send a S2C packet to update client static var.
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(
                ctx.getSource().getPlayerOrException(),
                new br.com.murilo.liberthia.observation.network.HudPositionS2CPacket(pos.toUpperCase()));
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§5§l✦ HUD position: §e" + pos.toUpperCase()), false);
            return 1;
        } catch (Throwable t) {
            ctx.getSource().sendFailure(Component.literal("§cPosições: bottom_left, bottom_right, top_left, top_right, center_top"));
            return 0;
        }
    }

    /** r71: Mostra info de level/xp/luck atual. */
    private static int executeLevelInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var sp = ctx.getSource().getPlayerOrException();
            int level = br.com.murilo.liberthia.observation.source.MagicLevelData.getLevel(sp);
            int xp = br.com.murilo.liberthia.observation.source.MagicLevelData.getXp(sp);
            int threshold = br.com.murilo.liberthia.observation.source.MagicLevelData.getXpThreshold(level);
            float luck = br.com.murilo.liberthia.observation.source.MagicLevelData.getLuck(sp);
            int sourceBonus = br.com.murilo.liberthia.observation.source.MagicLevelData.getSourceBonus(sp);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§5§l✦ Magic Level: §e" + level
                + " §7| XP: §e" + xp + "§7/§e" + threshold
                + " §7| Luck: §e" + luck + "%"
                + " §7| Source Bonus: §e+" + sourceBonus), false);
            return 1;
        } catch (Throwable t) { return 0; }
    }

    /** r71: Adiciona XP. */
    private static int executeAddXp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var sp = ctx.getSource().getPlayerOrException();
            int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
            br.com.murilo.liberthia.observation.source.MagicLevelEvents.awardXp(sp, amount, "command");
            return 1;
        } catch (Throwable t) { return 0; }
    }

    /** r70: lista todos os 16 presets nomeados. */
    private static int executeListPresets(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
            "§5§l✦ 16 Presets do Grimório §r(use §e/liberthia observe preset <0-15>§r):"), false);
        for (int i = 0; i < br.com.murilo.liberthia.observation.item.ObservationTomeItem.PRESET_COUNT; i++) {
            var spell = br.com.murilo.liberthia.observation.item.ObservationTomeItem.buildPreset(i);
            int idx = i;
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§7  " + idx + ". §e" + spell.name() + " §7(§c"
                + spell.totalSourceCost() + " Source§7, §c" + spell.totalSanityCost() + " Sanity§7, "
                + spell.recipe().size() + " glyphs)"), false);
        }
        return 1;
    }

    /** r70: seleciona preset por índice direto. */
    private static int executeSelectPreset(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var sp = ctx.getSource().getPlayerOrException();
            var stack = sp.getMainHandItem();
            if (!(stack.getItem() instanceof br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem)) {
                ctx.getSource().sendFailure(Component.literal("§cSegure o Grimório."));
                return 0;
            }
            int idx = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "index");
            stack.getOrCreateTag().putInt(br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem.NBT_PRESET, idx);
            stack.getOrCreateTag().remove(br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem.NBT_CUSTOM_RECIPE);
            var spell = br.com.murilo.liberthia.observation.item.ObservationTomeItem.buildPreset(idx);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ §rPreset selecionado: §e" + spell.name() + " §7(" + spell.totalSourceCost() + " Source)"), false);
            return 1;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int executeCycle(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var sp = ctx.getSource().getPlayerOrException();
            var stack = sp.getMainHandItem();
            if (stack.getItem() instanceof br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem) {
                br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem.cyclePreset(stack, sp);
                return 1;
            }
            ctx.getSource().sendFailure(Component.literal("§cVocê precisa segurar um Grimório."));
            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int executeSourceAdd(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var sp = ctx.getSource().getPlayerOrException();
            int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
            br.com.murilo.liberthia.observation.source.SourceData.add(sp, amount);
            int cur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
            int max = br.com.murilo.liberthia.observation.source.SourceData.getMax(sp);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ +§e" + amount + " Source §a→ §e" + cur + "§7/§e" + max), false);
            return 1;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static int executeTest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        try {
            var sp = ctx.getSource().getPlayerOrException();
            // Garante que parts inicializados
            ObservationParts.init();

            // Constrói spell exemplar
            ObservationSpell spell = ObservationSpell.builder("Tendril Amplificado", 0x9d4dd6)
                    .add(ObservationParts.DIRECT_GAZE)
                    .add(ObservationParts.TENDRIL)
                    .add(ObservationParts.AMPLIFY)
                    .build();

            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§5§lObservation Cast §rExecutando: §e" + spell.name()
                            + " §7(custo: §c" + spell.totalSanityCost() + "§7 sanity)"), false);

            boolean ok = ObservationResolver.cast(sp, spell);
            if (ok) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "§a✓ Spell completou."), false);
                return 1;
            } else {
                ctx.getSource().sendFailure(Component.literal("§c✗ Spell falhou."));
                return 0;
            }
        } catch (Throwable t) {
            ctx.getSource().sendFailure(Component.literal("§cErro: " + t.getMessage()));
            return 0;
        }
    }

    private static int executeInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        ObservationParts.init();
        int count = br.com.murilo.liberthia.observation.api.ObservationRegistry.all().size();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§5§lObservation Registry§r: §e" + count + " parts§r registrados."), false);
        for (var entry : br.com.murilo.liberthia.observation.api.ObservationRegistry.all().entrySet()) {
            var part = entry.getValue();
            String type = switch (part.typeIndex()) {
                case 1 -> "§bWatchMethod";
                case 5 -> "§dManifestation";
                case 10 -> "§eDistortion";
                default -> "§7Unknown";
            };
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "  " + type + " §7| §f" + entry.getKey() + " §8(sanity " + part.sanityCost() + ")"), false);
        }
        return 1;
    }
}
