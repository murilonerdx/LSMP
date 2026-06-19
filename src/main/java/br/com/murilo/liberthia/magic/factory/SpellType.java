package br.com.murilo.liberthia.magic.factory;

/**
 * r148: <b>12 tipos de comportamento</b> que um feitiço pode ter na Spell Factory.
 *
 * <p>Cada tipo determina HOW o spell se executa — a forma de delivery do efeito.
 * O que ele FAZ (dano, cura, status) é configurado separadamente em {@code effects}.
 *
 * <h2>Categorias</h2>
 * <ul>
 *   <li><b>Projectile-like:</b> PROJECTILE, HOMING, MULTI_SHOT, ARC, BEAM</li>
 *   <li><b>Area:</b> EXPLOSION, NOVA, CONE, RAIN, AURA</li>
 *   <li><b>Personal:</b> TOUCH, SELF, DASH</li>
 * </ul>
 */
public enum SpellType {
    /** Linha reta, voa, hit-target. Ex: Fireball. */
    PROJECTILE,
    /** Igual PROJECTILE mas trava alvo + persegue. Ex: Magic Missile. */
    HOMING,
    /** Dispara N projéteis em cone. Ex: Triple Shot. */
    MULTI_SHOT,
    /** Projétil com gravidade — parábola. Ex: Stone Toss. */
    ARC,
    /** Feixe contínuo a partir do caster (channelled). Ex: Death Beam. */
    BEAM,
    /** AOE instantâneo no ponto do crosshair. Ex: Meteor. */
    EXPLOSION,
    /** Onda expandindo do caster. Ex: Frost Nova. */
    NOVA,
    /** Cone de dano frontal (ângulo + range). Ex: Dragon Breath. */
    CONE,
    /** Strikes aleatórios caindo do céu numa área. Ex: Meteor Storm. */
    RAIN,
    /** Aura sustentada ao redor do caster (DoT + buff). Ex: Burning Aura. */
    AURA,
    /** Single-target melee range. Ex: Vampiric Touch. */
    TOUCH,
    /** Buff/debuff ao próprio caster. Ex: Haste, Shield. */
    SELF,
    /** Move o caster numa direção (look dir / forward). Ex: Blink, Phase Dash. */
    DASH;

    public boolean isProjectileLike() {
        return this == PROJECTILE || this == HOMING || this == MULTI_SHOT || this == ARC;
    }

    public boolean isAreaEffect() {
        return this == EXPLOSION || this == NOVA || this == CONE || this == RAIN;
    }
}
