package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blood Infection block — infectious corruption. Spreads to adjacent normal
 * terrain (grass/dirt/stone/sand) and infects entities that step on it or
 * walk nearby with Blood Infection II.
 * Contained by 4+ chalk symbols nearby.
 */
public class BloodInfectionBlock extends Block {
    public BloodInfectionBlock(Properties p) { super(p); }

    @Override public boolean isRandomlyTicking(BlockState s) { return false; /* DISABLED */ }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        /* DISABLED — kill switch permanente */
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        // Constant red dust for visibility
        for (int i = 0; i < 2; i++) {
            level.addParticle(BloodParticles.BLOOD,
                    pos.getX() + rng.nextDouble(),
                    pos.getY() + 1.02,
                    pos.getZ() + rng.nextDouble(),
                    (rng.nextDouble() - 0.5) * 0.05, 0.04, (rng.nextDouble() - 0.5) * 0.05);
        }
        if (rng.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CRIMSON_SPORE,
                    pos.getX() + rng.nextDouble(),
                    pos.getY() + 1.05,
                    pos.getZ() + rng.nextDouble(),
                    0, 0.02, 0);
        }
        if (rng.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.3 + rng.nextDouble() * 0.4,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.3 + rng.nextDouble() * 0.4,
                    0, 0.02, 0);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (BloodKin.is(entity)) return;
        if (!level.isClientSide && entity instanceof LivingEntity le
                && !(entity instanceof Player p && p.isCreative())) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 400, 1, false, true, true));
        }
    }
}
