package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.logic.ProtectionUtils;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CorruptedZombieEntity extends Zombie {

    private int sporeCooldown = 0;

    public CorruptedZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 25.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);
        if (result && target instanceof LivingEntity livingTarget) {
            livingTarget.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                data.addInfection(5);
                data.setDirty(true);
            });
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level().isClientSide) {
            return;
        }

        sporeCooldown--;
        if (sporeCooldown <= 0) {
            sporeCooldown = 80;

            List<Player> nearbyPlayers = level().getEntitiesOfClass(
                    Player.class,
                    new AABB(blockPosition()).inflate(12.0)
            );

            if (!nearbyPlayers.isEmpty()) {
                Player target = nearbyPlayers.get(0);
                DarkMatterSporeEntity spore = new DarkMatterSporeEntity(
                        ModEntities.DARK_MATTER_SPORE.get(), level());
                spore.setPos(getX(), getY() + 1.5, getZ());
                spore.setTarget(target.position());
                level().addFreshEntity(spore);

                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                            getX(), getY() + 1.0, getZ(),
                            8, 0.5, 0.3, 0.5, 0.02);
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && tickCount % 20 == 0) {
            if (ProtectionUtils.hasClearMatterProtection(level(), blockPosition())) {
                this.hurt(this.damageSources().magic(), 2.0F);
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int shardCount = 1 + this.random.nextInt(2);
        this.spawnAtLocation(new ItemStack(ModItems.DARK_MATTER_SHARD.get(), shardCount));
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
