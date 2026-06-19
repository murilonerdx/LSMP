package br.com.murilo.liberthia.magic.factory;

/**
 * r151: <b>4 elementos base + 6 combos duais</b> — ortogonal a SpellSchool.
 *
 * <p>School é a "linha mágica" (FIRE, ICE, BLOOD, etc — 7 categorias).
 * Element é o elemental ALCHEMICAL — combina-se livremente (água + fogo = vapor).
 * Use junto: spell pode ter school=BLOOD + element=WATER → magia de sangue
 * aquoso com bonus contra entidades secas.
 */
public enum SpellElement {
    // ─── Base 4 ─────────────────────────────────────────────
    NONE("Neutro", 0xCCCCCC, 0F),
    WATER("Água", 0x2266CC, 1.0F),
    FIRE("Fogo", 0xFF5500, 1.2F),
    AIR("Ar", 0xCCFFFF, 0.8F),
    EARTH("Terra", 0x8B6F47, 1.3F),

    // ─── Dual combos (6) — herdam multiplier MAIOR ──────────
    STEAM("Vapor", 0xAAAAEE, 1.4F),         // FIRE + WATER
    LAVA("Lava", 0xFF3300, 1.6F),           // FIRE + EARTH
    LIGHTNING("Raio", 0xFFFF66, 1.5F),      // FIRE + AIR
    ICE("Gelo", 0x88DDFF, 1.3F),            // WATER + AIR
    MUD("Lama", 0x664422, 1.1F),            // WATER + EARTH
    SAND("Areia", 0xDDCC88, 1.2F);          // EARTH + AIR

    public final String displayName;
    public final int colorHex;
    /** Multiplicador aplicado ao base damage da spell. */
    public final float damageMultiplier;

    SpellElement(String name, int color, float dmgMult) {
        this.displayName = name;
        this.colorHex = color;
        this.damageMultiplier = dmgMult;
    }

    public boolean isDual() {
        return this == STEAM || this == LAVA || this == LIGHTNING
                || this == ICE || this == MUD || this == SAND;
    }

    /** Combina 2 elementos base em dual. NONE se incompatível. */
    public static SpellElement combine(SpellElement a, SpellElement b) {
        if (a == b) return a;
        if ((a == FIRE && b == WATER) || (a == WATER && b == FIRE)) return STEAM;
        if ((a == FIRE && b == EARTH) || (a == EARTH && b == FIRE)) return LAVA;
        if ((a == FIRE && b == AIR) || (a == AIR && b == FIRE)) return LIGHTNING;
        if ((a == WATER && b == AIR) || (a == AIR && b == WATER)) return ICE;
        if ((a == WATER && b == EARTH) || (a == EARTH && b == WATER)) return MUD;
        if ((a == EARTH && b == AIR) || (a == AIR && b == EARTH)) return SAND;
        return NONE;
    }

    /** Parse case-insensitive de JSON. */
    public static SpellElement parse(String s) {
        if (s == null || s.isEmpty()) return NONE;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return NONE; }
    }
}
