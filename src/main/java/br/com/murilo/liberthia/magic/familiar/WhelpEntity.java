package br.com.murilo.liberthia.magic.familiar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * v0.1.24 r109: <b>Whelp</b> — small dragonling familiar que sopra particles
 * decorativas (flame breath) sem dano. Player-companion ambient.
 *
 * <p>Spawnado via item futuro. Passivo. Sopra flame particles a cada 100t.
 */
public class WhelpEntity extends PathfinderMob {

    public WhelpEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        // Spark particles ambient
        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.SMALL_FLAME,
                    getX(), getY() + 0.6, getZ(),
                    1, 0.1, 0.1, 0.1, 0.01);
        }

        // Flame breath a cada 100t — visual cone forward 5 blocos
        if (tickCount % 100 == 0) {
            var look = getLookAngle();
            for (int i = 1; i <= 5; i++) {
                sl.sendParticles(ParticleTypes.FLAME,
                        getX() + look.x * i, getY() + 0.6, getZ() + look.z * i,
                        2, 0.2, 0.1, 0.2, 0.02);
            }
        }
    }
}
