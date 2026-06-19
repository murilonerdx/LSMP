package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.magic.effect.SynergyEffects;
import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * r87: <b>Electromancer</b> — wizard de raio. Casta lightning chain + Static charge.
 */
public class ElectromancerEntity extends AbstractWizardEntity {
    public ElectromancerEntity(EntityType<? extends net.minecraft.world.entity.monster.Monster> type, Level level) {
        super(type, level);
    }

    @Override public SpellSchool school() { return SpellSchool.LIGHTNING; }
    @Override public int getProjectileColor() { return 0xFFFFFF44; }
    @Override public int getCastCooldownTicks() { return 80; }
    @Override public br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type vfxType() {
        return br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MAGICKA_HIT;
    }

    @Override
    public void castSpellAt(LivingEntity target) {
        castVfxBeam(target); // r178: VFX de spell animado (não partícula vanilla)
        // 6 dano lightning + static charge stack
        dealSchoolDamage(target, 6.0F, SchoolDamageSource.of(SpellSchool.LIGHTNING));
        int currentAmp = target.hasEffect(SynergyEffects.STATIC_CHARGE.get())
                ? target.getEffect(SynergyEffects.STATIC_CHARGE.get()).getAmplifier() + 1 : 0;
        target.addEffect(new MobEffectInstance(SynergyEffects.STATIC_CHARGE.get(),
                160, currentAmp, true, true));
        // Visual lightning (only flash, no entity damage)
        if (level() instanceof ServerLevel sl && Math.random() < 0.3) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
        }
    }
}
