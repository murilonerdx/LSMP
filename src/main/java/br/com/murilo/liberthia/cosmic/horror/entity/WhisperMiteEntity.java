package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** r183 — Ácaro Sussurrante: minúsculo, em enxame — náusea + sussurros; raramente se multiplica. */
public class WhisperMiteEntity extends AbstractParasiticEntity {
    public WhisperMiteEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 2.0).add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.ATTACK_DAMAGE, 1.0);
    }
    @Override protected float baseScale() { return 0.35F; }
    @Override protected int groundInfectChance() { return 200; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.WARPED_SPORE; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (distanceTo(target) < 8 && tickCount % 60 == 0)
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
        if (tickCount % 70 == 0 && distanceTo(target) < 12)
            sl.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 0.4F, 0.5F);
        // multiplica raramente (máx 6 no entorno)
        if (tickCount % 200 == 0 && sl.random.nextInt(4) == 0
                && sl.getEntitiesOfClass(WhisperMiteEntity.class, target.getBoundingBox().inflate(48)).size() < 8) {
            WhisperMiteEntity m = ModEntities.WHISPER_MITE.get().create(sl);
            if (m != null) { m.moveTo(getX(), getY(), getZ(), getYRot(), 0);
                m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                sl.addFreshEntity(m); }
        }
    }
}
