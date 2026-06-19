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

/** r183 — Vigia de Carne: quando NÃO está sendo observado, se cura e se aproxima rápido; dá fome. */
public class FleshWatcherEntity extends AbstractParasiticEntity {
    public FleshWatcherEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3.0);
    }
    @Override protected float baseScale() { return 1.3F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.CRIMSON_SPORE; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        boolean watched = isWatchedBy(target);
        if (!watched && tickCount % 20 == 0 && getHealth() < getMaxHealth())
            heal(1.5F); // regenera de costas (estilo Weeping Angel/carne)
        if (distanceTo(target) < 2.4 && tickCount % 16 == 0) {
            target.hurt(damageSources().mobAttack(this), 4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1, false, true));
        }
        if (!watched) sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + getBbHeight(), getZ(), 1, 0.2, 0.2, 0.2, 0.0);
    }
}
