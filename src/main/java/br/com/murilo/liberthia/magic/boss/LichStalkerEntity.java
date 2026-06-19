package br.com.murilo.liberthia.magic.boss;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * v0.1.24 r98: <b>Lich Stalker</b> — minion stealth do boss Abyssal Lich.
 * Aplica INVISIBILITY brief quando se aproxima de target. Melee builder.
 */
public class LichStalkerEntity extends Monster {

    public LichStalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 3.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;

        // Invisibility quando target longe
        if (getTarget() != null) {
            double dist = distanceTo(getTarget());
            if (dist > 6 && dist < 16) {
                this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false));
            }
        }
    }
}
