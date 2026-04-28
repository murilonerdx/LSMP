package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Attacking Flesh — aggressive flesh that bites nearby players.
 * Contained by 4+ chalk symbols nearby (stops attacks).
 */
public class AttackingFleshBlock extends Block {
    public AttackingFleshBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        attackNearby(level, pos, rand);
        // Reschedule every 20-30 ticks for steady aggression
        level.scheduleTick(pos, this, 20 + rand.nextInt(10));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        attackNearby(level, pos, rand);
    }

    private void attackNearby(ServerLevel level, BlockPos pos, RandomSource rand) {
        if (FleshMotherBlock.isContained(level, pos)) return;
        AABB box = new AABB(pos).inflate(4.0);
        Vec3 origin = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue; // don't cospe em nossos próprios bichos
            // Blood-stream trail from block → victim (arc with slight gravity droop)
            Vec3 target = new Vec3(le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ());
            Vec3 delta = target.subtract(origin);
            int steps = 8; // was 14
            for (int s = 1; s <= steps; s++) {
                double t = s / (double) steps;
                double arc = -0.35 * (t - 0.5) * (t - 0.5) + 0.09;
                double x = origin.x + delta.x * t;
                double y = origin.y + delta.y * t + arc;
                double z = origin.z + delta.z * t;
                level.sendParticles(BloodParticles.BLOOD, x, y, z,
                        1, 0.05, 0.05, 0.05, 0.0);
            }
            // Muzzle burst at the block
            level.sendParticles(BloodParticles.BLOOD,
                    origin.x, origin.y, origin.z,
                    4, 0.2, 0.1, 0.2, 0.15);
            // Impact splatter on the victim
            level.sendParticles(BloodParticles.BLOOD,
                    le.getX(), le.getY() + 0.5, le.getZ(),
                    8, 0.3, 0.5, 0.3, 0.2);
            // Damage + infection
            le.hurt(le.damageSources().magic(), 2.5F);
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 200, 0, false, true, true));
            level.playSound(null, pos, SoundEvents.SLIME_ATTACK, SoundSource.BLOCKS, 0.7F, 0.6F);
            level.playSound(null, le.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6F, 0.9F);
            break;
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        for (int i = 0; i < 3; i++) {
            double px = pos.getX() + rand.nextDouble();
            double py = pos.getY() + 1.0 + rand.nextDouble() * 0.3;
            double pz = pos.getZ() + rand.nextDouble();
            level.addParticle(BloodParticles.BLOOD, px, py, pz,
                    (rand.nextDouble() - 0.5) * 0.2, 0.1, (rand.nextDouble() - 0.5) * 0.2);
        }
    }
}
