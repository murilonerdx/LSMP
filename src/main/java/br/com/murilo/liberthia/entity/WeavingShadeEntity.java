package br.com.murilo.liberthia.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Weaving Shade — small spectral monster whose mere presence weakens nearby
 * players. While alive, every 60 ticks pulses §eWeakness II + §7Slowness I +
 * §6Mining Fatigue I to all non-{@link br.com.murilo.liberthia.logic.BloodKin}
 * players within 8 blocks. Melee is feeble (2 dmg) but the aura is the threat.
 *
 * <p>Reuses {@code minecraft:textures/entity/illager/vex.png} via the renderer.
 */
public class WeavingShadeEntity extends Monster {

    private int auraCooldown = 300;

    public WeavingShadeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new OpenDoorGoal(this, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;

        if (--auraCooldown <= 0) {
            auraCooldown = 60;
            pulseAura();
        }
    }

    private void pulseAura() {
        if (!(level() instanceof ServerLevel sl)) return;
        AABB box = new AABB(blockPosition()).inflate(8.0);
        boolean any = false;
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true, true));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, true, true));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 0, false, true, true));
            any = true;
        }
        if (any) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    getX(), getY() + 1.0, getZ(), 12, 1.5, 0.6, 1.5, 0.05);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player p) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true, true));
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextFloat() < 0.30F
                && br.com.murilo.liberthia.registry.ModItems.SANGUINE_ESSENCE.get() != null) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    br.com.murilo.liberthia.registry.ModItems.SANGUINE_ESSENCE.get(), 1));
        }
    }
}
