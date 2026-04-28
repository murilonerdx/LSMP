package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Single-target dart that scrambles the victim's senses. */
public class MindSplinterDartEntity extends ThrowableItemProjectile {

    public MindSplinterDartEntity(EntityType<? extends MindSplinterDartEntity> type, Level level) {
        super(type, level);
    }

    public MindSplinterDartEntity(Level level, LivingEntity shooter) {
        super(ModEntities.MIND_SPLINTER_DART.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.MIND_SPLINTER_DART.get(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();

        if (result.getType() == HitResult.Type.ENTITY) {
            LivingEntity target = ((EntityHitResult) result).getEntity() instanceof LivingEntity le ? le : null;
            if (target != null) {
                target.hurt(target.damageSources().thrown(this, getOwner()), 2.0F);
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 20, 2));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20 * 60, 1));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 30, 0));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 6, 0));
                sl.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0, target.getZ(),
                        24, 0.4, 0.6, 0.4, 0.05);
                sl.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.0, target.getZ(),
                        18, 0.5, 0.5, 0.5, 0.4);
            }
        }
        sl.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.5F);
        discard();
    }
}
