package br.com.murilo.liberthia.faction;

import br.com.murilo.liberthia.entity.BloodCultistEntity;
import br.com.murilo.liberthia.entity.BloodOrbEntity;
import br.com.murilo.liberthia.entity.BloodPriestEntity;
import br.com.murilo.liberthia.entity.BloodWormEntity;
import br.com.murilo.liberthia.entity.FleshCrawlerEntity;
import br.com.murilo.liberthia.entity.GoreWormEntity;
import br.com.murilo.liberthia.entity.WoundedPilgrimEntity;
import net.minecraft.world.entity.Entity;

/**
 * Resolves the {@link Faction} of any entity. Used by damage modifiers and
 * targeting logic. Keep in sync with {@link br.com.murilo.liberthia.logic.BloodKin}.
 */
public final class FactionTag {
    private FactionTag() {}

    public static Faction get(Entity e) {
        if (e == null) return Faction.NEUTRAL;
        if (e instanceof BloodCultistEntity
                || e instanceof BloodPriestEntity
                || e instanceof WoundedPilgrimEntity
                || e instanceof BloodWormEntity
                || e instanceof FleshCrawlerEntity
                || e instanceof GoreWormEntity
                || e instanceof BloodOrbEntity
                || e instanceof br.com.murilo.liberthia.entity.FleshMotherBossEntity) {
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
