package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.magic.effect.SynergyEffects;
import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * r87: <b>Cryomancer</b> — wizard de gelo. Casta frost beam + Chilled stacks.
 */
public class CryomancerEntity extends AbstractWizardEntity {
    public CryomancerEntity(EntityType<? extends net.minecraft.world.entity.monster.Monster> type, Level level) {
        super(type, level);
    }

    @Override public SpellSchool school() { return SpellSchool.ICE; }
    @Override public int getProjectileColor() { return 0xFF66CCFF; }
    @Override public int getCastCooldownTicks() { return 50; }

    @Override
    public void castSpellAt(LivingEntity target) {
        drawBeam(target, ParticleTypes.SNOWFLAKE);
        // 3 dano + 60 freeze ticks + 1 stack Chilled
        target.setTicksFrozen(target.getTicksFrozen() + 80);
        dealSchoolDamage(target, 3.0F, SchoolDamageSource.ice(80));
        // Aplica Chilled — escalando em amp se já tinha
        int currentAmp = target.hasEffect(SynergyEffects.CHILLED.get())
                ? target.getEffect(SynergyEffects.CHILLED.get()).getAmplifier() + 1 : 0;
        target.addEffect(new MobEffectInstance(SynergyEffects.CHILLED.get(),
                200, currentAmp, true, true));
    }
}
