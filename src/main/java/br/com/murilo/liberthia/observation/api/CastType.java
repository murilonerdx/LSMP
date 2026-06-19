package br.com.murilo.liberthia.observation.api;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * v0.1.22 r78: <b>CastType</b> — formato de saída do feitiço pelo Grimório.
 *
 * <p>Independente da recipe (que define O QUE faz), o CastType define COMO sai:
 *
 * <ul>
 *   <li><b>PROJECTILE</b> — projétil voador rápido (default, RANGED)</li>
 *   <li><b>BEAM</b> — raycast 32b instantâneo (LASER infinite)</li>
 *   <li><b>BURST</b> — AOE radial 6b em volta do caster (RADIAL)</li>
 *   <li><b>RAY</b> — múltiplos projéteis em cone 30° (DISPERSION)</li>
 *   <li><b>SELF</b> — aplica em si (BUFF)</li>
 * </ul>
 */
public enum CastType {
    PROJECTILE("Projétil", 0xFF6633, ChatFormatting.RED, "Voador rápido"),
    BEAM("Laser", 0xFF2222, ChatFormatting.DARK_RED, "Raio instantâneo 32b"),
    BURST("Estouro", 0xFFAA22, ChatFormatting.GOLD, "AOE radial 6b"),
    RAY("Dispersão", 0xAA22FF, ChatFormatting.LIGHT_PURPLE, "5 projéteis em cone"),
    SELF("Em Si", 0x22AAFF, ChatFormatting.AQUA, "Aplica no caster");

    public final String displayName;
    public final int color;
    public final ChatFormatting format;
    public final String hint;

    CastType(String displayName, int color, ChatFormatting format, String hint) {
        this.displayName = displayName;
        this.color = color;
        this.format = format;
        this.hint = hint;
    }

    public Component component() {
        return Component.literal(displayName).withStyle(format);
    }

    public CastType next() {
        CastType[] all = values();
        return all[(this.ordinal() + 1) % all.length];
    }

    public static CastType fromOrdinal(int n) {
        CastType[] all = values();
        if (n < 0 || n >= all.length) return PROJECTILE;
        return all[n];
    }
}
