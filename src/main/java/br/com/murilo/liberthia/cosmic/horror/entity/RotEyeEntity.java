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

/** r183 — Olho Pútrido: espalha podridão — Veneno + Wither; infecta o chão rápido. */
public class RotEyeEntity extends AbstractParasiticEntity {
    public RotEyeEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    @Override protected float baseScale() { return 0.9F; }
    @Override protected int groundInfectChance() { return 25; } // infecta rápido
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.SNEEZE; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        if (distanceTo(target) < 7 && tickCount % 50 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, target.getX(), target.getY() + 1, target.getZ(), 8, 0.4, 0.6, 0.4, 0.0);
        }
    }
}
