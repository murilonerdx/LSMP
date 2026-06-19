package br.com.murilo.liberthia.magic.mageclass;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * r162: Aplica bônus de classe de mago aos targets de spell.
 *
 * <p>Chamado pelo {@code SpellFactory.applyDamageAndEffects} para cada hit.
 */
public final class MageClassBonus {

    private MageClassBonus() {}

    /**
     * Aplica todos os bônus de classe do caster ao target.
     * Retorna o multiplicador de dano final (pra aplicar antes do hurt()).
     */
    public static float apply(Player caster, LivingEntity target) {
        if (caster == null) return 1.0F;
        MageClass cls = MageClassData.get(caster);
        if (cls == null) return 1.0F;
        int level = MageClassData.getLevel(caster);
        float mult = cls.dmgMultiplier(level);

        // Apply secondary effect baseado no tipo da classe
        applySecondaryEffect(cls, target, level);

        // XP per cast
        MageClassData.addXp(caster, 5);
        return mult;
    }

    private static void applySecondaryEffect(MageClass cls, LivingEntity target, int level) {
        int dur = cls.effectDuration * 20;  // segundos → ticks
        int amp = level / 3;  // 0..3 stacks

        switch (cls.primaryEffect) {
            case "poison":
                target.addEffect(new MobEffectInstance(MobEffects.POISON, dur, amp));
                break;
            case "fire":
                target.setSecondsOnFire(cls.effectDuration);
                break;
            case "chaos":
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, dur, amp));
                break;
            case "sanity":
                // Custom: nightmare/paranoia from r156
                target.addEffect(new MobEffectInstance(ModEffects.NIGHTMARE.get(), dur, amp));
                break;
            case "void":
                target.addEffect(new MobEffectInstance(ModEffects.HUNGRY_VOID.get(), dur, amp));
                break;
            case "heal":
                target.heal(2F + amp);
                break;
            case "slow":
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, amp + 1));
                break;
            case "crit":
                // No additional effect — crit handled via mult (which is 2x at level 10)
                break;
            case "raw":
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, amp));
                break;
            case "evade":
                // Self only — speed boost on caster
                break;
            case "speed":
                // Self only — speed boost on caster
                break;
            case "dash":
                // Self only — strength + speed boost on caster
                break;
            case "fly":
                // Self only — slow falling on caster
                break;
            case "all":
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, amp));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, amp));
                break;
        }

        // Secondary effect
        switch (cls.secondaryEffect) {
            case "burn":
                target.setSecondsOnFire(cls.effectDuration);
                break;
            case "sanity":
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dur, amp));
                break;
            case "regen":
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, amp));
                break;
            case "weaken":
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, amp));
                break;
            case "kb":
                // Knockback handled elsewhere
                break;
            case "amplify":
                // Damage multiplier handled
                break;
        }
    }

    /** Self-only bonus pra classes de mobility/defense. Aplica no caster. */
    public static void applyToSelf(Player caster) {
        if (caster == null) return;
        MageClass cls = MageClassData.get(caster);
        if (cls == null) return;
        int level = MageClassData.getLevel(caster);
        int dur = cls.effectDuration * 20;
        int amp = level / 3;
        switch (cls.primaryEffect) {
            case "evade":
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, amp + 1));
                caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0));
                break;
            case "speed":
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, amp + 1));
                break;
            case "dash":
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, amp + 2));
                caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, amp));
                break;
            case "fly":
                caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, dur, 0));
                caster.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, amp + 1));
                break;
        }
    }
}
