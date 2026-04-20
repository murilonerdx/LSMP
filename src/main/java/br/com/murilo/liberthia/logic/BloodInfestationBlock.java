package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Blood Infestation block — teems with worms. Spawns worms when players are near,
 * and emits crawling particles constantly. Breaking it releases an angry swarm.
 * Contained by 4+ chalk symbols.
 */
public class BloodInfestationBlock extends Block {
    public BloodInfestationBlock(Properties p) { super(p); }

    @Override public boolean isRandomlyTicking(BlockState s) { return true; }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        if (FleshMotherBlock.isContained(level, pos)) return;
        if (!BloodAltarBlock.hasActiveAltarNearby(level, pos, 24)) return;
        // Only spawn if a player is within 16 blocks
        boolean playerNear = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16.0),
                p -> !p.isCreative() && !p.isSpectator()).isEmpty();
        if (!playerNear) return;
        // Cap worms nearby to avoid runaway swarms
        int cap = 4;
        int count = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Silverfish.class,
                new AABB(pos).inflate(8.0)).size();
        if (count >= cap) return;
        // Spawn 1 worm above
        for (int tries = 0; tries < 4; tries++) {
            int dx = rng.nextInt(3) - 1;
            int dz = rng.nextInt(3) - 1;
            BlockPos sp = pos.offset(dx, 1, dz);
            if (level.getBlockState(sp).isAir()) {
                EntityType<?> type = rng.nextInt(3) == 0 ? ModEntities.GORE_WORM.get() : ModEntities.FLESH_CRAWLER.get();
                var worm = type.create(level);
                if (worm != null) {
                    worm.moveTo(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, rng.nextFloat() * 360F, 0F);
                    level.addFreshEntity(worm);
                    level.sendParticles(BloodParticles.BLOOD,
                            sp.getX() + 0.5, sp.getY() + 0.2, sp.getZ() + 0.5,
                            10, 0.3, 0.1, 0.3, 0.08);
                    level.playSound(null, pos, SoundEvents.SILVERFISH_STEP, SoundSource.BLOCKS, 0.5F, 0.7F);
                    break;
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        if (rng.nextInt(4) == 0) {
            // crawling particles along surface
            double side = rng.nextDouble();
            double x = pos.getX() + side;
            double z = pos.getZ() + rng.nextDouble();
            level.addParticle(BloodParticles.BLOOD,
                    x, pos.getY() + 1.01, z,
                    (rng.nextDouble() - 0.5) * 0.2, 0, (rng.nextDouble() - 0.5) * 0.2);
        }
        if (rng.nextInt(30) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.SILVERFISH_STEP, SoundSource.BLOCKS, 0.3F, 0.8F, false);
        }
    }

    @Override
    public void destroy(net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockState state) {
        if (world instanceof ServerLevel level && !FleshMotherBlock.isContained(level, pos)) {
            // release swarm
            RandomSource rng = level.random;
            for (int i = 0; i < 2 + rng.nextInt(3); i++) {
                var type = rng.nextInt(2) == 0 ? ModEntities.GORE_WORM.get() : ModEntities.FLESH_CRAWLER.get();
                var worm = type.create(level);
                if (worm != null) {
                    worm.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            rng.nextFloat() * 360F, 0F);
                    level.addFreshEntity(worm);
                }
            }
            level.sendParticles(BloodParticles.BLOOD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    30, 0.4, 0.4, 0.4, 0.2);
        }
        super.destroy(world, pos, state);
    }
}
