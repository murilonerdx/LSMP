package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.faction.Faction;
import br.com.murilo.liberthia.faction.FactionTag;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Robed melee cultist. Applies BloodInfection on hit, shouts cult phrases
 * to nearby players occasionally. Spawns naturally + in Cult Camps.
 */
public class BloodCultistEntity extends Monster {

    private static final String[] PHRASES = {
            "§cA Mãe nos chama...",
            "§cSua carne é fraca.",
            "§cO altar espera.",
            "§cSangue por sangue.",
            "§cHaverá nova carne."
    };

    private int chatCooldown = 120;

    public BloodCultistEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> FactionTag.get(e) == Faction.ORDER));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 25.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le && ModEffects.BLOOD_INFECTION.get() != null) {
            le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 100, 0));
        }
        return hit;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        chatCooldown--;
        if (chatCooldown <= 0) {
            chatCooldown = 200 + random.nextInt(200);
            if (random.nextFloat() < 0.4F) {
                Player p = level().getNearestPlayer(this, 12.0);
                if (p != null) {
                    p.displayClientMessage(Component.literal(
                            PHRASES[random.nextInt(PHRASES.length)]), false);
                }
            }
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()) return false;
        return super.canBeAffected(effect);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextFloat() < 0.55F) {
            spawnAtLocation(new ItemStack(ModItems.BLOODY_RAG.get(), 1));
        }
        if (random.nextFloat() < 0.30F) {
            spawnAtLocation(new ItemStack(ModItems.RUSTED_DAGGER.get(), 1));
        }
        if (random.nextFloat() < 0.05F) {
            spawnAtLocation(new ItemStack(ModItems.CHALK.get(), 1));
        }
        if (random.nextFloat() < 0.02F) {
            spawnAtLocation(new ItemStack(ModItems.PRIEST_SIGIL.get(), 1));
        }
    }
}
