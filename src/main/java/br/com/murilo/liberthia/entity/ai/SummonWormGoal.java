package br.com.murilo.liberthia.entity.ai;

import br.com.murilo.liberthia.entity.BloodPriestEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Every ~6 seconds while in combat, 40% chance to summon a Gore Worm or
 * Blood Worm at the priest's feet.
 */
public class SummonWormGoal extends Goal {

    private final BloodPriestEntity priest;
    private int cooldown = 120;

    public SummonWormGoal(BloodPriestEntity priest) {
        this.priest = priest;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity tgt = priest.getTarget();
        return tgt != null && tgt.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        cooldown--;
        if (cooldown > 0) return;
        cooldown = 120 + priest.getRandom().nextInt(60);
        if (priest.getRandom().nextFloat() >= 0.40F) return;

        EntityType<?> type = priest.getRandom().nextBoolean()
                ? ModEntities.GORE_WORM.get()
                : ModEntities.BLOOD_WORM.get();
        Entity worm = type.create(priest.level());
        if (worm == null) return;
        double x = priest.getX() + (priest.getRandom().nextDouble() - 0.5) * 2;
        double y = priest.getY();
        double z = priest.getZ() + (priest.getRandom().nextDouble() - 0.5) * 2;
        worm.moveTo(x, y, z, priest.getRandom().nextFloat() * 360F, 0);
        priest.level().addFreshEntity(worm);
        if (priest.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    x, y + 0.3, z, 20, 0.4, 0.3, 0.4, 0.05);
            sl.playSound(null, priest.blockPosition(), SoundEvents.SLIME_SQUISH,
                    SoundSource.HOSTILE, 1.0F, 0.7F);
        }
    }
}
