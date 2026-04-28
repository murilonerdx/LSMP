package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Screaming Soul — gaze attack. Looking at the block within range applies
 * Darkness + Wither + Confusion. Rewards keeping eyes off it.
 */
public class ScreamingSoulBlock extends Block {

    private static final double GAZE_RANGE = 8.0;
    private static final double GAZE_THRESHOLD = 0.92; // dot product (~ ±23°)

    public ScreamingSoulBlock(Properties props) { super(props); }

    @Override
    public boolean isRandomlyTicking(BlockState s) { return true; }

    @Override
    public void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) {
        super.onPlace(s, l, p, old, moved);
        if (!l.isClientSide) l.scheduleTick(p, this, 30);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (!FleshMotherBlock.isContained(level, pos)) attack(level, pos);
        level.scheduleTick(pos, this, 30 + rand.nextInt(10));
    }

    private void attack(ServerLevel level, BlockPos pos) {
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        AABB box = new AABB(pos).inflate(GAZE_RANGE);
        boolean any = false;
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue;
            Vec3 toBlock = center.subtract(le.getEyePosition()).normalize();
            Vec3 look = le.getLookAngle().normalize();
            double dot = look.dot(toBlock);
            if (dot < GAZE_THRESHOLD) continue;
            // Eye contact established — punish
            le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            le.hurt(le.damageSources().magic(), 2.0F);
            level.sendParticles(ParticleTypes.SOUL,
                    le.getX(), le.getY() + le.getBbHeight() * 0.7, le.getZ(),
                    8, 0.2, 0.3, 0.2, 0.02);
            any = true;
        }
        if (any) {
            level.playSound(null, pos, SoundEvents.WARDEN_AGITATED, SoundSource.BLOCKS, 0.6F, 1.5F);
            level.sendParticles(ParticleTypes.SCULK_SOUL,
                    center.x, center.y + 0.4, center.z, 6, 0.2, 0.2, 0.2, 0.02);
        }
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        if (r.nextFloat() < 0.4F) {
            l.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    p.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
                    p.getY() + 0.6 + r.nextDouble() * 0.2,
                    p.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.4,
                    0, 0.02, 0);
        }
    }
}
