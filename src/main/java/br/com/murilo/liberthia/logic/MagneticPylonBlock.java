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
 * Magnetic Pylon — pulls living entities toward it. Within 1.5 blocks they
 * take crushing damage and Slowness V. Items are pulled too.
 */
public class MagneticPylonBlock extends Block {

    public MagneticPylonBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean isRandomlyTicking(BlockState s) {
        return true;
    }

    @Override
    public void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) {
        super.onPlace(s, l, p, old, moved);
        if (!l.isClientSide) l.scheduleTick(p, this, 10);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
//        if (!FleshMotherBlock.isContained(level, pos)) pull(level, pos);
        pull(level, pos);
        level.scheduleTick(pos, this, 10);
    }

    private void pull(ServerLevel level, BlockPos pos) {
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        AABB box = new AABB(pos).inflate(8.0);

        // Pull living entities (skip kin + creative)
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le instanceof Player p && p.isCreative()) continue;
            if (BloodKin.is(le)) continue;
            Vec3 toCenter = center.subtract(le.position());
            double dist = toCenter.length();
            if (dist < 0.001) continue;
            double pullStrength = Math.min(0.10, 0.5 / (dist + 1.0));
            Vec3 pull = toCenter.normalize().scale(pullStrength);
            le.setDeltaMovement(le.getDeltaMovement().add(pull));
            le.hurtMarked = true;
            if (dist <= 1.6) {
                le.hurt(le.damageSources().magic(), 1.5F);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
            }
        }

        // Pull dropped items
        for (net.minecraft.world.entity.item.ItemEntity it
                : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            Vec3 toCenter = center.subtract(it.position());
            double dist = toCenter.length();
            if (dist < 0.5) continue;
            it.setDeltaMovement(it.getDeltaMovement().add(toCenter.normalize().scale(0.2)));
            it.hurtMarked = true;
        }

        if (level.getGameTime() % 20L == 0L) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.3F, 0.5F);
        }
        // Spiral particles
        double t = (level.getGameTime() % 40L) / 40.0 * Math.PI * 2;
        for (int i = 0; i < 4; i++) {
            double a = t + i * (Math.PI / 2);
            double r = 1.5;
            level.sendParticles(ParticleTypes.PORTAL,
                    center.x + Math.cos(a) * r,
                    center.y + 0.3,
                    center.z + Math.sin(a) * r,
                    1, 0.0, 0.5, 0.0, 0.05);
        }
    }
}
