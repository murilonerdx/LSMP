package br.com.murilo.liberthia.magic.spell;

import net.minecraft.world.item.ItemStack;

/**
 * v0.1.145 r112: <b>Spell Levels</b> — sistema de níveis 1–3 por feitiço
 * (estilo Iron's Spells N Spellbooks).
 *
 * <h2>Como funciona</h2>
 * <ul>
 *   <li>Cada {@link UniversalSpellScrollItem} guarda NBT {@code liberthia.spell_level} (1-3)</li>
 *   <li>Level 1 = base, Level 2 = +25% dano/range, Level 3 = +50%</li>
 *   <li>Custo de mana sobe junto: Lv2 +30%, Lv3 +60%</li>
 *   <li>Upgrade no Inscription Table com Source Berry / Wisdom Orb</li>
 * </ul>
 */
public final class SpellLevels {

    public static final String NBT_LEVEL = "liberthia.spell_level";
    public static final int MIN = 1;
    public static final int MAX = 3;

    private SpellLevels() {}

    public static int getLevel(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(NBT_LEVEL)) {
            return Math.max(MIN, Math.min(MAX, stack.getTag().getInt(NBT_LEVEL)));
        }
        return MIN;
    }

    public static void setLevel(ItemStack stack, int level) {
        int clamped = Math.max(MIN, Math.min(MAX, level));
        stack.getOrCreateTag().putInt(NBT_LEVEL, clamped);
    }

    /** Damage multiplier por level: 1=1.0, 2=1.25, 3=1.5 */
    public static float damageMultiplier(int level) {
        return switch (level) {
            case 2 -> 1.25F;
            case 3 -> 1.5F;
            default -> 1.0F;
        };
    }

    /** Range multiplier por level. */
    public static float rangeMultiplier(int level) {
        return switch (level) {
            case 2 -> 1.15F;
            case 3 -> 1.3F;
            default -> 1.0F;
        };
    }

    /** Mana multiplier por level (custo sobe). */
    public static float manaMultiplier(int level) {
        return switch (level) {
            case 2 -> 1.3F;
            case 3 -> 1.6F;
            default -> 1.0F;
        };
    }

    /** Cooldown multiplier por level (cooldown DIMINUI levels altos). */
    public static float cooldownMultiplier(int level) {
        return switch (level) {
            case 2 -> 0.9F;
            case 3 -> 0.75F;
            default -> 1.0F;
        };
    }

    /** Roman numeral label pra display. */
    public static String label(int level) {
        return switch (level) {
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
    }
}
