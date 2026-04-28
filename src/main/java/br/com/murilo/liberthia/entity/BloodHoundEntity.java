package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.item.CrystallizedBloodSoulItem;
import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Blood Hound — corrupted wolf cult dog. Hostile by default. Can be TAMED via
 * {@code blood_pact_amulet} when at low HP.
 *
 * <p>Familiar effects (when tamed and within 8 blocks of owner): Strength I + Lifesteal
 * tags applied periodically.
 *
 * <p>On death (tamed only), drops a {@code crystallized_blood_soul} with the
 * full entity NBT — right-click the soul to respawn an identical hound.
 */
public class BloodHoundEntity extends Wolf implements Enemy {

    public BloodHoundEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        this.setTame(false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // Hostile only when NOT tamed.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> !this.isTame()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()
                || effect.getEffect() == ModEffects.HEMO_SICKNESS.get()) {
            return false;
        }
        return super.canBeAffected(effect);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le && !BloodKin.is(le)) {
            // tamed hounds don't infect their owner's allies
            if (!this.isTame() || target != this.getOwner()) {
                le.addEffect(new MobEffectInstance(ModEffects.HEMO_SICKNESS.get(), 20 * 8, 0, false, true, true));
            }
        }
        return hit;
    }

    @Override
    public boolean canBeLeashed(Player player) { return this.isTame(); }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.CONGEALED_BLOOD.get()) || stack.is(net.minecraft.world.item.Items.ROTTEN_FLESH);
    }

    /** Right-click with blood_pact_amulet at low HP → tame. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.level().isClientSide && !this.isTame()
                && stack.is(ModItems.BLOOD_PACT_AMULET.get())
                && this.getHealth() < this.getMaxHealth() * 0.30F) {
            this.tame(player);
            this.setHealth(this.getMaxHealth());
            this.setOrderedToSit(false);
            this.navigation.stop();
            this.setTarget(null);
            this.level().broadcastEntityEvent(this, (byte) 7); // hearts particle
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        // Feeding restores HP if tamed and owner.
        if (this.isTame() && this.isOwnedBy(player) && isFood(stack) && this.getHealth() < this.getMaxHealth()) {
            this.heal(4F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            this.level().broadcastEntityEvent(this, (byte) 7);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Familiar aura — owner within 8 blocks gets buffs every 100 ticks. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.isTame() && this.tickCount % 100 == 0) {
            LivingEntity owner = this.getOwner();
            if (owner instanceof Player p && p.distanceToSqr(this) <= 64.0D) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, true, false, true));
            }
        }
    }

    /** Drops a Soul Shard NBT-encoded copy if tamed; otherwise normal loot. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!(this.level() instanceof ServerLevel)) return;

        if (this.isTame() && ModItems.CRYSTALLIZED_BLOOD_SOUL.get() != null) {
            ItemStack shard = new ItemStack(ModItems.CRYSTALLIZED_BLOOD_SOUL.get());
            CompoundTag nbt = new CompoundTag();
            // simulate-healthy-state to avoid death-on-respawn
            this.setHealth(this.getMaxHealth());
            this.removeAllEffects();
            this.saveWithoutId(nbt);
            CrystallizedBloodSoulItem.writeEntity(shard, this.getType(), nbt);
            spawnAtLocation(shard);
            return; // skip normal drop
        }
        if (random.nextFloat() < 0.5F) {
            spawnAtLocation(new ItemStack(ModItems.BLOODY_RAG.get(), 1));
        }
    }
}
