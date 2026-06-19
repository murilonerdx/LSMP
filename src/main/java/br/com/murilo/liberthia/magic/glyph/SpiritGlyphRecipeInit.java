package br.com.murilo.liberthia.magic.glyph;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.ItemStack;

import static br.com.murilo.liberthia.magic.glyph.SpiritGlyphRecipes.ing;

/**
 * v0.1.148 r116: <b>SpiritGlyphRecipeInit</b> — registra todas as receitas
 * de Spirit Reagents → Spell Scrolls.
 *
 * <p>Padrão: 2-4 reagents combinados → 1 spell scroll. Spells mais poderosos
 * (EPIC) precisam de mais variedade/quantidade de reagents.
 *
 * <p>Original code. Recipe registry chamado uma vez via {@link #registerAll}
 * no setup do mod.
 */
public final class SpiritGlyphRecipeInit {

    private static boolean initialized = false;

    private SpiritGlyphRecipeInit() {}

    public static void registerAll() {
        if (initialized) return;
        initialized = true;

        // ──────── FIRE SCHOOL ────────
        // Fireball: 2x Wisp + 1x Astral
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Bola de Fogo",
                () -> new ItemStack(ModItems.SPELL_FIREBALL.get()),
                ing(ModItems.WISP_ESSENCE, 2),
                ing(ModItems.ASTRAL_DUST, 1)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Inferno Cônico",
                () -> new ItemStack(ModItems.SPELL_INFERNO.get()),
                ing(ModItems.WISP_ESSENCE, 4),
                ing(ModItems.ASTRAL_DUST, 2),
                ing(ModItems.PHANTOM_INK, 1)));

        // ──────── ICE SCHOOL ────────
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Projétil de Gelo",
                () -> new ItemStack(ModItems.SPELL_FROSTBOLT.get()),
                ing(ModItems.MEMORY_SHARD, 2),
                ing(ModItems.WHISPERWOOD_RESIN, 1)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Nova Glacial",
                () -> new ItemStack(ModItems.SPELL_FROST_NOVA.get()),
                ing(ModItems.MEMORY_SHARD, 3),
                ing(ModItems.ASTRAL_DUST, 2),
                ing(ModItems.ECTOPLASM_STRAND, 1)));

        // ──────── LIGHTNING SCHOOL ────────
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Raio Mágico",
                () -> new ItemStack(ModItems.SPELL_LIGHTNING_BOLT.get()),
                ing(ModItems.ASTRAL_DUST, 3),
                ing(ModItems.ECTOPLASM_STRAND, 1)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Raio em Cadeia",
                () -> new ItemStack(ModItems.SPELL_CHAIN_LIGHTNING.get()),
                ing(ModItems.ASTRAL_DUST, 4),
                ing(ModItems.MEMORY_SHARD, 2),
                ing(ModItems.VEIL_FRAGMENT, 1)));

        // ──────── BLOOD SCHOOL ────────
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Lança de Sangue",
                () -> new ItemStack(ModItems.SPELL_BLOOD_SPEAR.get()),
                ing(ModItems.PHANTOM_INK, 2),
                ing(ModItems.WHISPERWOOD_RESIN, 1)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Drenagem Vital",
                () -> new ItemStack(ModItems.SPELL_LIFEDRAIN.get()),
                ing(ModItems.PHANTOM_INK, 3),
                ing(ModItems.ECTOPLASM_STRAND, 2)));

        // ──────── ELDRITCH SCHOOL ────────
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Explosão Eldritch",
                () -> new ItemStack(ModItems.SPELL_ELDRITCH_BLAST.get()),
                ing(ModItems.ECTOPLASM_STRAND, 3),
                ing(ModItems.MEMORY_SHARD, 1),
                ing(ModItems.VEIL_FRAGMENT, 1)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Tentáculo do Vazio",
                () -> new ItemStack(ModItems.SPELL_VOID_TENTACLE.get()),
                ing(ModItems.ECTOPLASM_STRAND, 2),
                ing(ModItems.PHANTOM_INK, 2),
                ing(ModItems.VEIL_FRAGMENT, 1)));

        // ──────── HOLY SCHOOL ────────
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Cura Maior",
                () -> new ItemStack(ModItems.SPELL_GREATER_HEAL.get()),
                ing(ModItems.WISP_ESSENCE, 5),
                ing(ModItems.WHISPERWOOD_RESIN, 2)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Castigo Divino",
                () -> new ItemStack(ModItems.SPELL_SMITE.get()),
                ing(ModItems.WISP_ESSENCE, 3),
                ing(ModItems.ASTRAL_DUST, 2)));

        // ──────── NATURE SCHOOL ────────
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Lasca de Pedra",
                () -> new ItemStack(ModItems.SPELL_STONE_SHARD.get()),
                ing(ModItems.WHISPERWOOD_RESIN, 2),
                ing(ModItems.WISP_ESSENCE, 1)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Emaranhado de Cipós",
                () -> new ItemStack(ModItems.SPELL_VINE_TANGLE.get()),
                ing(ModItems.WHISPERWOOD_RESIN, 3),
                ing(ModItems.MEMORY_SHARD, 1)));

        // ──────── EVOCATION ────────
        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Míssil Mágico",
                () -> new ItemStack(ModItems.SPELL_MAGIC_MISSILE.get()),
                ing(ModItems.ASTRAL_DUST, 2)));

        SpiritGlyphRecipes.register(new SpiritGlyphRecipes.Recipe(
                "Escudo Arcano",
                () -> new ItemStack(ModItems.SPELL_MAGIC_SHIELD.get()),
                ing(ModItems.WISP_ESSENCE, 3),
                ing(ModItems.WHISPERWOOD_RESIN, 1),
                ing(ModItems.MEMORY_SHARD, 1)));
    }
}
