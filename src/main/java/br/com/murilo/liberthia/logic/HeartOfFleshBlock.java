package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pulsing flesh heart block. Placed at the four cardinal offsets (±3,0,0 and
 * 0,0,±3) of a FleshMother to start the boss ritual. The summoning check
 * lives in {@link FleshMotherBlock} randomTick.
 */
public class HeartOfFleshBlock extends Block {
    public HeartOfFleshBlock(Properties p) { super(p); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(BloodParticles.BLOOD,
                    pos.getX() + rng.nextDouble(),
                    pos.getY() + 0.8 + rng.nextDouble() * 0.4,
                    pos.getZ() + rng.nextDouble(),
                    (rng.nextDouble() - 0.5) * 0.05,
                    0.02,
                    (rng.nextDouble() - 0.5) * 0.05);
        }
        if (rng.nextFloat() < 0.15F) {
            level.addParticle(ParticleTypes.HEART,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    0, 0.05, 0);
        }
    }
}
