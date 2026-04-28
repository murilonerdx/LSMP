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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/**
 * Thorn Briar — small spike-bush. Damages every LivingEntity that touches its
 * hitbox (5 dmg + Wither II 60t + small Bleed/BloodInfection if effect exists).
 * Inspired by EvilCraft's BlockSpikedPlate but per-entity contact (no signal).
 */
public class ThornBriarBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.55, 1.0);

    public ThornBriarBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // Walkable but slows + damages
        return Shapes.empty();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof LivingEntity le)) return;
        if (le instanceof Player p && p.isCreative()) return;
        if (BloodKin.is(le)) return;
        // Throttle: only damage every 10 ticks per entity to avoid spam
        if (le.tickCount % 10 != 0) return;

        ServerLevel sl = (ServerLevel) level;
        le.hurt(le.damageSources().cactus(), 4.0F);
        le.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, false, true, true));
        if (ModEffects.BLOOD_INFECTION != null && ModEffects.BLOOD_INFECTION.get() != null) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 100, 0, false, true, true));
        }
        sl.sendParticles(ParticleTypes.CRIT,
                le.getX(), le.getY() + 0.4, le.getZ(),
                6, 0.2, 0.3, 0.2, 0.05);
        sl.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.BLOCKS, 0.6F, 0.7F);
    }
}
