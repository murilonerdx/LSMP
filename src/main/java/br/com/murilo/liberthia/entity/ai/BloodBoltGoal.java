package br.com.murilo.liberthia.entity.ai;

import br.com.murilo.liberthia.entity.BloodPriestEntity;
import br.com.murilo.liberthia.entity.projectile.HemoBoltEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Priest ranged attack: fire a HemoBolt at the current target every 80 ticks.
 */
public class BloodBoltGoal extends Goal {

    private final BloodPriestEntity priest;
    private int cooldown = 80;

    public BloodBoltGoal(BloodPriestEntity priest) {
        this.priest = priest;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity tgt = priest.getTarget();
        return tgt != null && tgt.isAlive() && priest.distanceToSqr(tgt) < 30.0 * 30.0;
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        LivingEntity tgt = priest.getTarget();
        if (tgt == null) return;
        priest.getLookControl().setLookAt(tgt, 30F, 30F);
        cooldown--;
        if (cooldown > 0) return;
        cooldown = 80;

        double dx = tgt.getX() - priest.getX();
        double dy = tgt.getY(0.5) - (priest.getY() + 1.3);
        double dz = tgt.getZ() - priest.getZ();
        HemoBoltEntity bolt = new HemoBoltEntity(priest.level(), priest, dx, dy, dz);
        bolt.setPos(priest.getX(), priest.getY() + 1.3, priest.getZ());
        priest.level().addFreshEntity(bolt);

        if (priest.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    priest.getX(), priest.getY() + 1.5, priest.getZ(),
                    12, 0.2, 0.2, 0.2, 0.05);
            sl.playSound(null, priest.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }
}
