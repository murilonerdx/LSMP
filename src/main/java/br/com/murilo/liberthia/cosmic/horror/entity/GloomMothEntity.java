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

/** r183 — Mariposa da Penumbra: voa errático, suga a luz (Escuridão + Cegueira curta). */
public class GloomMothEntity extends AbstractParasiticEntity {
    public GloomMothEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 5.0).add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.ATTACK_DAMAGE, 1.0);
    }
    @Override protected float baseScale() { return 0.7F; }
    @Override protected double observeDist() { return 4.0; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.SQUID_INK; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        // movimento errático
        if (tickCount % 6 == 0)
            setDeltaMovement(getDeltaMovement().add((sl.random.nextDouble() - 0.5) * 0.2, (sl.random.nextDouble() - 0.5) * 0.1, (sl.random.nextDouble() - 0.5) * 0.2));
        if (distanceTo(target) < 6 && tickCount % 50 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, true));
        }
    }
}
