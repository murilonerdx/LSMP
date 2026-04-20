package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.entity.BloodCultistEntity;
import br.com.murilo.liberthia.entity.BloodOrbEntity;
import br.com.murilo.liberthia.entity.BloodPriestEntity;
import br.com.murilo.liberthia.entity.BloodWormEntity;
import br.com.murilo.liberthia.entity.FleshCrawlerEntity;
import br.com.murilo.liberthia.entity.GoreWormEntity;
import br.com.murilo.liberthia.entity.WoundedPilgrimEntity;
import net.minecraft.world.entity.Entity;

/**
 * Identifies "blood-kin" entities — the worms and orbs spawned by the
 * blood system. These must be IMMUNE to damage/infection from blood blocks
 * (altar, flesh, spikes, infection block, volcano, fountain, etc.), since
 * otherwise the creatures we spawn would attack themselves and each other.
 */
public final class BloodKin {
    private BloodKin() {}

    /** True if {@code e} is one of the blood system's own creatures. */
    public static boolean is(Entity e) {
        return e instanceof BloodWormEntity
                || e instanceof FleshCrawlerEntity
                || e instanceof GoreWormEntity
                || e instanceof BloodOrbEntity
                || e instanceof BloodCultistEntity
                || e instanceof BloodPriestEntity
                || e instanceof WoundedPilgrimEntity;
    }
}
