package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Reskinned Arrow that applies Blood Infection I on hit. Vanilla Arrow behaviour otherwise. */
public class BleedingArrowEntity extends Arrow {

    public BleedingArrowEntity(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
    }

    public BleedingArrowEntity(Level level, LivingEntity owner) {
        super(level, owner);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 200, 0));
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    8, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(Items.ARROW);
    }
}
