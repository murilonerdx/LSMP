package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Veil of Madness — full-cube blood-themed block. While placed, every 30 ticks
 * applies §dBlindness §6+ Nausea + Weakness §7to all non-{@link BloodKin}
 * living entities within 6 blocks. Periodically also a brief soul-fire flash.
 *
 * Uses {@code randomTick} to drive the sweep — keeps it cheap.
 */
public class VeilOfMadnessBlock extends Block {
    private static final double RANGE = 6.0;

    public VeilOfMadnessBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) { return false; /* DISABLED */ }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        /* DISABLED — kill switch permanente */
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        // ambient client-side particles drift up
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.SCULK_SOUL,
                    pos.getX() + rand.nextDouble(),
                    pos.getY() + 1.0 + rand.nextDouble() * 0.4,
                    pos.getZ() + rand.nextDouble(),
                    (rand.nextDouble() - 0.5) * 0.05,
                    0.02 + rand.nextDouble() * 0.04,
                    (rand.nextDouble() - 0.5) * 0.05);
        }
    }
}
