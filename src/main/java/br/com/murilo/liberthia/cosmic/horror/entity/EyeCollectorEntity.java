package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r178: <b>O Coletor de Olhos</b> — espreita no escuro e "quer os seus olhos". No escuro
 * avança devagar e, em contato, te cega (Blindness + Darkness). Na luz ele cobre o rosto
 * e fica parado. Quanto mais escuro, mais ousado.
 */
public class EyeCollectorEntity extends Monster {

    public EyeCollectorEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 40.0);
        if (p == null || p.isCreative() || p.isSpectator()) { this.getNavigation().stop(); return; }

        int light = this.level().getMaxLocalRawBrightness(this.blockPosition());
        // LUZ → cobre o rosto e congela
        if (light > 7) {
            this.getNavigation().stop();
            this.setXxa(0); this.setZza(0);
            this.getLookControl().setLookAt(p);
            return;
        }

        // ESCURO → avança e, em contato, arranca a visão
        this.setTarget(p);
        this.getNavigation().moveTo(p, 1.05);
        if (this.distanceTo(p) < 2.0 && this.attackAnim <= 0F) {
            p.hurt(this.damageSources().mobAttack(this), 4.0F);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.level().playSound(null, this.blockPosition(), ModSounds.PERIPHERAL_BLIND.get(),
                    SoundSource.HOSTILE, 0.9F, 0.7F);
        }
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean canBeAffected(MobEffectInstance e) { return false; }
}
