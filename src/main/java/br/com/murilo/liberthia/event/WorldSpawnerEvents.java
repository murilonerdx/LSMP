package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModFluids;
import br.com.murilo.liberthia.registry.ModSounds;
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

        placeCluster(serverLevel, top);
        serverLevel.playSound(null, top, ModSounds.DARK_PULSE.get(), SoundSource.BLOCKS, 0.8F, 0.75F);
    }

    private void placeCluster(ServerLevel level, BlockPos center) {
        BlockPos base = center.below();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos floor = base.offset(x, 0, z);
                if (level.isEmptyBlock(floor.above())) {
                    level.setBlockAndUpdate(floor, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
                }
            }
        }

        BlockState fluidState = ModFluids.DARK_MATTER.get().defaultFluidState().createLegacyBlock();
        if (level.isEmptyBlock(base.above())) {
            level.setBlockAndUpdate(base.above(), fluidState);
        }
    }
}
