package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.magic.effect.SynergyEffects;
import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * v0.1.24 r106: <b>Archevoker</b> — wizard high-tier que casta RANDOMLY de
 * 3 escolas (Fire/Lightning/Eldritch) com damage maior.
 *
 * <p>Mais HP (60) e cast cooldown menor (40t). Mini-boss tier.
 */
public class ArchevokerEntity extends AbstractWizardEntity {

    public ArchevokerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createArchevokerAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override public SpellSchool school() { return SpellSchool.ELDRITCH; }
    @Override public int getProjectileColor() { return 0xFF6633CC; }
    @Override public int getCastCooldownTicks() { return 40; }
    @Override public double getCastRange() { return 20.0; }
    @Override public br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type vfxType() {
        return br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.NEBULA;
    }

    @Override
    public void castSpellAt(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        castVfxBeam(target); // r178: VFX de spell animado (boss)

        int roll = random.nextInt(3);
        switch (roll) {
            case 0 -> {
                // FIRE volley
                target.setRemainingFireTicks(120);
                target.hurt(SchoolDamageSource.fire(120).toVanilla(sl, this), 6F);
                drawBeam(target, ParticleTypes.FLAME);
            }
            case 1 -> {
                // LIGHTNING
                target.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(sl, this), 8F);
                target.addEffect(new MobEffectInstance(SynergyEffects.STATIC_CHARGE.get(), 160, 1, true, true));
                drawBeam(target, ParticleTypes.ELECTRIC_SPARK);
            }
            case 2 -> {
                // ELDRITCH
                target.hurt(SchoolDamageSource.eldritch().toVanilla(sl, this), 7F);
                target.addEffect(new MobEffectInstance(SynergyEffects.HEARTSTOP.get(), 40, 0, true, true));
                drawBeam(target, ParticleTypes.SOUL);
            }
        }

        // Aura visual constante
        for (int i = 0; i < 12; i++) {
            double a = (i / 12.0) * Math.PI * 2;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(a) * 1.5,
                    getY() + 1.5,
                    getZ() + Math.sin(a) * 1.5,
                    1, 0, 0, 0, 0);
        }
    }
}
