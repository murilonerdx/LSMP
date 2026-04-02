package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModFluids;
import br.com.murilo.liberthia.registry.ModSounds;
import br.com.murilo.liberthia.logic.InfectionLogic;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldSpawnerEvents {
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }

        if (!(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (!LiberthiaConfig.SERVER.worldSpawnsEnabled.get()) {
            return;
        }

        int interval = LiberthiaConfig.SERVER.spawnIntervalTicks.get();
        if (interval <= 0 || serverLevel.getGameTime() % interval != 0L) {
            return;
        }

        BlockPos anomalyCenter;
        if (!serverLevel.players().isEmpty()) {
            ServerPlayer pivot = serverLevel.players().get(serverLevel.random.nextInt(serverLevel.players().size()));
            anomalyCenter = pivot.blockPosition();
        } else {
            BlockPos spawn = serverLevel.getSharedSpawnPos();
            anomalyCenter = spawn.offset(serverLevel.random.nextInt(1025) - 512, 0, serverLevel.random.nextInt(1025) - 512);
        }
        InfectionLogic.evaluateDarkMatterRegion(serverLevel, anomalyCenter);

        List<ServerPlayer> players = serverLevel.players();
        if (players.isEmpty() || serverLevel.random.nextFloat() > 0.15F) {
            return;
        }

        ServerPlayer player = players.get(serverLevel.random.nextInt(players.size()));
        BlockPos center = player.blockPosition().offset(serverLevel.random.nextInt(33) - 16, 0, serverLevel.random.nextInt(33) - 16);
        BlockPos top = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);

        if (top.getY() < serverLevel.getMinBuildHeight() + 2 || top.getY() >= serverLevel.getMaxBuildHeight() - 2) {
            return;
        }

        placeScatteredGrowth(serverLevel, top);
        serverLevel.playSound(null, top, ModSounds.DARK_PULSE.get(), SoundSource.BLOCKS, 0.8F, 0.75F);
    }

    private void placeScatteredGrowth(ServerLevel level, BlockPos center) {
        List<BlockPos> placed = new ArrayList<>();
        int desired = 3 + level.random.nextInt(3);
        int attempts = 0;

        while (placed.size() < desired && attempts < 40) {
            attempts++;
            int distance = 16 + level.random.nextInt(17);
            double angle = level.random.nextDouble() * Math.PI * 2.0D;

            BlockPos candidate = center.offset(
                    (int) Math.round(Math.cos(angle) * distance),
                    0,
                    (int) Math.round(Math.sin(angle) * distance)
            );
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
            BlockPos base = top.below();

            if (top.getY() <= level.getMinBuildHeight() + 2 || top.getY() >= level.getMaxBuildHeight() - 2) {
                continue;
            }
            if (!isFarEnoughFromOthers(base, placed, 16.0D)) {
                continue;
            }
            if (hasNearbyDarkMatter(level, base, 10)) {
                continue;
            }

            placeSingleGrowth(level, base);
            placed.add(base.immutable());
        }
    }

    private boolean isFarEnoughFromOthers(BlockPos candidate, List<BlockPos> placed, double minDistance) {
        for (BlockPos existing : placed) {
            if (existing.distSqr(candidate) < (minDistance * minDistance)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNearbyDarkMatter(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (level.getBlockState(pos).is(ModBlocks.DARK_MATTER_BLOCK.get())) {
                return true;
            }
        }
        return false;
    }

    private void placeSingleGrowth(ServerLevel level, BlockPos base) {
        BlockState previous = level.getBlockState(base);
        if (!level.getBlockState(base).isSolidRender(level, base)) {
            return;
        }
        if (!level.isEmptyBlock(base.above())) {
            return;
        }

        br.com.murilo.liberthia.logic.MatterHistoryManager.recordOriginalBlock(level, base, previous);
        level.setBlockAndUpdate(base, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
        int trunkHeight = 1 + level.random.nextInt(2);
        BlockPos top = base;
        for (int i = 0; i < trunkHeight; i++) {
            BlockPos trunkPos = base.above(i + 1);
            if (!level.isEmptyBlock(trunkPos)) {
                break;
            }
            level.setBlockAndUpdate(trunkPos, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
            top = trunkPos;
        }

        for (BlockPos canopyPos : BlockPos.betweenClosed(top.offset(-1, 0, -1), top.offset(1, 1, 1))) {
            if (level.random.nextFloat() < 0.45F && level.isEmptyBlock(canopyPos)) {
                level.setBlockAndUpdate(canopyPos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            }
        }
        return true;
    }

    private boolean hasNearbyDarkMatter(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (level.getBlockState(pos).is(ModBlocks.DARK_MATTER_BLOCK.get())) {
                return true;
            }
        }
        return false;
    }

    private void placeSingleGrowth(ServerLevel level, BlockPos base) {
        BlockState previous = level.getBlockState(base);
        if (!level.getBlockState(base).isSolidRender(level, base)) {
            return;
        }
        if (!level.isEmptyBlock(base.above())) {
            return;
        }

        br.com.murilo.liberthia.logic.MatterHistoryManager.recordOriginalBlock(level, base, previous);
        level.setBlockAndUpdate(base, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
        int trunkHeight = 1 + level.random.nextInt(2);
        BlockPos top = base;
        for (int i = 0; i < trunkHeight; i++) {
            BlockPos trunkPos = base.above(i + 1);
            if (!level.isEmptyBlock(trunkPos)) {
                break;
            }
            level.setBlockAndUpdate(trunkPos, ModBlocks.INFECTION_GROWTH.get().defaultBlockState());
            top = trunkPos;
        }

        for (BlockPos canopyPos : BlockPos.betweenClosed(top.offset(-1, 0, -1), top.offset(1, 1, 1))) {
            if (level.random.nextFloat() < 0.45F && level.isEmptyBlock(canopyPos)) {
                level.setBlockAndUpdate(canopyPos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
            }
        }
        return true;
    }

    private boolean hasNearbyDarkMatter(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (level.getBlockState(pos).is(ModBlocks.DARK_MATTER_BLOCK.get())) {
                return true;
            }
        }
        return false;
    }

    private void placeSingleGrowth(ServerLevel level, BlockPos base) {
        if (!level.getBlockState(base).isSolidRender(level, base)) {
            return;
        }
        if (!level.isEmptyBlock(base.above())) {
            return;
        }

        level.setBlockAndUpdate(base, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
        BlockState fluidState = ModFluids.DARK_MATTER.get().defaultFluidState().createLegacyBlock();
        if (level.isEmptyBlock(top.above())) {
            level.setBlockAndUpdate(top.above(), fluidState);
        }
    }
}
