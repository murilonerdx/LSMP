package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.entity.ai.BloodBoltGoal;
import br.com.murilo.liberthia.entity.ai.PriestChannelGoal;
import br.com.murilo.liberthia.entity.ai.SummonWormGoal;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Mini-boss caster. While alive near a Blood Altar within radius 20, the
 * altar is considered NOT contained even with 4 chalks — killing the priest
 * restores normal containment. Casts blood bolts, summons worms, self-heals
 * while channeling the altar.
 */
public class BloodPriestEntity extends Monster {

    public static final int CHANNEL_RADIUS = 20;

    public BloodPriestEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PriestChannelGoal(this));
        this.goalSelector.addGoal(2, new BloodBoltGoal(this));
        this.goalSelector.addGoal(3, new SummonWormGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()) return false;
        return super.canBeAffected(effect);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.PRIEST_SIGIL.get(), 1));
        int rags = 1 + random.nextInt(3);
        spawnAtLocation(new ItemStack(ModItems.BLOODY_RAG.get(), rags));
        if (random.nextFloat() < 0.5F) {
            spawnAtLocation(new ItemStack(ModItems.TOME_OF_THE_MOTHER.get(), 1));
        }
        spawnAtLocation(new ItemStack(ModItems.CHALK.get(), 1 + random.nextInt(2)));
    }
}
