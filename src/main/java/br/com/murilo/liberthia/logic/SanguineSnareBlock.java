package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sanguine Snare — entity-trap pad. Walkable. Stepping onto it:
 *   • Slowness V for 5s.
 *   • Mining Fatigue III for 5s.
 *   • Vertical motion zeroed each tick (entity gets pinned in place like cobweb).
 *   • Occasional twitch particles.
 *
 * {@link BloodKin} entities are unaffected so kin can patrol over it.
 */
public class SanguineSnareBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public SanguineSnareBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof LivingEntity le)) return;
        if (BloodKin.is(le)) return;

        // Pin in place: zero horizontal motion every tick + tiny downward pull.
        Vec3 m = le.getDeltaMovement();
        le.setDeltaMovement(m.x * 0.18, Math.min(m.y, 0), m.z * 0.18);

        if (le.tickCount % 20 == 0) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, true, true));
            le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2, false, true, true));
            ((ServerLevel) level).sendParticles(ParticleTypes.SQUID_INK,
                    le.getX(), le.getY() + 0.2, le.getZ(),
                    6, 0.3, 0.2, 0.3, 0.02);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.4F, 0.4F);
        }
    }
}
