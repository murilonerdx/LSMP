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

/** r183 — Olho Colossal: imenso, lento, tanque — aura de Lentidão + Fadiga; o "chefe" do grupo. */
public class ColossalEyeEntity extends AbstractParasiticEntity {
    public ColossalEyeEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 80.0).add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }
    @Override protected float baseScale() { return 3.2F; }
    @Override protected double observeDist() { return 10.0; }
    @Override protected int maxLifeTicks() { return -1; }              // não expira (efêmero ou não, fica)
    @Override protected int groundInfectChance() { return 15; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.SCULK_CHARGE_POP; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (distanceTo(target) < 18 && tickCount % 40 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, true));
        }
        if (distanceTo(target) < 4 && tickCount % 20 == 0)
            target.hurt(damageSources().mobAttack(this), 7.0F);
        if (tickCount % 10 == 0)
            sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 1.2, 1.2, 1.2, 0.01);
    }
}
