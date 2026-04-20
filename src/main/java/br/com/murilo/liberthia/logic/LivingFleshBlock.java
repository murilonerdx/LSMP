package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Living Flesh — passive growth type. Pulses and damages living entities
 * that step directly on it. Grows very slowly, does NOT proliferate.
 */
public class LivingFleshBlock extends Block {
    public LivingFleshBlock(Properties props) {
        super(props);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        if (BloodKin.is(entity)) return;
        if (entity instanceof LivingEntity le && !(entity instanceof Player p && p.isCreative())) {
            if (entity.tickCount % 20 == 0) {
                le.hurt(le.damageSources().magic(), 1.0F);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        // More visible: constant red dust
        for (int i = 0; i < 2; i++) {
            if (rand.nextInt(3) == 0) {
                level.addParticle(BloodParticles.BLOOD,
                        pos.getX() + rand.nextDouble(),
                        pos.getY() + 1.02,
                        pos.getZ() + rand.nextDouble(),
                        0, 0.03, 0);
            }
        }
        if (rand.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.CRIMSON_SPORE,
                    pos.getX() + rand.nextDouble(),
                    pos.getY() + 1.05,
                    pos.getZ() + rand.nextDouble(),
                    0, 0.015, 0);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        // Pulse damage AOE
        AABB box = new AABB(pos).inflate(2.0);
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue;
            le.hurt(le.damageSources().magic(), 0.5F);
        }
        // Occasionally spawn worms from the flesh (only if altar nearby + player around)
        if (rand.nextInt(14) != 0) return;
        if (!BloodAltarBlock.hasActiveAltarNearby(level, pos, 24)) return;
        boolean playerNear = !level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(20.0),
                p -> !p.isCreative() && !p.isSpectator()).isEmpty();
        if (!playerNear) return;
        int cap = 6;
        int nearby = level.getEntitiesOfClass(Silverfish.class, new AABB(pos).inflate(14.0)).size();
        if (nearby >= cap) return;
        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir()) return;
        EntityType<?> type = rand.nextInt(3) == 0 ? ModEntities.GORE_WORM.get()
                : (rand.nextInt(2) == 0 ? ModEntities.BLOOD_WORM.get() : ModEntities.FLESH_CRAWLER.get());
        var worm = type.create(level);
        if (worm != null) {
            worm.moveTo(above.getX() + 0.5, above.getY(), above.getZ() + 0.5, rand.nextFloat() * 360F, 0F);
            level.addFreshEntity(worm);
            level.sendParticles(BloodParticles.BLOOD,
                    above.getX() + 0.5, above.getY() + 0.2, above.getZ() + 0.5,
                    10, 0.3, 0.1, 0.3, 0.08);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }
}
