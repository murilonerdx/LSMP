package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SporeSpitterEntity extends Monster {

    private int sporeAttackCooldown = 0;

    public SporeSpitterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level().isClientSide) {
            return;
        }

        sporeAttackCooldown--;
        if (sporeAttackCooldown <= 0) {
            sporeAttackCooldown = 40 + this.random.nextInt(41); // 40-80 ticks

            List<Player> nearbyPlayers = level().getEntitiesOfClass(
                    Player.class,
                    new AABB(blockPosition()).inflate(12.0)
            );

            if (!nearbyPlayers.isEmpty()) {
                Player target = nearbyPlayers.get(0);
                DarkMatterSporeEntity spore = new DarkMatterSporeEntity(
                        ModEntities.DARK_MATTER_SPORE.get(), level());
                spore.setPos(getX(), getY() + 0.4, getZ());
                spore.setTarget(target.position());
                level().addFreshEntity(spore);

                level().playSound(null, blockPosition(),
                        ModSounds.DARK_PULSE.get(), SoundSource.HOSTILE,
                        1.0F, 1.2F);
            }
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }
}
