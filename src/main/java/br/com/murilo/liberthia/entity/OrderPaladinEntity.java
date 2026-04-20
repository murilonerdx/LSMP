package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.faction.Faction;
import br.com.murilo.liberthia.faction.FactionReputation;
import br.com.murilo.liberthia.faction.FactionTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Order Paladin — armored humanoid that targets Blood faction mobs and any
 * player whose Blood reputation > +50. Drops iron armor fragments + a chance
 * to drop Holy Blade / Holy Hammer (if available).
 */
public class OrderPaladinEntity extends Monster {

    public OrderPaladinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Blood faction mobs
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> FactionTag.get(e) == Faction.BLOOD));
        // Blood-friendly players
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> isBloodAligned(e)));
    }

    private boolean isBloodAligned(net.minecraft.world.entity.LivingEntity e) {
        if (!(e instanceof ServerPlayer p)) return false;
        if (!(p.level() instanceof ServerLevel sl)) return false;
        int blood = FactionReputation.forLevel(sl).get(p.getUUID(), Faction.BLOOD);
        return blood > 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextFloat() < 0.4F) spawnAtLocation(new ItemStack(Items.IRON_INGOT, 1 + random.nextInt(2)));
        if (random.nextFloat() < 0.15F) spawnAtLocation(new ItemStack(Items.EMERALD));
        if (random.nextFloat() < 0.05F) {
            spawnAtLocation(new ItemStack(br.com.murilo.liberthia.registry.ModItems.HOLY_BLADE.get()));
        }
    }
}
