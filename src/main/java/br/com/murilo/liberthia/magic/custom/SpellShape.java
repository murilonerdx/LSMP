package br.com.murilo.liberthia.magic.custom;

import net.minecraft.network.chat.Component;

/**
 * v0.1.22 r42: Formas que uma custom spell pode assumir.
 *
 * <p>Cada shape muda como o cast é executado e como o VFX é renderizado.
 */
public enum SpellShape {
    /** Projétil único viajando em linha reta — explode no impacto. */
    PROJECTILE("Projétil", "Voa em linha reta, dano único no impacto", 30, 1.0F),
    /** Beam contínuo a partir do caster — dano sustained. */
    BEAM("Raio Contínuo", "Beam de 24b, dmg/tick por 3s", 50, 0.4F),
    /** Laser instantâneo — atinge tudo na linha. */
    LASER("Laser", "Hit-scan instantâneo, dmg max", 60, 1.5F),
    /** AOE em volta do caster. */
    AOE("Área", "Explode em volta do caster (8b raio)", 50, 0.8F),
    /** Self buff/heal. */
    SELF("Si Mesmo", "Efeito aplicado em você", 20, 0F),
    /** Touch — afeta primeira entidade tocada. */
    TOUCH("Toque", "Single target a 4b de distância", 25, 1.2F);

    public final String displayName;
    public final String description;
    /** Custo base de mana — multiplicado pelo power. */
    public final int baseManaCost;
    /** Multiplicador de dano. */
    public final float damageMultiplier;

    SpellShape(String displayName, String description, int baseManaCost, float damageMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.baseManaCost = baseManaCost;
        this.damageMultiplier = damageMultiplier;
    }

    public Component displayComponent() {
        return Component.literal(displayName);
    }

    public static SpellShape byOrdinal(int i) {
        var v = values();
        if (i < 0 || i >= v.length) return PROJECTILE;
        return v[i];
    }
}
