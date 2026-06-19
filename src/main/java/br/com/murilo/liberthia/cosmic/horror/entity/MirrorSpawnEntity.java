package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** r183 — Reflexo: copia a orientação do player; quando encarado, gera uma brasa e "se divide". */
public class MirrorSpawnEntity extends AbstractParasiticEntity {
    public MirrorSpawnEntity(EntityType<? extends Monster> t, Level l) { super(t, l); }
    public static AttributeSupplier.Builder createAttributes() {
        return AbstractParasiticEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 9.0).add(Attributes.MOVEMENT_SPEED, 0.31).add(Attributes.ATTACK_DAMAGE, 2.0);
    }
    @Override protected float baseScale() { return 1.0F; }
    @Override protected ParticleOptions ambientParticle() { return ParticleTypes.GLOW; }
    @Override protected void tickAbility(ServerLevel sl, Player target) {
        // espelha a direção do player (encara igual)
        this.setYRot(target.getYRot() + 180F);
        if (isWatchedBy(target) && tickCount % 100 == 0 && distanceTo(target) < 20
                && sl.getEntitiesOfClass(CinderParasiteEntity.class, getBoundingBox().inflate(24)).size() < 3) {
            CinderParasiteEntity c = ModEntities.CINDER_PARASITE.get().create(sl);
            if (c != null) { c.moveTo(getX(), getY(), getZ(), getYRot(), 0);
                c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
                sl.addFreshEntity(c); }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5, getZ(), 12, 0.3, 0.4, 0.3, 0.04);
        }
        if (distanceTo(target) < 2.4 && tickCount % 18 == 0)
            target.hurt(damageSources().mobAttack(this), 3.0F);
    }
}
