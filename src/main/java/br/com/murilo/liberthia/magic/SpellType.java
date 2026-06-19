package br.com.murilo.liberthia.magic;

/**
 * v0.1.22 r36: Categorias de feitiço — define como SpellExecutor processa.
 */
public enum SpellType {
    /** Projétil que viaja em linha reta até atingir alvo/bloco. */
    PROJECTILE,
    /** Beam contínuo desde caster até onde olha (raycast a cada tick). */
    BEAM,
    /** Área-of-effect imediato em volta do caster. */
    AOE,
    /** Buff/debuff aplicado no caster (regen, speed, etc). */
    SELF_BUFF,
    /** Heal direto no caster ou alvo olhado. */
    HEAL,
    /** Teleporte do caster pra onde está olhando. */
    TELEPORT,
    /** Invoca entidade leal. */
    SUMMON,
    /** Drena HP/mana de inimigos pra caster. */
    DRAIN,
    /** Curse — aplica efeito negativo num alvo específico. */
    CURSE
}
