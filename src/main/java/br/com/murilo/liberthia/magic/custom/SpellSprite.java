package br.com.murilo.liberthia.magic.custom;

import net.minecraft.network.chat.Component;

/**
 * v0.1.22 r42: Enum dos 10 spell VFX sprite sheets disponíveis pro player
 * escolher quando craftar uma spell custom.
 *
 * <p>Cada um corresponde a um {@code spell_<id>_[0-3].png} em
 * {@code assets/liberthia/textures/particle/}.
 *
 * <p>Sprites são 16×16, 4 frames cada, gerados pelo
 * {@code tools/gen_spell_vfx_sprites.py}.
 */
public enum SpellSprite {
    FIRE_BLAST("fire_blast", "Explosão de Fogo", 0xFF6020, "Núcleo amarelo com flames vermelhos"),
    ICE_LANCE("ice_lance", "Lança de Gelo", 0x88DDFF, "Diamond cyan com frost crystals"),
    VOID_ORB("void_orb", "Orbe do Vazio", 0x6E2DA0, "Esfera escura com chromatic ring"),
    LIGHT_RAY("light_ray", "Raio de Luz", 0xFFE060, "Cruz dourada com sparkles"),
    BLOOD_SHOT("blood_shot", "Tiro de Sangue", 0xB01010, "Orbe vermelho com pingo"),
    ARCANE_MISSILE("arcane_missile", "Míssil Arcano", 0xB050FF, "Estrela 5 pontas roxa"),
    EARTH_SPIKE("earth_spike", "Espinho de Terra", 0x804020, "Triângulo rochoso marrom"),
    LIGHTNING_BOLT("lightning_bolt", "Raio Elétrico", 0xAACCFF, "Zigzag azul-branco"),
    SHADOW_DART("shadow_dart", "Dardo de Sombra", 0x301050, "Dardo preto com fumaça"),
    NATURE_THORN("nature_thorn", "Espinho da Natureza", 0x50C040, "Vinha com folhas e espinhos");

    public final String id;
    public final String displayName;
    public final int color;
    public final String description;

    SpellSprite(String id, String displayName, int color, String description) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.description = description;
    }

    public Component displayComponent() {
        return Component.literal(displayName).withStyle(s -> s.withColor(color));
    }

    /** Texture base path (sem _0/_1/_2/_3 e sem .png). */
    public String textureBase() {
        return "liberthia:particle/spell_" + id;
    }

    public static SpellSprite byOrdinal(int i) {
        var v = values();
        if (i < 0 || i >= v.length) return FIRE_BLAST;
        return v[i];
    }
}
