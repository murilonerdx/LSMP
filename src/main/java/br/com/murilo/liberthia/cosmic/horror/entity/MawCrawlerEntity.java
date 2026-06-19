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
import net.minecraft.world.phys.Vec3;

/** r183 — Rastejante de Bocas: PUXA o player pra perto e morde (wither). */
public class MawCrawlerEntity extends AbstractParasiticEntity {
    public MawCrawlerEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3.0);
    }
    @Override protected float baseScale() { return 1.0F; }
    @Override protected double observeDist() { return 3.0; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.CRIMSON_SPORE; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        double d = distanceTo(target);
        if (d < 12 && d > 2.5 && tickCount % 50 == 0) {
            Vec3 pull = position().subtract(target.position()).normalize().scale(1.4);
            target.push(pull.x, 0.3, pull.z);
            target.hurtMarked = true;
            sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, target.getX(), target.getY() + 0.5, target.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
        }
        if (d < 2.4 && tickCount % 16 == 0) {
            target.hurt(damageSources().mobAttack(this), 4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
        }
    }
}
