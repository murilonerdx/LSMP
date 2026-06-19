package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r184 — <b>Verme Dimensional</b>. Gerado pela doença <i>Infecção Dimensional</i>: persegue o
 * portador e, ao tocá-lo, aplica Náusea + Fome. Pequeno e efêmero (some sozinho), serve para
 * atormentar quem está infectado até que use o Soro Dimensional. Reutiliza toda a base
 * {@link AbstractParasiticEntity} (billboard, persistência, VFX).
 */
public class DimensionalWormEntity extends AbstractParasiticEntity {
    public DimensionalWormEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override protected float baseScale() { return 0.45F; }
    @Override protected double observeDist() { return 0.0; }       // persegue/encosta
    @Override protected int maxLifeTicks() { return 1800; }        // 90s
    @Override protected boolean infectsGround() { return false; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.WITCH; }

    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (distanceTo(target) < 1.5 && tickCount % 20 == 0) {
            target.hurt(damageSources().mobAttack(this), 1.0F);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 0, false, true));
            sl.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.3, 0.4, 0.3, 0.0);
        }
    }
}
