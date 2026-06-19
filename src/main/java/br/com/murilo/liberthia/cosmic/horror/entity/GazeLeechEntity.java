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

/** r183 — Sanguessuga do Olhar: ENCARAR ela a faz CRESCER e ganhar força/velocidade. Grande = perigosa. */
public class GazeLeechEntity extends AbstractParasiticEntity {
    public GazeLeechEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.ATTACK_DAMAGE, 3.0);
    }
    @Override protected float baseScale() { return 0.8F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.WITCH; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (isWatchedBy(target)) {
            if (tickCount % 10 == 0) {
                growBy(0.06F);
                int lvl = (int) Math.min(3, billboardScale());
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, lvl, false, false));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, lvl, false, false));
            }
            sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + getBbHeight(), getZ(), 3, 0.2, 0.2, 0.2, 0.01);
        }
        if (billboardScale() > 1.6F && distanceTo(target) < 2.6 && tickCount % 16 == 0)
            target.hurt(damageSources().mobAttack(this), 4.0F + billboardScale());
    }
}
