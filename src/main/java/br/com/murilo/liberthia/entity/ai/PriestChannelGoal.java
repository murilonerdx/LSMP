package br.com.murilo.liberthia.entity.ai;

import br.com.murilo.liberthia.entity.BloodPriestEntity;
import br.com.murilo.liberthia.logic.BloodAltarBlock;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * While a Blood Altar is within {@link BloodPriestEntity#CHANNEL_RADIUS},
 * the priest stands near it, heals slowly, and emits blood particles linking
 * priest→altar. Does not lock targets entirely — bolt/summon goals still fire.
 */
public class PriestChannelGoal extends Goal {

    private final BloodPriestEntity priest;
    private BlockPos altar;
    private int particleTick;

    public PriestChannelGoal(BloodPriestEntity priest) {
        this.priest = priest;
        this.setFlags(EnumSet.noneOf(Flag.class)); // non-exclusive
    }

    @Override
    public boolean canUse() {
        if (priest.tickCount % 20 != 0) return false;
        this.altar = findAltar();
        return altar != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (altar == null) return false;
        BlockState s = priest.level().getBlockState(altar);
        if (!(s.getBlock() instanceof BloodAltarBlock)) return false;
        return priest.blockPosition().distSqr(altar) <= (double) (BloodPriestEntity.CHANNEL_RADIUS * BloodPriestEntity.CHANNEL_RADIUS);
    }

    @Override
    public void tick() {
        if (altar == null) return;
        // Self-heal 0.5 HP/s
        if (priest.tickCount % 40 == 0 && priest.getHealth() < priest.getMaxHealth()) {
            priest.heal(1.0F);
        }
        // Beam particles every few ticks
        particleTick++;
        if (particleTick >= 4 && priest.level() instanceof ServerLevel sl) {
            particleTick = 0;
            double sx = priest.getX();
            double sy = priest.getY() + 1.4;
            double sz = priest.getZ();
            double dx = (altar.getX() + 0.5) - sx;
            double dy = (altar.getY() + 0.8) - sy;
            double dz = (altar.getZ() + 0.5) - sz;
            int steps = 8;
            for (int i = 0; i < steps; i++) {
                double t = i / (double) steps;
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        sx + dx * t, sy + dy * t, sz + dz * t,
                        1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    private BlockPos findAltar() {
        BlockPos c = priest.blockPosition();
        int r = BloodPriestEntity.CHANNEL_RADIUS;
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                for (int dy = -6; dy <= 6; dy += 2) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (priest.level().getBlockState(p).is(ModBlocks.BLOOD_ALTAR.get())) {
                        return p.immutable();
                    }
                }
            }
        }
        return null;
    }
}
