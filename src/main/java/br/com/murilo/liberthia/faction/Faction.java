package br.com.murilo.liberthia.faction;

/**
 * Faction tag used across the mod for damage modifiers, targeting, and
 * reputation bookkeeping. {@code NEUTRAL} is the default for vanilla mobs.
 * {@code BLOOD} = cult/flesh; {@code ORDER} = holy/paladin (Fase 5).
 */
public enum Faction {
    NEUTRAL,
    BLOOD,
    ORDER
}
