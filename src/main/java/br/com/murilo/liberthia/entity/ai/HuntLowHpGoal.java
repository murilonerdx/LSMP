package br.com.murilo.liberthia.entity.ai;

import br.com.murilo.liberthia.logic.BloodKin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/**
 * Re-targets the holding {@link Mob} every tick to the lowest-HP non-creative
 * player within range. If the current target is healthier than someone else
 * nearby, the mob switches to the wounded one.
 *
 * <p>Used to make Blood-kin enemies "smart" — they smell weakness.
 */
public class HuntLowHpGoal extends Goal {
    private final Mob mob;
    private final double range;
    private int retargetCooldown;

    public HuntLowHpGoal(Mob mob, double range) {
        this.mob = mob;
        this.range = range;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() { return true; }

    @Override
    public boolean canContinueToUse() { return true; }

    @Override
    public void tick() {
        if (--retargetCooldown > 0) return;
        retargetCooldown = 20; // re-evaluate every second

        Player best = null;
        float bestHp = Float.MAX_VALUE;
        List<Player> players = mob.level().getEntitiesOfClass(Player.class,
                mob.getBoundingBox().inflate(range));
        for (Player p : players) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (BloodKin.is(p)) continue;
            if (p.getHealth() < bestHp) {
                bestHp = p.getHealth();
                best = p;
            }
        }
        if (best != null) {
            LivingEntity current = mob.getTarget();
            // Switch only if current is dead, missing, or healthier than the best candidate.
            if (current == null || !current.isAlive() || current.getHealth() > bestHp + 0.5F) {
                mob.setTarget(best);
            }
        }
    }
}
