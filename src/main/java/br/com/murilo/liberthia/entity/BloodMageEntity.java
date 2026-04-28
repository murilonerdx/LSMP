package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.entity.projectile.HemoBoltEntity;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Blood Mage — ranged caster that fires HemoBolts in a 3-round burst and
 * short-teleports when hurt. Unlike Blood Priest it doesn't bind to an altar,
 * so it can spawn naturally at night. Guaranteed drop of
 * {@code HEMOMANCER_STAFF_BROKEN} — but this mod doesn't have that item yet,
 * so we settle for 1–2 Sanguine Essence + a Priest Sigil 15% of the time,
 * which keeps the Hemomancer Staff craft reachable without requiring a Priest.
 *
 * EvilCraft inspiration: {@code EntityVengeanceSpirit}'s flight pattern plus
 * light burst-casting semantics from evokers.
 */
public class BloodMageEntity extends Monster {

    public BloodMageEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new HemoBurstGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()
                || effect.getEffect() == ModEffects.HEMO_SICKNESS.get()) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    // Short-range teleport when hurt — evokes EvilCraft vengeance spirits.
    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean r = super.hurt(src, amount);
        if (r && !this.level().isClientSide && this.isAlive() && this.random.nextFloat() < 0.35F) {
            tryBlink();
        }
        return r;
    }

    private void tryBlink() {
        for (int i = 0; i < 8; i++) {
            double nx = this.getX() + (this.random.nextDouble() - 0.5) * 8.0;
            double ny = this.getY() + (this.random.nextInt(3) - 1);
            double nz = this.getZ() + (this.random.nextDouble() - 0.5) * 8.0;
            if (this.level().noCollision(this.getBoundingBox().move(nx - getX(), ny - getY(), nz - getZ()))) {
                ServerLevel sl = (ServerLevel) this.level();
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
                sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.3F);
                this.teleportTo(nx, ny, nz);
                sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
                break;
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int essence = 1 + random.nextInt(2);
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_ESSENCE.get(), essence));
        if (random.nextFloat() < 0.15F) {
            spawnAtLocation(new ItemStack(ModItems.PRIEST_SIGIL.get(), 1));
        }
        if (random.nextFloat() < 0.4F) {
            spawnAtLocation(new ItemStack(ModItems.BLOODY_RAG.get(), 1));
        }
    }

    /** Three-shot HemoBolt burst with ~6s cooldown. */
    private static class HemoBurstGoal extends Goal {
        private final BloodMageEntity mage;
        private int cooldown = 60;
        private int shotsLeft = 0;
        private int shotDelay = 0;

        HemoBurstGoal(BloodMageEntity mage) {
            this.mage = mage;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity tgt = mage.getTarget();
            return tgt != null && tgt.isAlive() && mage.distanceToSqr(tgt) < 24.0 * 24.0;
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity tgt = mage.getTarget();
            if (tgt == null) return;
            mage.getLookControl().setLookAt(tgt, 30F, 30F);

            if (shotsLeft > 0) {
                if (--shotDelay <= 0) {
                    fireBolt(tgt);
                    shotsLeft--;
                    shotDelay = 6;
                    if (shotsLeft == 0) cooldown = 120;
                }
                return;
            }
            if (--cooldown <= 0) {
                shotsLeft = 3;
                shotDelay = 0;
            }
        }

        private void fireBolt(LivingEntity tgt) {
            double dx = tgt.getX() - mage.getX();
            double dy = tgt.getY(0.5) - (mage.getY() + 1.4);
            double dz = tgt.getZ() - mage.getZ();
            HemoBoltEntity bolt = new HemoBoltEntity(mage.level(), mage, dx, dy, dz);
            bolt.setPos(mage.getX(), mage.getY() + 1.4, mage.getZ());
            mage.level().addFreshEntity(bolt);

            if (mage.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        mage.getX(), mage.getY() + 1.6, mage.getZ(),
                        10, 0.2, 0.2, 0.2, 0.04);
                sl.playSound(null, mage.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                        SoundSource.HOSTILE, 0.9F, 0.8F);
            }
        }
    }
}
