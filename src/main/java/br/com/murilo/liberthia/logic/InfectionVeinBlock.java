package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

public class InfectionVeinBlock extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public InfectionVeinBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, pos)) {
            return;
        }

        int age = state.getValue(AGE);

        // F2: Count connected veins for network boost
        int connectedVeins = 0;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(this)) {
                connectedVeins++;
            }
        }

        // Grow age
        if (age < 3 && random.nextFloat() < 0.15f) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
            age = age + 1;
        }

        // Spread scales with connected veins
        float spreadChance = 0.20f * (1.0f + connectedVeins * 0.25f);
        if (pos.getY() < 50 && random.nextFloat() < spreadChance) {
            spreadUnderground(level, pos, random);
        }

        // At age 3: 10% chance to convert self to dark_matter_block
        if (age >= 3 && random.nextFloat() < 0.10f) {
            level.setBlock(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState(), 3);
            return;
        }

        // F2: 4+ connected veins = apply Slowness to nearby entities
        if (connectedVeins >= 4 && random.nextFloat() < 0.15f) {
            AABB area = new AABB(pos).inflate(4);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
            }
        }

        // Particles scale with connected veins
        int particleCount = 2 + connectedVeins / 2;
        if (random.nextFloat() < 0.15f + connectedVeins * 0.05f) {
            level.sendParticles(
                    ParticleTypes.SQUID_INK,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    particleCount, 0.2D, 0.2D, 0.2D, 0.01D
            );
        }
        if (random.nextFloat() < 0.10f) {
            level.sendParticles(
                    ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    1, 0.15D, 0.15D, 0.15D, 0.005D
            );
        }
    }

    private void spreadUnderground(ServerLevel level, BlockPos pos, RandomSource random) {
        Direction direction = Direction.getRandom(random);
        BlockPos targetPos = pos.relative(direction);

        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, targetPos)) {
            return;
        }

        BlockState targetState = level.getBlockState(targetPos);

        // Only spread to stone-type blocks
        if (isStoneForVein(targetState)) {
            // Check that at least one neighbor of the target is stone (vein grows along surfaces)
            boolean hasStoneNeighbor = false;
            for (Direction dir : Direction.values()) {
                BlockState neighborState = level.getBlockState(targetPos.relative(dir));
                if (isStoneForVein(neighborState) || neighborState.is(this)) {
                    hasStoneNeighbor = true;
                    break;
                }
            }
            if (hasStoneNeighbor) {
                level.setBlockAndUpdate(targetPos, this.defaultBlockState());
            }
        }
    }

    private static boolean isStoneForVein(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        if (random.nextFloat() < 0.3f) {
            level.addParticle(
                    ParticleTypes.SQUID_INK,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.02D, 0.0D
            );
        }
    }
}
