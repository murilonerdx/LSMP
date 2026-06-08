package br.com.murilo.liberthia.faction;

import net.minecraft.world.entity.Entity;

/**
 * Resolves the {@link Faction} of any entity. Used by damage modifiers and
 * targeting logic. Keep in sync with {@link br.com.murilo.liberthia.logic.BloodKin}.
 */
public final class FactionTag {
    private FactionTag() {}

    public static Faction get(Entity e) {
        if (e == null) return Faction.NEUTRAL;
        // r179: usa a lista canônica do BloodKin (inclui BloodMage, BloodHound,
        // BloodWarden, WeavingShade, Disarmer). Antes FactionTag estava dessincronizado
        // → o Paladino não reconhecia metade da facção de sangue como inimiga (#64-66).
        if (br.com.murilo.liberthia.logic.BloodKin.is(e)) {
            return Faction.BLOOD;
        }
        if (e instanceof br.com.murilo.liberthia.entity.OrderPaladinEntity) {
            return Faction.ORDER;
        }
        return Faction.NEUTRAL;
    }

    public static boolean isBlood(Entity e) { return get(e) == Faction.BLOOD; }
    public static boolean isOrder(Entity e) { return get(e) == Faction.ORDER; }
}
