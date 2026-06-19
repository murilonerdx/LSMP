package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.magic.factory.DynamicSpellItem;
import br.com.murilo.liberthia.magic.factory.SpellRecipe;
import br.com.murilo.liberthia.magic.factory.SpellRecipeRegistry;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * r148: <b>/liberthia spell</b> — comandos da Spell Factory.
 *
 * <h2>Subcomandos</h2>
 * <ul>
 *   <li>{@code /liberthia spell list} — lista todas as recipes carregadas</li>
 *   <li>{@code /liberthia spell give <id>} — dá um pergaminho da factory pra você</li>
 *   <li>{@code /liberthia spell info <id>} — mostra detalhes de uma recipe</li>
 *   <li>{@code /liberthia spell reload} — força re-load das recipes (datapack reload)</li>
 * </ul>
 *
 * <p>Auto-completion: o argumento {@code <id>} oferece sugestões com todas
 * as recipes carregadas via {@link SpellRecipeRegistry#ids()}.
 */
public final class SpellFactoryCommand {

    private SpellFactoryCommand() {}

    /**
     * r168: SuggestionProvider que oferece IDs de spells de AMBOS os registries
     * (SpellLibrary código + SpellRecipeRegistry JSON-data).
     */
    private static final SuggestionProvider<CommandSourceStack> SPELL_IDS =
            (ctx, builder) -> {
                String prefix = builder.getRemaining().toLowerCase();
                // SpellLibrary (in-code: ~150 spells incluindo os 50 r168)
                for (String id : SpellLibrary.ALL.keySet()) {
                    if (id.toLowerCase().startsWith(prefix)) builder.suggest(id);
                }
                // SpellRecipeRegistry (JSON-loaded recipes)
                for (String id : SpellRecipeRegistry.ids()) {
                    if (id.toLowerCase().startsWith(prefix)) builder.suggest(id);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("spell")
                        .requires(src -> src.hasPermission(2))
                        // list
                        .then(Commands.literal("list").executes(SpellFactoryCommand::doList))
                        // reload
                        .then(Commands.literal("reload").executes(SpellFactoryCommand::doReload))
                        // give <id>
                        .then(Commands.literal("give")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .suggests(SPELL_IDS)
                                        .executes(SpellFactoryCommand::doGive)))
                        // info <id>
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .suggests(SPELL_IDS)
                                        .executes(SpellFactoryCommand::doInfo)))));
    }

    private static int doList(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        int libCount = SpellLibrary.ALL.size();
        int recipeCount = SpellRecipeRegistry.count();
        if (libCount == 0 && recipeCount == 0) {
            src.sendSystemMessage(Component.literal("§7Nenhuma spell carregada"));
            return 0;
        }
        // ── SpellLibrary (in-code, includes r168 set) ─────────────────────
        src.sendSystemMessage(Component.literal("§l§dSpellLibrary §7— " + libCount + " spells in-code:"));
        for (SpellDef d : SpellLibrary.all()) {
            src.sendSystemMessage(Component.literal(
                    "  §7• §f" + d.id + " §8(§7" + d.school + " §8/ §7" + d.rarity + "§8) §7→ §f" + d.name));
        }
        // ── SpellRecipeRegistry (JSON-data) ────────────────────────────────
        if (recipeCount > 0) {
            src.sendSystemMessage(Component.literal(
                    "§l§dSpellRecipe (JSON) §7— " + recipeCount + " recipes:"));
            for (SpellRecipe r : SpellRecipeRegistry.all()) {
                src.sendSystemMessage(Component.literal(
                        "  §7• §f" + r.id + " §8(§7" + r.school + " §8/ §7" + r.type + "§8)"));
            }
        }
        return libCount + recipeCount;
    }

    private static int doReload(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSystemMessage(Component.literal("§eRe-load triggers via §6/reload§e. Use esse pra forçar."));
        // O reload real acontece via /reload de datapacks vanilla — nosso loader é AddReloadListener
        src.getServer().getResourceManager(); // no-op pra forçar lazy
        return 1;
    }

    private static int doGive(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        // r168: check both registries (SpellLibrary takes precedence)
        SpellDef libDef = SpellLibrary.get(id);
        SpellRecipe recipe = libDef == null ? SpellRecipeRegistry.get(id) : null;
        if (libDef == null && recipe == null) {
            src.sendFailure(Component.literal("§cSpell desconhecida: " + id));
            return 0;
        }
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.literal("§cPrecisa ser um player"));
            return 0;
        }
        ItemStack stack = DynamicSpellItem.stackFor(ModItems.FACTORY_SPELL_SCROLL.get(), id);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        String displayName = libDef != null ? libDef.name : recipe.name;
        src.sendSystemMessage(Component.literal("§a✓ §f" + displayName + " §7entregue"));
        return 1;
    }

    private static int doInfo(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "id");
        // r168: try SpellLibrary first
        SpellDef libDef = SpellLibrary.get(id);
        if (libDef != null) {
            src.sendSystemMessage(Component.literal("§l§d═══ " + libDef.name + " §8(" + libDef.id + ") §d═══"));
            src.sendSystemMessage(Component.literal("§7Escola: §f" + libDef.school
                    + " §7• Raridade: §f" + libDef.rarity));
            src.sendSystemMessage(Component.literal("§7Mana: §b" + libDef.manaCost
                    + " §7• CD: §c" + (libDef.cooldownTicks / 20) + "s"
                    + " §7• Dano: §c" + libDef.damage + " §7• Range: §a" + libDef.range + "b"));
            if (!libDef.lore.isEmpty()) {
                src.sendSystemMessage(Component.literal("§o§8" + libDef.lore));
            }
            return 1;
        }
        SpellRecipe r = SpellRecipeRegistry.get(id);
        if (r == null) {
            src.sendFailure(Component.literal("§cSpell desconhecida: " + id));
            return 0;
        }
        src.sendSystemMessage(Component.literal("§l§d═══ " + r.name + " §8(" + r.id + ") §d═══"));
        src.sendSystemMessage(Component.literal("§7Escola: §f" + r.school + " §7• Tipo: §f" + r.type
                + " §7• Raridade: §f" + r.rarity));
        src.sendSystemMessage(Component.literal("§7Mana: §b" + r.mana + " §7• CD: §c" + (r.cooldown / 20) + "s"
                + " §7• Dano: §c" + r.damage + " §7• Range: §a" + r.range + "b"));
        if (!r.lore.isEmpty()) {
            src.sendSystemMessage(Component.literal("§o§8" + r.lore));
        }
        if (!r.effects.isEmpty()) {
            src.sendSystemMessage(Component.literal("§7Efeitos:"));
            for (var eff : r.effects) {
                src.sendSystemMessage(Component.literal("  §7• §f" + eff.type
                        + " §8(" + eff.duration + "t / amp " + eff.amplifier + ")"));
            }
        }
        return 1;
    }
}
