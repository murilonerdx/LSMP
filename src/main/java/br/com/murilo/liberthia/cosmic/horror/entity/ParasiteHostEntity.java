package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** r183 — Hospedeiro: INFECTA mobs próximos — eles viram parasitados que atacam o player (alvo + força/velocidade + partículas). */
public class ParasiteHostEntity extends AbstractParasiticEntity {
    public ParasiteHostEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    @Override protected float baseScale() { return 1.4F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.WARPED_SPORE; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (tickCount % 40 != 0) return;
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(12))) {
            if (mob == this || mob instanceof AbstractParasiticEntity || !mob.isAlive()) continue;
            mob.setTarget(target);
            mob.setLastHurtByMob(target);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, false));
            mob.getPersistentData().putBoolean("liberthia.parasitized", true);
            sl.sendParticles(ParticleTypes.WARPED_SPORE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        }
    }
}
