package br.com.murilo.liberthia.effect;

import br.com.murilo.liberthia.logic.BloodKin;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Infected Sight — Occultism-{@code ThirdEyeEffect}-inspired buff.
 *
 * <p>While active: every tick, the server highlights nearby Blood-kin entities
 * with a red dust particle ring at the entity's position. Even through walls
 * (particles are rendered after entity culling on the client), so the player
 * can spot infected mobs through chunks.
 *
 * <p>Server-only logic — no client renderer needed.
 */
public class InfectedSightEffect extends MobEffect {

    private static final float SCAN_RADIUS = 24F;

    public InfectedSightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xb22a3a);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (!(entity instanceof ServerPlayer p)) return;
        if (p.tickCount % 6 != 0) return; // ~3 sweeps per second

        ServerLevel sl = (ServerLevel) p.level();
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(1.0F, 0.15F, 0.18F), 1.6F);

        AABB box = p.getBoundingBox().inflate(SCAN_RADIUS);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == p) continue;
            if (!BloodKin.is(le)) continue;
            // Outline silhouette: 8 particles in a vertical ring around the entity.
            for (int i = 0; i < 8; i++) {
                double a = (i / 8.0) * Math.PI * 2 + (p.tickCount * 0.05);
                double r = le.getBbWidth() * 0.7;
                sl.sendParticles(p, dust,
                        true,                // forced — ignores particle distance limit
                        le.getX() + Math.cos(a) * r,
                        le.getY() + le.getBbHeight() * 0.6,
                        le.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
