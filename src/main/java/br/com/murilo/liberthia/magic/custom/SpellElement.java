package br.com.murilo.liberthia.magic.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * v0.1.22 r42: Elementos que afetam o "secondary effect" das custom spells.
 *
 * <p>Cada element aplica um MobEffect no alvo (ou no caster pra self) e
 * tem um leve modificador de dano por dano-tipo.
 */
public enum SpellElement {
    FIRE("Fogo", 0xFF6020, MobEffects.MOVEMENT_SPEED, true, "Acende em fogo 8s, dmg fogo"),
    ICE("Gelo", 0x88DDFF, MobEffects.MOVEMENT_SLOWDOWN, false, "Slowness IV 5s, dmg gélido"),
    VOID("Vazio", 0x6E2DA0, MobEffects.WITHER, false, "Wither II 5s, dmg mágico"),
    LIGHT("Luz", 0xFFE060, MobEffects.GLOWING, false, "Glowing 10s + bonus vs undead"),
    BLOOD("Sangue", 0xB01010, MobEffects.HUNGER, false, "Hunger III, lifesteal 30%"),
    ARCANE("Arcano", 0xB050FF, MobEffects.LEVITATION, false, "Levitation 1.5s, dmg mágico"),
    EARTH("Terra", 0x804020, MobEffects.DIG_SLOWDOWN, false, "Mining Fatigue II, knockback"),
    LIGHTNING("Eletricidade", 0xAACCFF, MobEffects.MOVEMENT_SLOWDOWN, false, "Stun 1s, dmg elétrico"),
    SHADOW("Sombra", 0x301050, MobEffects.BLINDNESS, false, "Blindness 8s, dmg trevas"),
    NATURE("Natureza", 0x50C040, MobEffects.POISON, false, "Poison II 10s, dmg veneno");

    public final String displayName;
    public final int color;
    public final MobEffect appliedEffect;
    /** true se o effect é positivo (aplica no caster), false = aplica no alvo. */
    public final boolean isBuff;
    public final String description;

    SpellElement(String displayName, int color, MobEffect effect, boolean isBuff, String description) {
        this.displayName = displayName;
        this.color = color;
        this.appliedEffect = effect;
        this.isBuff = isBuff;
        this.description = description;
    }

    public Component displayComponent() {
        return Component.literal(displayName).withStyle(s -> s.withColor(color));
    }

    public static SpellElement byOrdinal(int i) {
        var v = values();
        if (i < 0 || i >= v.length) return FIRE;
        return v[i];
    }
}
