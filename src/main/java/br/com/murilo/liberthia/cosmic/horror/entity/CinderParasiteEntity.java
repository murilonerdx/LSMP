package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** r183 — Brasa Parasita: lacaio pequeno e RÁPIDO que incendeia quem toca. Efêmero. */
public class CinderParasiteEntity extends AbstractParasiticEntity {
    public CinderParasiteEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.42).add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    @Override protected float baseScale() { return 0.5F; }
    @Override protected double observeDist() { return 0.0; }          // persegue/encosta
    @Override protected int maxLifeTicks() { return 1200; }           // 60s
    @Override protected boolean infectsGround() { return false; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.FLAME; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (distanceTo(target) < 1.6 && tickCount % 10 == 0) {
            target.setSecondsOnFire(4);
            target.hurt(damageSources().mobAttack(this), 2.0F);
            sl.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 0.5, target.getZ(), 4, 0.2, 0.3, 0.2, 0.0);
        }
    }
}
