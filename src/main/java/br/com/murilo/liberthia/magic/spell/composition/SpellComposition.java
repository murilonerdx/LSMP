package br.com.murilo.liberthia.magic.spell.composition;

import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * v0.1.151 r119: <b>SpellComposition</b> — multi-página spell composto de
 * 1 base + N modifiers.
 *
 * <p>Encoded em NBT no ItemStack do spell scroll:
 * <pre>
 *   liberthia.composition: {
 *     base: "fireball",
 *     mods: ["amplify", "amplify", "aoe", "ignite"]   ← lista (com repetição)
 *   }
 * </pre>
 *
 * <h2>Cálculo de stats</h2>
 * Aplica multiplicadores de cada modifier no order:
 * <ul>
 *   <li>Mana final = mana base × Π(modMana[i])</li>
 *   <li>Dano final = dano base × Π(modDamage[i])</li>
 *   <li>Range final = range base × Π(modRange[i])</li>
 *   <li>Cooldown final = cd base × Π(modCooldown[i])</li>
 * </ul>
 *
 * <p>Stacking de modifiers: cada repetição multiplica novamente. Ex:
 * AMPLIFY (×1.5 dano) × 3 stacks = ×3.375 dano final.
 */
public final class SpellComposition {

    public static final String NBT_COMPOSITION = "liberthia.composition";
    public static final String NBT_BASE = "base";
    public static final String NBT_MODS = "mods";

    public final String baseSpellId;
    public final List<SpellModifier> modifiers;

    public SpellComposition(String baseSpellId, List<SpellModifier> modifiers) {
        this.baseSpellId = baseSpellId;
        this.modifiers = List.copyOf(modifiers);
    }

    public static SpellComposition simple(String baseSpellId) {
        return new SpellComposition(baseSpellId, List.of());
    }

    public boolean isComposed() {
        return !modifiers.isEmpty();
    }

    public SpellDef baseDef() {
        return SpellLibrary.get(baseSpellId);
    }

    /** Conta quantos stacks tem de cada modifier. */
    public Map<SpellModifier, Integer> stackCounts() {
        Map<SpellModifier, Integer> out = new EnumMap<>(SpellModifier.class);
        for (SpellModifier m : modifiers) out.merge(m, 1, Integer::sum);
        return out;
    }

    /** Quantos modifier slots (total) — útil pra GUI mostrar "Página X/Y". */
    public int pageCount() {
        return 1 + modifiers.size(); // base + cada modifier = 1 página
    }

    // ── Stat calculators ───────────────────────────────────────────────

    public int finalManaCost() {
        SpellDef base = baseDef();
        if (base == null) return 9999;
        float mana = base.manaCost;
        for (SpellModifier m : modifiers) mana *= m.manaMultiplier;
        return Math.max(1, Math.round(mana));
    }

    public float finalDamage() {
        SpellDef base = baseDef();
        if (base == null) return 0F;
        float dmg = base.damage;
        for (SpellModifier m : modifiers) dmg *= m.damageMultiplier;
        return dmg;
    }

    public float finalRange() {
        SpellDef base = baseDef();
        if (base == null) return 0F;
        float r = base.range;
        for (SpellModifier m : modifiers) r *= m.rangeMultiplier;
        return r;
    }

    public int finalCooldownTicks() {
        SpellDef base = baseDef();
        if (base == null) return 60;
        float cd = base.cooldownTicks;
        for (SpellModifier m : modifiers) cd *= m.cooldownMultiplier;
        return Math.max(20, Math.round(cd));
    }

    /** Build SpellDef "virtual" com stats finais — usado por UniversalSpellScrollItem. */
    public SpellDef toVirtualDef() {
        SpellDef base = baseDef();
        if (base == null) return null;
        return SpellDef.builder(base.id)
                .name(base.name + (isComposed() ? " (Composto)" : ""))
                .school(base.school)
                .rarity(base.rarity)
                .mana(finalManaCost())
                .cooldown(finalCooldownTicks())
                .damage(finalDamage())
                .range(finalRange())
                .lore(base.lore)
                .cast(base.cast)
                .build();
    }

    // ── NBT ────────────────────────────────────────────────────────────

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_BASE, baseSpellId);
        ListTag modList = new ListTag();
        for (SpellModifier m : modifiers) modList.add(StringTag.valueOf(m.id));
        tag.put(NBT_MODS, modList);
        return tag;
    }

    public static SpellComposition fromNbt(CompoundTag tag) {
        if (tag == null || !tag.contains(NBT_BASE)) return null;
        String base = tag.getString(NBT_BASE);
        List<SpellModifier> mods = new ArrayList<>();
        ListTag modList = tag.getList(NBT_MODS, 8); // 8 = string
        for (int i = 0; i < modList.size(); i++) {
            SpellModifier m = SpellModifier.byId(modList.getString(i));
            if (m != null) mods.add(m);
        }
        return new SpellComposition(base, mods);
    }

    public static SpellComposition fromStack(ItemStack stack) {
        if (stack == null || !stack.hasTag()) return null;
        CompoundTag t = stack.getTag();
        if (!t.contains(NBT_COMPOSITION)) return null;
        return fromNbt(t.getCompound(NBT_COMPOSITION));
    }

    public void writeToStack(ItemStack stack) {
        stack.getOrCreateTag().put(NBT_COMPOSITION, toNbt());
    }

    // ── Tooltip ───────────────────────────────────────────────────────

    public List<Component> tooltip() {
        List<Component> out = new ArrayList<>();
        SpellDef base = baseDef();
        if (base == null) return out;

        out.add(Component.literal("§6═ Páginas: §e" + pageCount() + " §6═"));
        out.add(Component.literal("§7Base: ").append(base.displayName()));
        if (isComposed()) {
            out.add(Component.literal("§7Modificadores §8(efeito real):"));
            Map<SpellModifier, Integer> counts = stackCounts();
            for (var entry : counts.entrySet()) {
                SpellModifier m = entry.getKey();
                String stackStr = entry.getValue() > 1 ? " §7×" + entry.getValue() : "";
                out.add(Component.literal("  §8• ").append(m.label())
                        .append(Component.literal(stackStr)));
                // r173 (Wave 7): documenta o EFEITO REAL de cada componente no spell criado
                out.add(Component.literal("      §8§o" + m.description));
            }
        }
        out.add(Component.literal("§7Mana: §b" + finalManaCost()
                + " §7• Dano: §c" + String.format("%.1f", finalDamage())
                + " §7• CD: §e" + (finalCooldownTicks() / 20) + "s"));
        if (finalDamage() >= 1000F) {
            out.add(Component.literal("§4§l⚡ APOLÃO LENDÁRIO ⚡").withStyle(net.minecraft.ChatFormatting.OBFUSCATED));
        }
        return out;
    }

    /** Validação: pode adicionar mais um modifier? */
    public boolean canAdd(SpellModifier mod) {
        if (mod == SpellModifier.APOLAO) {
            // Só permite Apolão em spells EPIC base
            SpellDef base = baseDef();
            if (base == null || base.rarity != net.minecraft.world.item.Rarity.EPIC) return false;
        }
        int currentStacks = (int) modifiers.stream().filter(m -> m == mod).count();
        return currentStacks < mod.maxStacks;
    }
}
