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

/** r183 — Carrapato do Vazio: TELEPORTA pras costas do player e drena fome/saturação. */
public class VoidTickEntity extends AbstractParasiticEntity {
    public VoidTickEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    @Override protected float baseScale() { return 0.55F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.PORTAL; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (tickCount % 80 == 0 && !isWatchedBy(target) && distanceTo(target) < 20) {
            Vec3 behind = target.position().subtract(target.getViewVector(1f).multiply(2, 0, 2));
            this.teleportTo(behind.x, target.getY(), behind.z);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5, getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        }
        if (distanceTo(target) < 2.2 && tickCount % 20 == 0) {
            target.hurt(damageSources().mobAttack(this), 2.0F);
            target.causeFoodExhaustion(4.0F);
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, 0, false, true));
        }
    }
}
