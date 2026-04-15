package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Glitch Block — appears in heavily infested areas (max 10 per chunk).
 * Uses PHASE property to cycle through visual states rapidly via scheduled ticks.
 * Reverts to stone when infection density drops below 30%.
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide()) {
            // Schedule rapid ticking for visual glitching
            level.scheduleTick(pos, this, 3 + level.getRandom().nextInt(5));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Rapid phase cycling via scheduled ticks (much faster than randomTick)
        int nextPhase = (state.getValue(PHASE) + 1 + random.nextInt(4)) % 8;
        level.setBlock(pos, state.setValue(PHASE, nextPhase), 2);

        // Schedule next glitch tick (2-6 ticks = very fast flashing)
        level.scheduleTick(pos, this, 2 + random.nextInt(5));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        // Check if area is still heavily infested; if not, revert to stone
        float density = InfectionLogic.getChunkInfectionDensity(level, pos);
        if (density < 0.30f) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            return;
        }

        // Visual glitch particles
        level.sendParticles(ParticleTypes.ENCHANT,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                5, 0.4, 0.4, 0.4, 0.15);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        // Spawn block-breaking particles of random block types for visual noise
        Block[] particleBlocks = {
                Blocks.STONE, Blocks.DIAMOND_ORE, Blocks.GOLD_BLOCK,
                Blocks.REDSTONE_BLOCK, Blocks.OBSIDIAN, Blocks.NETHERRACK,
                Blocks.EMERALD_ORE, Blocks.AMETHYST_BLOCK, Blocks.SCULK
        };

        if (random.nextFloat() < 0.7f) {
            Block particleBlock = particleBlocks[random.nextInt(particleBlocks.length)];
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, particleBlock.defaultBlockState()),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.3,
                    random.nextDouble() * 0.2,
                    (random.nextDouble() - 0.5) * 0.3
            );
        }
        if (random.nextFloat() < 0.4f) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.05, 0);
        }
        if (random.nextFloat() < 0.2f) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0, -0.05, 0);
        }
    }
}
