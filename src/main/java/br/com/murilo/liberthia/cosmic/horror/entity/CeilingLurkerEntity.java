package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r178: <b>O do Teto</b> — fica imóvel pendurado no teto (sem gravidade). Quando um
 * player passa logo ABAIXO, ele se SOLTA e despenca em cima, com susto + dano. Depois
 * de cair, persegue um pouco e some. Clássico ambush de caverna.
 */
public class CeilingLurkerEntity extends Monster {

    private boolean dropping = false;
    private int sinceDrop = 0;

    public CeilingLurkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setSilent(true);
        this.setNoGravity(true); // pendurado
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 24.0);

        if (!dropping) {
            this.setNoGravity(true);
            this.setDeltaMovement(0, 0, 0);
            if (p == null) return;
            this.getLookControl().setLookAt(p);
            double dx = p.getX() - this.getX(), dz = p.getZ() - this.getZ();
            boolean under = (dx * dx + dz * dz) < 2.5 * 2.5 && p.getY() < this.getY() - 1.0;
            if (under) {
                dropping = true;
                this.setNoGravity(false); // CAI
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.4F);
            }
            return;
        }

        // caindo / caçando após cair
        sinceDrop++;
        if (p != null) {
            this.setTarget(p);
            this.getNavigation().moveTo(p, 1.2);
            if (this.distanceTo(p) < 2.0 && this.attackAnim <= 0F) {
                p.hurt(this.damageSources().mobAttack(this), 8.0F);
                this.swing(InteractionHand.MAIN_HAND);
            }
        }
        if (sinceDrop > 200) { // ~10s após cair → some
            this.discard();
        }
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean canBeAffected(MobEffectInstance e) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dropping", dropping);
        tag.putInt("SinceDrop", sinceDrop);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dropping = tag.getBoolean("Dropping");
        sinceDrop = tag.getInt("SinceDrop");
    }
}
