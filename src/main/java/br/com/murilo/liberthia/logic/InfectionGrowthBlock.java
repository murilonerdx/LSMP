package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.common.Tags;

public class InfectionGrowthBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    private static final int MAX_VERTICAL_STACK = 3;
    private static final int HORIZONTAL_MIN_RADIUS = 2;
    private static final int HORIZONTAL_MAX_RADIUS = 4;

    public InfectionGrowthBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 2400 + level.random.nextInt(2400));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        if (isSpreadBlockedByProtectiveBlocks(level, pos)) {
            return;
        }

        float density = InfectionLogic.getChunkInfectionDensity(level, pos);
        int age = state.getValue(AGE);

        if (age >= 3 && density >= 0.75f && countColumnHeight(level, pos) >= 3) {
            level.setBlock(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState(), 3);
            return;
        }

        level.scheduleTick(pos, this, 2400 + random.nextInt(2400));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isSpreadBlockedByProtectiveBlocks(level, pos)) {
            return;
        }

        float density = InfectionLogic.getChunkInfectionDensity(level, pos);
        int attempts = 1 + (int) (density * 2.0f);

        BlockState currentState = state;
        int age = state.getValue(AGE);

        if (age < 3 && random.nextFloat() < (0.12f + (density * 0.10f))) {
            currentState = state.setValue(AGE, age + 1);
            level.setBlock(pos, currentState, 3);
            age = currentState.getValue(AGE);
        }

        spreadHorizontally(level, pos, random, attempts, density);
        growUpwards(level, pos, random, age, density);
        attemptSporeLaunch(level, pos, random, density);

        if (random.nextFloat() < (0.18f + (density * 0.25f))) {
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                    3,
                    0.35D, 0.25D, 0.35D,
                    0.03D
            );
        }

        if (random.nextFloat() < (density * 0.12f)) {
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    2,
                    0.20D, 0.10D, 0.20D,
                    0.01D
            );
        }
    }

    private void spreadHorizontally(ServerLevel level, BlockPos pos, RandomSource random, int attempts, float density) {
        for (int i = 0; i < attempts; i++) {
            int distance = HORIZONTAL_MIN_RADIUS + random.nextInt((HORIZONTAL_MAX_RADIUS - HORIZONTAL_MIN_RADIUS) + 1);
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);

            BlockPos targetColumn = pos.relative(direction, distance);
            BlockPos groundPos = findGroundAtSurface(level, targetColumn);
            BlockState groundState = level.getBlockState(groundPos);

            if (isSpreadBlockedByProtectiveBlocks(level, groundPos)) {
                continue;
            }

            if (isCorruptibleGround(level, groundPos, groundState)) {
                level.setBlockAndUpdate(groundPos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
                continue;
            }

            if ((groundState.is(ModBlocks.CORRUPTED_SOIL.get()) || groundState.is(ModBlocks.DARK_MATTER_BLOCK.get()))
                    && canPlaceNewGrowthColumn(level, groundPos.above())) {
                level.setBlockAndUpdate(groundPos.above(), this.defaultBlockState());
            }
        }

        if (density >= 0.70f && random.nextFloat() < 0.20f) {
            BlockPos near = findGroundAtSurface(level, pos.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3));
            BlockState nearState = level.getBlockState(near);

            if (!isSpreadBlockedByProtectiveBlocks(level, near) && isCorruptibleGround(level, near, nearState)) {
                level.setBlockAndUpdate(near, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            }
        }
    }

    private void growUpwards(ServerLevel level, BlockPos pos, RandomSource random, int age, float density) {
        if (age < 1) {
            return;
        }

        if (random.nextFloat() >= (0.06f + (density * 0.08f))) {
            return;
        }

        int currentHeight = countColumnHeight(level, pos);
        if (currentHeight >= MAX_VERTICAL_STACK) {
            return;
        }

        BlockPos above = pos.above();
        if (isSpreadBlockedByProtectiveBlocks(level, above)) {
            return;
        }

        if (!level.getBlockState(above).isAir()) {
            return;
        }

        if (!level.getFluidState(above).isEmpty()) {
            return;
        }

        level.setBlockAndUpdate(above, this.defaultBlockState());
    }

    private void attemptSporeLaunch(ServerLevel level, BlockPos pos, RandomSource random, float density) {
        float sporeChance = 0.03f + (density * 0.05f);
        if (random.nextFloat() >= sporeChance) {
            return;
        }

        int radius = 4 + random.nextInt(3);
        int rx = random.nextInt((radius * 2) + 1) - radius;
        int rz = random.nextInt((radius * 2) + 1) - radius;

        BlockPos target = pos.offset(rx, 0, rz);
        BlockPos groundPos = findGroundAtSurface(level, target);
        BlockState groundState = level.getBlockState(groundPos);

        if (isSpreadBlockedByProtectiveBlocks(level, groundPos)) {
            return;
        }

        if (isCorruptibleGround(level, groundPos, groundState)) {
            level.setBlockAndUpdate(groundPos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            return;
        }

        if ((groundState.is(ModBlocks.CORRUPTED_SOIL.get()) || groundState.is(ModBlocks.DARK_MATTER_BLOCK.get()))
                && canPlaceNewGrowthColumn(level, groundPos.above())) {
            level.setBlockAndUpdate(groundPos.above(), this.defaultBlockState());
        }
    }

    private static BlockPos findGroundAtSurface(ServerLevel level, BlockPos target) {
        BlockPos surface = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                target
        );

        BlockPos ground = surface.below();
        BlockState groundState = level.getBlockState(ground);

        if (groundState.isAir()) {
            return target;
        }

        return ground;
    }

    private boolean canPlaceNewGrowthColumn(ServerLevel level, BlockPos growthPos) {
        if (isSpreadBlockedByProtectiveBlocks(level, growthPos)) {
            return false;
        }

        if (!level.getBlockState(growthPos).isAir()) {
            return false;
        }

        if (!level.getFluidState(growthPos).isEmpty()) {
            return false;
        }

        BlockPos below = growthPos.below();
        BlockState belowState = level.getBlockState(below);

        if (!(belowState.is(ModBlocks.CORRUPTED_SOIL.get()) || belowState.is(ModBlocks.DARK_MATTER_BLOCK.get()))) {
            return false;
        }

        return !hasGrowthTooClose(level, growthPos);
    }

    private static boolean hasGrowthTooClose(ServerLevel level, BlockPos pos) {
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (scan.equals(pos)) {
                continue;
            }
            if (level.getBlockState(scan).is(ModBlocks.INFECTION_GROWTH.get())) {
                return true;
            }
        }
        return false;
    }

    private int countColumnHeight(ServerLevel level, BlockPos pos) {
        int total = 1;

        BlockPos up = pos.above();
        while (level.getBlockState(up).is(this)) {
            total++;
            up = up.above();
        }

        BlockPos down = pos.below();
        while (level.getBlockState(down).is(this)) {
            total++;
            down = down.below();
        }

        return total;
    }

    private static boolean isCorruptibleGround(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())
                || state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
            return false;
        }

        if (isYellowMatterBlock(state) || isClearMatterBlock(state)) {
            return false;
        }

        if (!state.isFaceSturdy(level, pos, Direction.UP)) {
            return false;
        }

        if (state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Tags.Blocks.ORES)
                || hasRegistryPath(state, "_ore")
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MUDDY_MANGROVE_ROOTS)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.END_STONE)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.PACKED_MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.COARSE_DIRT)) {
            return true;
        }

        return false;
    }

    private static boolean isSpreadBlockedByProtectiveBlocks(ServerLevel level, BlockPos center) {
        if (hasYellowMatterProtection(level, center)) {
            return true;
        }
        return hasClearMatterProtection(level, center);
    }

    private static boolean hasYellowMatterProtection(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -1, -2), center.offset(2, 1, 2))) {
            if (isYellowMatterBlock(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClearMatterProtection(Level level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (isClearMatterBlock(level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClearMatterBlock(BlockState state) {
        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
            return true;
        }

        return hasRegistryPath(state, "clear_matter")
                || hasRegistryPath(state, "white_matter")
                || hasRegistryPath(state, "materia_branca");
    }

    private static boolean isYellowMatterBlock(BlockState state) {
        return hasRegistryPath(state, "yellow_matter")
                || hasRegistryPath(state, "yellowmatter")
                || hasRegistryPath(state, "materia_amarela");
    }

    private static boolean hasRegistryPath(BlockState state, String expectedFragment) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) {
            return false;
        }
        return key.getPath().contains(expectedFragment);
    }
}