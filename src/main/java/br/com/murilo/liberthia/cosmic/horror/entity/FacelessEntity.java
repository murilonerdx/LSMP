package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * r178: <b>O Sem-Rosto</b> — humanoide pálido que SÓ se move no ESCURO. Na luz ele
 * congela completamente (luz = segurança). No escuro, avança rápido e silencioso;
 * em contato, ataca. Inspirado nos clássicos "a luz te protege".
 */
public class FacelessEntity extends Monster {

    public FacelessEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 48.0);
        if (p == null || p.isCreative() || p.isSpectator()) { this.getNavigation().stop(); return; }

        int light = this.level().getMaxLocalRawBrightness(this.blockPosition());
        boolean watched = isWatchedBy(p);

        // LUZ ou sendo ENCARADO → congela (anjo chorão + medo da luz)
        if (light > 7 || watched) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            this.setXxa(0); this.setZza(0);
            this.getLookControl().setLookAt(p);
            return;
        }

        // ESCURO e não observado → avança e ataca
        this.setTarget(p);
        this.getNavigation().moveTo(p, 1.15);
        if (this.distanceTo(p) < 1.9 && this.attackAnim <= 0F) {
            p.hurt(this.damageSources().mobAttack(this), 6.0F);
            Vec3 kb = p.position().subtract(this.position()).normalize().scale(0.5);
            p.push(kb.x, 0.25, kb.z);
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    private boolean isWatchedBy(Player p) {
        Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.6, 0).subtract(p.getEyePosition());
        if (toMe.lengthSqr() < 1.0e-4) return false;
        return toMe.normalize().dot(p.getLookAngle()) > 0.6 && this.hasLineOfSight(p);
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean canBeAffected(MobEffectInstance e) { return false; }
}
