package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * r87: <b>Pyromancer</b> — wizard de fogo. Casta fireball + burn.
 */
public class PyromancerEntity extends AbstractWizardEntity {
    public PyromancerEntity(EntityType<? extends net.minecraft.world.entity.monster.Monster> type, Level level) {
        super(type, level);
    }

    @Override public SpellSchool school() { return SpellSchool.FIRE; }
    @Override public int getProjectileColor() { return 0xFFFF6633; }
    @Override public int getCastCooldownTicks() { return 60; }

    @Override
    public void castSpellAt(LivingEntity target) {
        // Beam de fogo + 4 dano + 60 burn ticks
        drawBeam(target, ParticleTypes.FLAME);
        target.setRemainingFireTicks(target.getRemainingFireTicks() + 80);
        dealSchoolDamage(target, 4.0F, SchoolDamageSource.fire(80));
    }
}
