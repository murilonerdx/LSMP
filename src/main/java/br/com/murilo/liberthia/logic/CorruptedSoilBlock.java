package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CorruptedSoilBlock extends Block {
    private static final int LOCAL_SPREAD_ATTEMPTS = 3;
    private static final int LOCAL_SPREAD_RADIUS = 2;
    private static final int SPORE_RADIUS = 5;
    private static final int DEEP_CORRUPTION_DEPTH = 4;
    private static final int UPWARD_CORRUPTION_HEIGHT = 2;

    public CorruptedSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, pos)) {
            return;
        }

        spreadInfection(level, pos, random);
        attemptSporeLaunch(level, pos, random);
        attemptGrowthSprout(level, pos, random);

        if (random.nextFloat() < 0.12f) {
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    2,
                    0.25D, 0.10D, 0.25D,
                    0.01D
            );
        }
    }

    private void spreadInfection(ServerLevel level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < LOCAL_SPREAD_ATTEMPTS; i++) {
            if (random.nextFloat() >= 0.60F) {
                continue;
            }

            int dx = random.nextInt((LOCAL_SPREAD_RADIUS * 2) + 1) - LOCAL_SPREAD_RADIUS;
            int dz = random.nextInt((LOCAL_SPREAD_RADIUS * 2) + 1) - LOCAL_SPREAD_RADIUS;

            infectSurfaceColumnDeep(level, pos.offset(dx, 0, dz), random, false);
        }
    }

    private void attemptSporeLaunch(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= 0.04f) {
            return;
        }

        int rx = random.nextInt((SPORE_RADIUS * 2) + 1) - SPORE_RADIUS;
        int rz = random.nextInt((SPORE_RADIUS * 2) + 1) - SPORE_RADIUS;

        infectSurfaceColumnDeep(level, pos.offset(rx, 0, rz), random, true);
    }

    private void attemptGrowthSprout(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= 0.06f) {
            return;
        }

        BlockPos above = pos.above();
        if (canSproutGrowth(level, above)) {
            level.setBlockAndUpdate(above, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());

            BlockPos second = above.above();
            if (level.getBlockState(second).isAir() && level.getFluidState(second).isEmpty() && random.nextFloat() < 0.30F) {
                level.setBlockAndUpdate(second, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
            }
        }
    }

    private void infectSurfaceColumnDeep(ServerLevel level, BlockPos targetColumn, RandomSource random, boolean aggressive) {
        BlockPos groundPos = findGroundAtSurface(level, targetColumn);
        BlockState groundState = level.getBlockState(groundPos);

        if (groundState.isAir() || ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, groundPos)) {
            return;
        }

        int minY = Math.max(level.getMinBuildHeight() + 1, groundPos.getY() - DEEP_CORRUPTION_DEPTH);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, groundPos.getY() + UPWARD_CORRUPTION_HEIGHT);
        boolean changed = false;

        for (int y = groundPos.getY(); y >= minY; y--) {
            BlockPos current = new BlockPos(groundPos.getX(), y, groundPos.getZ());
            if (corruptBelowSurface(level, current, y == groundPos.getY(), aggressive)) {
                changed = true;
            }
        }

        for (int y = groundPos.getY() + 1; y <= maxY; y++) {
            BlockPos current = new BlockPos(groundPos.getX(), y, groundPos.getZ());
            if (corruptAboveSurface(level, current, aggressive)) {
                changed = true;
            }
        }

        if (changed) {
            sproutGrowthColumn(level, groundPos, random);
        }
    }

    private boolean corruptBelowSurface(ServerLevel level, BlockPos pos, boolean surfaceLayer, boolean aggressive) {
        BlockState state = level.getBlockState(pos);

        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get()) || state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)) {
            return false;
        }

        if (state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty()) {
            level.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            return true;
        }

        if (state.isAir()) {
            return false;
        }

//        if (state.isAir()) {
//            if (aggressive) {
//                level.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
//                return true;
//            }
//            return false;
//        }

        if (surfaceLayer && isSurfaceSoilBlock(state)) {
            level.setBlockAndUpdate(pos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            return true;
        }

        if (isDeepCorruptibleGround(level, pos, state)) {
            level.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            return true;
        }

        return false;
    }

    private boolean corruptAboveSurface(ServerLevel level, BlockPos pos, boolean aggressive) {
        BlockState state = level.getBlockState(pos);

        if (state.is(ModBlocks.CLEAR_MATTER_BLOCK.get()) || state.is(Blocks.BEDROCK)) {
            return false;
        }

        if (state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())) {
            return false;
        }

        if (!level.getFluidState(pos).isEmpty()) {
            level.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            return true;
        }

        if (state.isAir()) {
            return false;
        }

        if (state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || ProtectionUtils.hasRegistryPath(state, "grass")
                || ProtectionUtils.hasRegistryPath(state, "fern")
                || ProtectionUtils.hasRegistryPath(state, "vine")
                || ProtectionUtils.hasRegistryPath(state, "flower")
                || ProtectionUtils.hasRegistryPath(state, "plant")
                || ProtectionUtils.hasRegistryPath(state, "crop")) {
            if (state.getDestroySpeed(level, pos) >= 0.0F) {
                level.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
                return true;
            }
        }

        return false;
    }

    private void sproutGrowthColumn(ServerLevel level, BlockPos groundPos, RandomSource random) {
        BlockState baseState = level.getBlockState(groundPos);
        if (!baseState.is(ModBlocks.CORRUPTED_SOIL.get()) && !baseState.is(ModBlocks.DARK_MATTER_BLOCK.get())) {
            return;
        }

        BlockPos first = groundPos.above();
        if (!canSproutGrowth(level, first)) {
            return;
        }

        level.setBlockAndUpdate(first, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());

        BlockPos second = first.above();
        if (level.getBlockState(second).isAir() && level.getFluidState(second).isEmpty() && random.nextFloat() < 0.12F) {
            level.setBlockAndUpdate(second, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
        }

        BlockPos third = second.above();
        if (level.getBlockState(third).isAir() && level.getFluidState(third).isEmpty() && random.nextFloat() < 0.04F) {
            level.setBlockAndUpdate(third, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
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

    private static boolean canSproutGrowth(ServerLevel level, BlockPos growthPos) {
        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, growthPos)) {
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

        return !ProtectionUtils.hasGrowthTooClose(level, growthPos, 6);
    }

    private boolean isSurfaceSoilBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    private static boolean isDeepCorruptibleGround(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.is(ModBlocks.CORRUPTED_SOIL.get())
                || state.is(ModBlocks.DARK_MATTER_BLOCK.get())
                || state.is(ModBlocks.INFECTION_GROWTH.get())
                || state.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
            return false;
        }

        if (ProtectionUtils.isYellowMatterBlock(state) || ProtectionUtils.isClearMatterBlock(state)) {
            return false;
        }

        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }

        if (state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE)
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
                || state.is(Blocks.PACKED_MUD)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || ProtectionUtils.hasRegistryPath(state, "_ore")) {
            return true;
        }

        return state.isFaceSturdy(level, pos, Direction.UP);
    }
}