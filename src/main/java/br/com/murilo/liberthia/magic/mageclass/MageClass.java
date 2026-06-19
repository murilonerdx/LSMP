package br.com.murilo.liberthia.magic.mageclass;

import net.minecraft.ChatFormatting;

/**
 * r162: <b>13 classes de mago</b> — cada uma confere bônus por feitiço cast.
 *
 * <p>Bônus escalam linearmente com level (1-10). Fórmula:
 * <code>base + (level - 1) * step</code>. Level 1 = base, Level 10 = base + 9 × step.
 */
public enum MageClass {
    // ─── 5 classes originais (user spec) ────────────────────────────────
    NECROMANCER ("Necromante",    "§5",    15, 1.0F, "chaos",  "poison",   10),
    OBFUSCATOR  ("Ofuscador",     "§2",    10, 1.5F, "poison", "sanity",    8),
    PYROMANCER  ("Piromante",     "§c",    15, 1.0F, "fire",   "burn",      8),
    VOID_MAGE   ("Vazio",         "§5",    10, 1.0F, "chaos",  "void",      5),
    PARACHOQUE  ("Parachoque",    "§6",     5, 1.0F, "fire",   "burn",      8),

    // ─── 8 NEW classes ────────────────────────────────────────────────
    HEALER      ("Curandeiro",    "§a",    12, 1.2F, "heal",   "regen",     5),
    GROUP_CTRL  ("Mestre do Grupo","§b",    8, 1.0F, "slow",   "weaken",    5),
    CRIT_LORD   ("Senhor Crítico","§e",     5, 2.0F, "crit",   "double",    1),
    JUGGERNAUT  ("Força Bruta",   "§4",    20, 1.5F, "raw",    "kb",        3),
    POWER_MAGE  ("Mago Poderoso", "§d",    18, 1.0F, "all",    "amplify",   3),
    ESCAPIST    ("Escapista",     "§7",     0, 0.0F, "evade",  "iframe",   60),
    SPEEDSTER   ("Veloz",         "§b",     5, 1.0F, "speed",  "haste",     8),
    DASH_MASTER ("Mestre Dash",   "§3",    10, 1.5F, "dash",   "lunge",     5),
    AVIATOR     ("Aviador",       "§f",     3, 0.5F, "fly",    "levitate",  5);

    public final String displayName;
    public final String colorCode;
    /** Base damage % bonus (level 1). */
    public final int baseDmgPct;
    /** Damage % step per level (level N = base + (N-1)*step). */
    public final float stepPct;
    /** Primary effect type. */
    public final String primaryEffect;
    /** Secondary effect type. */
    public final String secondaryEffect;
    /** Effect duration in ticks. */
    public final int effectDuration;

    MageClass(String name, String color, int baseDmg, float step,
              String primary, String secondary, int duration) {
        this.displayName = name;
        this.colorCode = color;
        this.baseDmgPct = baseDmg;
        this.stepPct = step;
        this.primaryEffect = primary;
        this.secondaryEffect = secondary;
        this.effectDuration = duration;
    }

    /** Retorna o bonus de dano (%) para um level específico. */
    public int dmgBonusAt(int level) {
        int lv = Math.max(1, Math.min(10, level));
        return baseDmgPct + (int)((lv - 1) * stepPct);
    }

    /** Multiplicador final do dano. */
    public float dmgMultiplier(int level) {
        return 1.0F + dmgBonusAt(level) / 100F;
    }

    public ChatFormatting chatColor() {
        return switch (colorCode) {
            case "§5" -> ChatFormatting.DARK_PURPLE;
            case "§c" -> ChatFormatting.RED;
            case "§6" -> ChatFormatting.GOLD;
            case "§a" -> ChatFormatting.GREEN;
            case "§b" -> ChatFormatting.AQUA;
            case "§e" -> ChatFormatting.YELLOW;
            case "§4" -> ChatFormatting.DARK_RED;
            case "§d" -> ChatFormatting.LIGHT_PURPLE;
            case "§7" -> ChatFormatting.GRAY;
            case "§3" -> ChatFormatting.DARK_AQUA;
            case "§f" -> ChatFormatting.WHITE;
            case "§2" -> ChatFormatting.DARK_GREEN;
            default -> ChatFormatting.WHITE;
        };
    }
}
