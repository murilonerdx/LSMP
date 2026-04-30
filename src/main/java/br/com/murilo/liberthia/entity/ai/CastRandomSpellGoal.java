package br.com.murilo.liberthia.entity.ai;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Picks a "spell" from a small kit at random based on situation:
 *
 * <ul>
 *   <li>WEAKEN → applies Weakness II + Slowness I (close range).</li>
 *   <li>BLEED → applies Wither I + Blood Infection (medium range).</li>
 *   <li>DAZE → applies Confusion + Darkness (far range).</li>
 *   <li>BURST → quick AoE damage tick (any range).</li>
 * </ul>
 *
 * <p>Cooldown scales with mob HP — wounded mobs cast more.
 */
public class CastRandomSpellGoal extends Goal {

    private final Mob mob;
    private final double range;
    private int cooldown;

    public CastRandomSpellGoal(Mob mob, double range) {
        this.mob = mob;
        this.range = range;
        this.cooldown = 80 + mob.getRandom().nextInt(80);
        this.setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) return false;
        LivingEntity tgt = mob.getTarget();
        if (tgt == null || !tgt.isAlive()) return false;
        return mob.distanceToSqr(tgt) <= range * range;
    }

    @Override
    public void start() {
        // Reset cooldown — wounded mobs cast more often.
        float hp = mob.getHealth() / mob.getMaxHealth();
        int base = 100;
        int variance = 60;
        // Below 30% HP: half the cooldown.
        if (hp < 0.3F) base = 60;
        cooldown = base + mob.getRandom().nextInt(variance);

        LivingEntity tgt = mob.getTarget();
        if (tgt == null || !(mob.level() instanceof ServerLevel sl)) return;

        // Choose spell weighted by distance.
        double dist = mob.distanceTo(tgt);
        Spell spell;
        if (dist < 4.0) spell = Spell.WEAKEN;
        else if (dist < 8.0) spell = Spell.BLEED;
        else if (dist < 12.0) spell = (mob.getRandom().nextBoolean() ? Spell.DAZE : Spell.BURST);
        else spell = Spell.BURST;

        cast(sl, tgt, spell);
    }

    private void cast(ServerLevel sl, LivingEntity tgt, Spell spell) {
        // Visual telegraph at the caster.
        sl.sendParticles(ParticleTypes.WITCH,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.7,
                mob.getZ(), 16, 0.4, 0.4, 0.4, 0.04);
        sl.playSound(null, mob.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE,
                1.0F, 0.6F + mob.getRandom().nextFloat() * 0.4F);

        if (!(tgt instanceof Player p) || p.isCreative() || p.isSpectator()) return;

        switch (spell) {
            case WEAKEN -> {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0));
            }
            case BLEED -> {
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                if (ModEffects.BLOOD_INFECTION.get() != null) {
                    p.addEffect(new MobEffectInstance(
                            ModEffects.BLOOD_INFECTION.get(), 200, 1));
                }
            }
            case DAZE -> {
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 220, 0));
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
            }
            case BURST -> {
                p.hurt(p.damageSources().indirectMagic(mob, mob), 4.0F);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        p.getX(), p.getY() + 1, p.getZ(), 16, 0.3, 0.4, 0.3, 0.05);
            }
        }
    }

    private enum Spell { WEAKEN, BLEED, DAZE, BURST }
}
