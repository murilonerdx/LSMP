package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModEffects;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hemorrhage Spike — bleed-themed pad. Walking over it once per second:
 *   • 4.0 dmg generic + Wither II + Blood Infection II + 60t bleed (Wither I).
 *   • Walkable (no collision).
 *   • {@link BloodKin} entities are immune (so kin can pass / drink the bleed).
 */
public class HemorrhageSpikeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public HemorrhageSpikeBlock(Properties props) {
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
        if (le.tickCount % 20 != 0) return;

        ServerLevel sl = (ServerLevel) level;
        le.hurt(le.damageSources().wither(), 4.0F);
        le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, true, true));
        if (ModEffects.BLOOD_INFECTION.get() != null) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 120, 1, false, true, true));
        }
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                le.getX(), le.getY() + 0.5, le.getZ(),
                10, 0.3, 0.4, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.DRIPPING_LAVA,
                le.getX(), le.getY() + 0.2, le.getZ(),
                4, 0.3, 0.1, 0.3, 0);
        sl.playSound(null, pos, SoundEvents.GENERIC_HURT, SoundSource.BLOCKS, 0.5F, 0.5F);
    }
}
