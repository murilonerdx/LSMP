package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/** Lightning Grenade — summons a real lightning bolt at impact + Slowness V stun. */
public class LightningGrenadeEntity extends ThrowableItemProjectile {

    public LightningGrenadeEntity(EntityType<? extends LightningGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public LightningGrenadeEntity(Level level, LivingEntity shooter) {
        super(ModEntities.LIGHTNING_GRENADE.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() { return ModItems.LIGHTNING_GRENADE.get(); }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(getX(), getY(), getZ());
            sl.addFreshEntity(bolt);
        }

        // Stun anything close that survived the bolt
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(4.0))) {
            if (BloodKin.is(le)) continue;
            if (le == getOwner()) continue;
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
            le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.5, getZ(),
                30, 0.6, 0.6, 0.6, 0.2);
        sl.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        discard();
    }
}
