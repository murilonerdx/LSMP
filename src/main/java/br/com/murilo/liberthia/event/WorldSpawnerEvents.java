package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.logic.MatterHistoryManager;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModFluids;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class WorldSpawnerEvents {
    private static final int MIN_GROWTH_SPACING_BLOCKS = 8;

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return;
        if (!LiberthiaConfig.SERVER.worldSpawnsEnabled.get()) return;

        int interval = LiberthiaConfig.SERVER.spawnIntervalTicks.get();
        if (interval <= 0 || serverLevel.getGameTime() % interval != 0L) return;

        // Avalia anomalias mesmo sem players online.
        InfectionLogic.evaluateDarkMatterRegion(serverLevel, pickAnomalyCenter(serverLevel));

        trySpawnNearRandomPlayer(serverLevel);
    }

    private void trySpawnNearRandomPlayer(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty() || level.random.nextFloat() > 0.15F) return;

        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        BlockPos center = player.blockPosition().offset(level.random.nextInt(33) - 16, 0, level.random.nextInt(33) - 16);
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);

        if (top.getY() < level.getMinBuildHeight() + 2 || top.getY() >= level.getMaxBuildHeight() - 2) return;

        placeScatteredGrowth(level, top);
        level.playSound(null, top, ModSounds.DARK_PULSE.get(), SoundSource.BLOCKS, 0.8F, 0.75F);
        List<ServerPlayer> players = serverLevel.players();
        if (players.isEmpty() || serverLevel.random.nextFloat() > 0.15F) return;

        ServerPlayer player = players.get(serverLevel.random.nextInt(players.size()));
        BlockPos center = player.blockPosition().offset(serverLevel.random.nextInt(33) - 16, 0, serverLevel.random.nextInt(33) - 16);
        BlockPos top = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);

        if (top.getY() < serverLevel.getMinBuildHeight() + 2 || top.getY() >= serverLevel.getMaxBuildHeight() - 2) return;

        placeScatteredGrowth(serverLevel, top);
        serverLevel.playSound(null, top, ModSounds.DARK_PULSE.get(), SoundSource.BLOCKS, 0.8F, 0.75F);
    }

    private BlockPos pickAnomalyCenter(ServerLevel level) {
        if (!level.players().isEmpty()) {
            ServerPlayer pivot = level.players().get(level.random.nextInt(level.players().size()));
            return pivot.blockPosition();
        }
        BlockPos spawn = level.getSharedSpawnPos();
        return spawn.offset(level.random.nextInt(1025) - 512, 0, level.random.nextInt(1025) - 512);
    }

    private void placeScatteredGrowth(ServerLevel level, BlockPos center) {
        List<BlockPos> placed = new ArrayList<>();
        int desired = 3 + level.random.nextInt(3);
        int attempts = 0;

        while (placed.size() < desired && attempts++ < 40) {
            int distance = 16 + level.random.nextInt(17);
            double angle = level.random.nextDouble() * Math.PI * 2.0D;

            BlockPos candidate = center.offset(
                    (int) Math.round(Math.cos(angle) * distance),
                    0,
                    (int) Math.round(Math.sin(angle) * distance)
            );


    private void placeScatteredGrowth(ServerLevel level, BlockPos center) {
        List<BlockPos> placed = new ArrayList<>();
        int desired = 3 + level.random.nextInt(3);
        int attempts = 0;

        while (placed.size() < desired && attempts++ < 40) {
            int distance = 16 + level.random.nextInt(17);
            double angle = level.random.nextDouble() * Math.PI * 2.0D;

            BlockPos candidate = center.offset(
                    (int) Math.round(Math.cos(angle) * distance),
                    0,
                    (int) Math.round(Math.sin(angle) * distance)
            );

            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            BlockPos base = top.below();

            if (top.getY() <= level.getMinBuildHeight() + 2 || top.getY() >= level.getMaxBuildHeight() - 2) continue;
            if (!isFarEnough(base, placed, MIN_GROWTH_SPACING_BLOCKS)) continue;
            if (hasNearbyDarkMatter(level, base, 10)) continue;
            if (hasNearbyGrowthTree(level, base, MIN_GROWTH_SPACING_BLOCKS)) continue;

            if (placeSingleGrowth(level, base)) {
                placed.add(base.immutable());
            }
            if (!isFarEnough(base, placed, MIN_GROWTH_SPACING_BLOCKS)) continue;
            if (hasNearbyDarkMatter(level, base, 10)) continue;
            if (hasNearbyGrowthTree(level, base, MIN_GROWTH_SPACING_BLOCKS)) continue;

            placeSingleGrowth(level, base);
            placed.add(base.immutable());
        }
    }

    private boolean isFarEnough(BlockPos candidate, List<BlockPos> placed, double minDistance) {
        for (BlockPos existing : placed) {
            if (existing.distSqr(candidate) < (minDistance * minDistance)) return false;
        }
        return true;
    }

    private boolean hasNearbyGrowthTree(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 4, radius))) {
            if (level.getBlockState(pos).is(ModBlocks.INFECTION_GROWTH.get())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNearbyDarkMatter(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (level.getBlockState(pos).is(ModBlocks.DARK_MATTER_BLOCK.get())) return true;
        }
        return false;
    }

    private boolean placeSingleGrowth(ServerLevel level, BlockPos base) {
        if (isHydroBlocked(level, base)) return false;
        if (hasNearbyGrowthTree(level, base, MIN_GROWTH_SPACING_BLOCKS)) return false;

        BlockState previous = level.getBlockState(base);
        if (!previous.isSolidRender(level, base)) return false;
        if (!level.isEmptyBlock(base.above())) return false;

        MatterHistoryManager.recordOriginalBlock(level, base, previous);
        level.setBlockAndUpdate(base, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
        infectGroundAroundTree(level, base, 2);

        int trunkHeight = 1 + level.random.nextInt(3); // até 3 blocos verticais
        BlockPos top = base;
        for (int i = 0; i < trunkHeight; i++) {
            BlockPos trunkPos = base.above(i + 1);
            if (!level.isEmptyBlock(trunkPos)) break;
            if (hasAdjacentGrowth(level, trunkPos)) break;
            level.setBlockAndUpdate(trunkPos, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
            top = trunkPos;
        }

        BlockState fluidState = ModFluids.DARK_MATTER.get().defaultFluidState().createLegacyBlock();
        if (level.isEmptyBlock(top.above()) && !isHydroBlocked(level, top.above())) {
            level.setBlockAndUpdate(top.above(), fluidState);
        }
        return true;
    }

    private boolean hasAdjacentGrowth(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.north()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.south()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.east()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.west()).is(ModBlocks.INFECTION_GROWTH.get());
    }

    private void infectGroundAroundTree(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 0, radius))) {
            if (isHydroBlocked(level, pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                    || state.is(net.minecraft.world.level.block.Blocks.DIRT)
                    || state.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                    || state.is(net.minecraft.world.level.block.Blocks.PODZOL)
                    || state.is(net.minecraft.world.level.block.Blocks.MYCELIUM)
                    || state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)
                    || state.is(net.minecraft.world.level.block.Blocks.MUD)) {
                MatterHistoryManager.recordOriginalBlock(level, pos, state);
                level.setBlockAndUpdate(pos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            }
        }
    }

    private boolean isHydroBlocked(ServerLevel level, BlockPos pos) {
        for (BlockPos scan : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            FluidState fluid = level.getFluidState(scan);
            if (fluid.is(net.minecraft.tags.FluidTags.WATER) || fluid.is(net.minecraft.tags.FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
        }
        return false;
    }

    private boolean hasAdjacentGrowth(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.north()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.south()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.east()).is(ModBlocks.INFECTION_GROWTH.get())
                || level.getBlockState(pos.west()).is(ModBlocks.INFECTION_GROWTH.get());
    }
}
