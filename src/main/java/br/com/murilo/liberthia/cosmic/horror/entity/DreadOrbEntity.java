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

/** r183 — Orbe do Pavor: grande, paira; pulsos de Escuridão + Lentidão em área. */
public class DreadOrbEntity extends AbstractParasiticEntity {
    public DreadOrbEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 1.0);
    }
    @Override protected float baseScale() { return 2.0F; }
    @Override protected double observeDist() { return 8.0; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.REVERSE_PORTAL; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (tickCount % 60 == 0 && distanceTo(target) < 14) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, true));
            sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.0, getZ(), 20, 1.0, 0.6, 1.0, 0.02);
        }
    }
}
