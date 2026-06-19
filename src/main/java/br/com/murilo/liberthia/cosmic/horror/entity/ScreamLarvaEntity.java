package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** r183 — Larva Gritante: solta um grito que empurra e ensurdece (náusea) em área. */
public class ScreamLarvaEntity extends AbstractParasiticEntity {
    public ScreamLarvaEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.29).add(Attributes.ATTACK_DAMAGE, 1.0);
    }
    @Override protected float baseScale() { return 0.8F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.SCULK_SOUL; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (distanceTo(target) < 9 && tickCount % 90 == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.7F, 1.6F);
            Vec3 push = target.position().subtract(position()).normalize().scale(1.2);
            target.push(push.x, 0.45, push.z);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, true));
            target.hurt(damageSources().mobAttack(this), 2.0F);
            for (int i = 0; i < 24; i++) {
                double a = sl.random.nextDouble() * Math.PI * 2;
                sl.sendParticles(ParticleTypes.SONIC_BOOM, getX() + Math.cos(a) * 2, getY() + 0.5, getZ() + Math.sin(a) * 2, 1, 0, 0, 0, 0);
            }
        }
    }
}
