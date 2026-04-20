package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.BloodParticles;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.AABB;

/**
 * Blood Scythe — cursed sword. Sweeping AOE on hit that applies Blood Infection and
 * lifesteal 30% of damage dealt to wielder.
 */
public class BloodScytheItem extends SwordItem {
    public BloodScytheItem(Tier tier, int dmg, float speed, Properties p) {
        super(tier, dmg, speed, p);
    }

    @Override
    public boolean hurtEnemy(net.minecraft.world.item.ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide) {
            target.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 200, 1, false, true, true));
            // lifesteal
            attacker.heal(3.0F);
            if (attacker.level() instanceof ServerLevel sl) {
                sl.sendParticles(BloodParticles.BLOOD, target.getX(), target.getY() + 1.0, target.getZ(),
                        16, 0.3, 0.5, 0.3, 0.2);
            }
            // sweep AOE
            AABB box = target.getBoundingBox().inflate(2.5);
            for (Entity e : attacker.level().getEntities(attacker, box, en -> en != target && en != attacker && en instanceof LivingEntity)) {
                if (e instanceof LivingEntity le) {
                    le.hurt(le.damageSources().mobAttack(attacker), 5.0F);
                    le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 120, 0, false, true, true));
                }
            }
        }
        return r;
    }
}
