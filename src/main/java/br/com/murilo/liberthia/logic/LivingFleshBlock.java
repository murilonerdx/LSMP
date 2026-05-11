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
        /* DISABLED — kill switch permanente */
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) { return false; /* DISABLED */ }
}
