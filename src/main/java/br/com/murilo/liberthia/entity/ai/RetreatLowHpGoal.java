package br.com.murilo.liberthia.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * When the mob's HP drops below {@link #threshold}, runs away from its target
 * for a few seconds and applies self-regen ticks. Restores aggression once HP
 * climbs above 60%.
 */
public class RetreatLowHpGoal extends Goal {
    private final PathfinderMob mob;
    private final float threshold;
    private final double speed;
    private int retreatTicks;
    private boolean retreating;

    public RetreatLowHpGoal(PathfinderMob mob, float threshold, double speed) {
        this.mob = mob;
        this.threshold = threshold;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity tgt = mob.getTarget();
        if (tgt == null) return false;
        return mob.getHealth() / mob.getMaxHealth() < threshold;
    }

    @Override
    public void start() {
        retreating = true;
        retreatTicks = 60 + mob.getRandom().nextInt(40);
        Vec3 from = DefaultRandomPos.getPosAway(mob, 12, 7, mob.getTarget().position());
        if (from != null) {
            mob.getNavigation().moveTo(from.x, from.y, from.z, speed);
        }
    }

    @Override
    public boolean canContinueToUse() {
        return retreatTicks > 0
                && mob.getHealth() / mob.getMaxHealth() < 0.6F;
    }

    @Override
    public void tick() {
        retreatTicks--;
        // Self-heal trickle: 1 HP every 20 ticks while retreating.
        if (mob.tickCount % 20 == 0) {
            mob.heal(1.0F);
        }
    }

    @Override
    public void stop() {
        retreating = false;
        mob.getNavigation().stop();
    }
}
