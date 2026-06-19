package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** r183 — Tecelão Cego: ENCARAR fixamente te CEGA; ele invoca brasas (lacaios de fogo rápidos). */
public class BlindWeaverEntity extends AbstractParasiticEntity {
    public BlindWeaverEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    @Override protected float baseScale() { return 1.2F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.SQUID_INK; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (isWatchedBy(target) && tickCount % 30 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, true));
        }
        // invoca brasas perto do player periodicamente (máx ~4 por perto)
        if (tickCount % 120 == 0 && distanceTo(target) < 24
                && sl.getEntitiesOfClass(CinderParasiteEntity.class, getBoundingBox().inflate(28)).size() < 4) {
            for (int i = 0; i < 2; i++) {
                CinderParasiteEntity c = ModEntities.CINDER_PARASITE.get().create(sl);
                if (c == null) continue;
                c.moveTo(getX() + sl.random.nextInt(3) - 1, getY(), getZ() + sl.random.nextInt(3) - 1, getYRot(), 0);
                c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                sl.addFreshEntity(c);
            }
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.5, getZ(), 10, 0.4, 0.4, 0.4, 0.02);
        }
    }
}
