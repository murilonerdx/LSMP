package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.logic.ProtectionUtils;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Coração da Infecção — pulsa, spawna mobs, destruição limpa a área.
 */
public class InfectionHeartBlockEntity extends BlockEntity {

    private int tickCounter = 0;

    public InfectionHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INFECTION_HEART.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        if (!(be instanceof InfectionHeartBlockEntity heart)) return;

        heart.tickCounter++;

        // Pulse particles every 40 ticks
        if (heart.tickCounter % 40 == 0) {
            serverLevel.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    12, 1.0, 1.0, 1.0, 0.05);
            serverLevel.sendParticles(ParticleTypes.HEART,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    2, 0.3, 0.2, 0.3, 0.01);
        }

        // Spawn CorruptedZombie every 200 ticks if player within 32 blocks
        if (heart.tickCounter % 200 == 0) {
            boolean playerNearby = !serverLevel.getEntitiesOfClass(ServerPlayer.class,
                    new AABB(pos).inflate(32)).isEmpty();
            if (playerNearby) {
                double spawnX = pos.getX() + 0.5 + (serverLevel.random.nextDouble() - 0.5) * 8;
                double spawnZ = pos.getZ() + 0.5 + (serverLevel.random.nextDouble() - 0.5) * 8;
                var zombie = ModEntities.CORRUPTED_ZOMBIE.get().create(serverLevel);
                if (zombie != null) {
                    zombie.moveTo(spawnX, pos.getY() + 1, spawnZ, serverLevel.random.nextFloat() * 360, 0);
                    zombie.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos),
                            MobSpawnType.EVENT, null, null);
                    serverLevel.addFreshEntity(zombie);
                }
            }
        }
    }

    /**
     * Called when the heart block is destroyed — purges infection in 16-block radius.
     */
    public static void purgeArea(ServerLevel level, BlockPos center) {
        int radius = 16;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) > radius * radius) continue;
            ProtectionUtils.replaceInfectionWithScar(level, pos);
        }

        // Big purge particle effect
        level.sendParticles(ParticleTypes.END_ROD,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                100, radius * 0.5, radius * 0.5, radius * 0.5, 0.1);
    }
}
