package br.com.murilo.liberthia.observation.api;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * v0.1.22 r71: <b>SpellClass</b> — classificação elemental dos feitiços.
 *
 * <p>Cada parte da observação pertence a uma classe (Fogo/Água/Terra/Ar/Cósmico).
 * Quando ImbuedSword equipa um spell, sua classe define as propriedades extras.
 *
 * <h2>Propriedades por classe</h2>
 * <table>
 *   <tr><th>Classe</th><th>Características</th></tr>
 *   <tr><td>FIRE</td><td>dano, destruição, purificação, proteção contra escuridão</td></tr>
 *   <tr><td>WATER</td><td>fluidez, manipulação, poção, rapidez, utilidade</td></tr>
 *   <tr><td>EARTH</td><td>material, mineração, encontrar minérios, teletransporte</td></tr>
 *   <tr><td>AIR</td><td>voar, velocidade, inteligência, teletransporte</td></tr>
 *   <tr><td>COSMIC</td><td>especial: horror, decay, gravidade, manipulação mental</td></tr>
 * </table>
 */
public enum SpellClass {
    FIRE(0xFF6633, ChatFormatting.RED, "Fogo"),
    WATER(0x3399FF, ChatFormatting.BLUE, "Água"),
    EARTH(0x99CC33, ChatFormatting.GREEN, "Terra"),
    AIR(0xEEEEFF, ChatFormatting.WHITE, "Ar"),
    COSMIC(0x9D4DD6, ChatFormatting.LIGHT_PURPLE, "Cósmico"),
    NONE(0xAAAAAA, ChatFormatting.GRAY, "Neutro");

    public final int color;
    public final ChatFormatting format;
    public final String displayName;

    SpellClass(int color, ChatFormatting format, String displayName) {
        this.color = color;
        this.format = format;
        this.displayName = displayName;
    }

    public Component component() {
        return Component.literal(displayName).withStyle(format);
    }
}
