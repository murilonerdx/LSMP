package br.com.murilo.liberthia.magic.factory;

/**
 * r151: <b>12 categorias de feitiço</b> — meta-tag pra UI/filtros/balancing.
 *
 * <p>Diferente de {@link SpellType} (que dita o COMPORTAMENTO técnico),
 * Category é o TIPO TEMÁTICO — pra usuário escolher "quero spells de DESTRUIÇÃO".
 */
public enum SpellCategory {
    EXPLOSION("Explosão"),       // burst damage point
    DESTRUCTION("Destruição"),   // high-damage finishers
    DASH("Investida"),           // movement
    RAY("Raio"),                 // beams
    CRITICAL("Crítico"),         // high crit chance
    UTILITY("Utilidade"),        // buffs/debuffs
    SUMMON("Invocação"),         // spawns entity
    AOE("Área"),                 // area effect
    CHANNELED("Canalizado"),     // held
    INSTANT("Instantâneo"),      // quick
    PROJECTILE("Projétil"),      // fired
    MIXED("Misto");              // multi-categoria

    public final String displayName;
    SpellCategory(String d) { this.displayName = d; }

    public static SpellCategory parse(String s) {
        if (s == null || s.isEmpty()) return PROJECTILE;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return PROJECTILE; }
    }
}
