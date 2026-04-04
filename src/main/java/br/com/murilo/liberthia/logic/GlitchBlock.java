package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Glitch Block — appears in heavily infested areas (max 10 per chunk).
 * Rapidly cycles through visual "phases" simulating block corruption/glitch.
 * Emissive rendering, no light interaction, flickers visually.
 */
public class GlitchBlock extends Block {

    // PHASE 0-7: different visual states (texture variants that simulate glitching)
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 7);

    public GlitchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PHASE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 15; // Blocks all light
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Cycle phase rapidly
        int nextPhase = (state.getValue(PHASE) + 1 + random.nextInt(3)) % 8;
        level.setBlock(pos, state.setValue(PHASE, nextPhase), 2);

        // Check if area is still heavily infested; if not, revert to original block
        float density = InfectionLogic.getChunkInfectionDensity(level, pos);
        if (density < 0.30f) {
            // Revert to stone when infection clears
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            return;
        }

        // Visual glitch particles
        for (int i = 0; i < 2; i++) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Client-side flicker particles
        if (random.nextFloat() < 0.6f) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.05, 0);
        }
        if (random.nextFloat() < 0.3f) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, -0.05, 0);
        }
    }
}
