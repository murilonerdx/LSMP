package br.com.murilo.liberthia.entity.ai;

import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Goal that periodically picks a random debuff and casts it at the mob's
 * current target (line-of-sight not required — mob waves arms, target gets
 * effect from a distance).
 *
 * <p>Casts every {@code [minCooldown, maxCooldown]} ticks. Range cap 16.
 * Adds visible cast particles + sound to telegraph the spell.
 */
public class RandomMagicCastGoal extends Goal {
    private static final List<Supplier<MobEffect>> SPELL_POOL = List.of(
            () -> MobEffects.MOVEMENT_SLOWDOWN,
            () -> MobEffects.WEAKNESS,
            () -> MobEffects.WITHER,
            () -> MobEffects.BLINDNESS,
            () -> MobEffects.HUNGER,
            () -> MobEffects.CONFUSION,
            () -> MobEffects.DIG_SLOWDOWN,
            () -> ModEffects.BLOOD_INFECTION.get()
    );

    private final Mob mob;
    private final int minCooldown;
    private final int maxCooldown;
    private final double range;
    private int cooldown;

    public RandomMagicCastGoal(Mob mob, int minCooldown, int maxCooldown) {
        this(mob, minCooldown, maxCooldown, 16.0);
    }

    public RandomMagicCastGoal(Mob mob, int minCooldown, int maxCooldown, double range) {
        this.mob = mob;
        this.minCooldown = minCooldown;
        this.maxCooldown = maxCooldown;
        this.range = range;
        this.cooldown = mob.getRandom().nextInt(maxCooldown);
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive() && t.distanceToSqr(mob) <= range * range;
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void tick() {
        if (--cooldown > 0) return;
        cooldown = minCooldown + mob.getRandom().nextInt(Math.max(1, maxCooldown - minCooldown));

        LivingEntity target = mob.getTarget();
        if (target == null || BloodKin.is(target)) return;

        // Pick a random spell.
        Supplier<MobEffect> sup = SPELL_POOL.get(mob.getRandom().nextInt(SPELL_POOL.size()));
        MobEffect effect = sup.get();
        if (effect == null) return;
        int duration = 80 + mob.getRandom().nextInt(120); // 4-10s
        int amplifier = mob.getRandom().nextInt(2);       // 0 or 1
        target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));

        // Telegraph: particles on caster + zap line + sound.
        if (mob.level() instanceof ServerLevel sl) {
            // Caster aura
            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05);
            // Cast line trail
            double dx = target.getX() - mob.getX();
            double dy = target.getY(0.5) - (mob.getY() + 1.0);
            double dz = target.getZ() - mob.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int steps = (int) Math.max(1, len);
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                sl.sendParticles(ParticleTypes.WITCH,
                        mob.getX() + dx * t,
                        mob.getY() + 1.0 + dy * t,
                        mob.getZ() + dz * t,
                        1, 0, 0, 0, 0);
            }
            // Land particles
            sl.sendParticles(ParticleTypes.SQUID_INK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05);
            sl.playSound(null, mob.blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.6F, 1.4F);
        }

        // Mob does a small "cast" raise of arms (look at target).
        mob.getLookControl().setLookAt(target, 30F, 30F);
    }
}
