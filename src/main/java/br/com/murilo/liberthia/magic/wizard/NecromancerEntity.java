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
 * r87: <b>Necromancer</b> — wizard de sangue. Casta blood drain + Bleed.
 */
public class NecromancerEntity extends AbstractWizardEntity {
    public NecromancerEntity(EntityType<? extends net.minecraft.world.entity.monster.Monster> type, Level level) {
        super(type, level);
    }

    @Override public SpellSchool school() { return SpellSchool.BLOOD; }
    @Override public int getProjectileColor() { return 0xFF990033; }
    @Override public int getCastCooldownTicks() { return 70; }
    @Override public br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type vfxType() {
        return br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.FELSPELL;
    }

    @Override
    public void castSpellAt(LivingEntity target) {
        castVfxBeam(target); // r178: VFX de spell animado
        // 5 dano com lifesteal 50% + bleed
        dealSchoolDamage(target, 5.0F, SchoolDamageSource.blood(0.5F));
        target.addEffect(new MobEffectInstance(SynergyEffects.BLEED.get(),
                160, 1, true, true));
    }
}
