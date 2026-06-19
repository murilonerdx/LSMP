package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.cosmic.framework.HorrorFramework;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import br.com.murilo.liberthia.magic.effect.SynergyEffects;
import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r87: <b>Eldritch Cultist</b> — wizard cósmico. Casta horror + Heartstop +
 * adiciona ELDRITCH horror exposure ao player.
 */
public class EldritchCultistEntity extends AbstractWizardEntity {
    public EldritchCultistEntity(EntityType<? extends net.minecraft.world.entity.monster.Monster> type, Level level) {
        super(type, level);
    }

    @Override public SpellSchool school() { return SpellSchool.ELDRITCH; }
    @Override public int getProjectileColor() { return 0xFF6633CC; }
    @Override public int getCastCooldownTicks() { return 90; }
    @Override public br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type vfxType() {
        return br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.VORTEX;
    }

    @Override
    public void castSpellAt(LivingEntity target) {
        castVfxBeam(target); // r178: VFX de spell animado
        // 4 dano + heartstop + horror
        dealSchoolDamage(target, 4.0F, SchoolDamageSource.eldritch());
        target.addEffect(new MobEffectInstance(SynergyEffects.HEARTSTOP.get(),
                40, 0, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, true, true));
        // Adiciona horror cósmico se for player
        if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
            HorrorFramework.getState(sp).addExposure(HorrorType.COGNITIVE, 8.0F);
            HorrorFramework.getState(sp).addExposure(HorrorType.COSMIC, 5.0F);
        }
    }
}
