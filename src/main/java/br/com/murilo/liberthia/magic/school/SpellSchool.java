package br.com.murilo.liberthia.magic.school;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * v0.1.24 r82: <b>SpellSchool</b> — 7 escolas de magia inspiradas no Iron's
 * Spells N Spellbooks.
 *
 * <p>Cada spell/glyph/projectile carrega uma school. Players ganham bonus
 * de damage/resistance por school via Curios/armor/perks. Affinity bonus
 * em uma school = penalty em a oposta.
 *
 * <h2>7 Schools</h2>
 * <ul>
 *   <li>{@code FIRE} — burn, immolate, melt. Oposto: ICE.</li>
 *   <li>{@code ICE} — chill, frostbite, freeze. Oposto: FIRE.</li>
 *   <li>{@code LIGHTNING} — shock, static, chain. Oposto: NATURE.</li>
 *   <li>{@code BLOOD} — bleed, lifesteal, hemorrhage. Oposto: HOLY.</li>
 *   <li>{@code ELDRITCH} — confusion, void, cosmic. Oposto: HOLY.</li>
 *   <li>{@code HOLY} — heal, smite, ward. Oposto: BLOOD/ELDRITCH.</li>
 *   <li>{@code NATURE} — grow, root, poison. Oposto: LIGHTNING.</li>
 * </ul>
 */
public enum SpellSchool {
    FIRE      ("Fogo",      ChatFormatting.RED,         0xFFFF6633),
    ICE       ("Gelo",      ChatFormatting.AQUA,        0xFF66CCFF),
    LIGHTNING ("Raio",      ChatFormatting.YELLOW,      0xFFFFFF44),
    BLOOD     ("Sangue",    ChatFormatting.DARK_RED,    0xFF990033),
    ELDRITCH  ("Eldritch",  ChatFormatting.DARK_PURPLE, 0xFF6633CC),
    HOLY      ("Sagrado",   ChatFormatting.WHITE,       0xFFFFEEAA),
    NATURE    ("Natureza",  ChatFormatting.DARK_GREEN,  0xFF33AA33);

    private final String displayName;
    private final ChatFormatting color;
    private final int colorHex;

    SpellSchool(String displayName, ChatFormatting color, int colorHex) {
        this.displayName = displayName;
        this.color = color;
        this.colorHex = colorHex;
    }

    public String displayName() { return displayName; }
    public ChatFormatting color() { return color; }
    public int colorHex() { return colorHex; }

    public Component label() {
        return Component.literal(displayName).withStyle(color);
    }

    /** Retorna a escola oposta (resistência cruzada). */
    public SpellSchool opposite() {
        return switch (this) {
            case FIRE -> ICE;
            case ICE -> FIRE;
            case LIGHTNING -> NATURE;
            case NATURE -> LIGHTNING;
            case BLOOD -> HOLY;
            case ELDRITCH -> HOLY;
            case HOLY -> ELDRITCH;
        };
    }

    /** Multiplicador de dano baseado em resistance do alvo. 0.0 = imune, 2.0 = vulnerável. */
    public static float computeMultiplier(float resistance) {
        // resistance é -100 a +100. 100=imune. -100=2x dano.
        float clamped = Math.max(-100, Math.min(100, resistance));
        return 1.0F - (clamped / 100.0F);
    }
}
