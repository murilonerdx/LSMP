package br.com.murilo.liberthia.loom.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * v0.1.22 r33: <b>Dimensional Worm</b> — verme parasita spawnado pela
 * Dimensional Infection effect.
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Persegue player infectado, atacando pra drenar HP</li>
 *   <li>Cada hit: 1 dmg + heals self 2 (vampírico)</li>
 *   <li>Solta partículas cinzas que CEGAM players próximos (passive aura)</li>
 *   <li>HP baixo (4), morre fácil mas spawna em grupos</li>
 *   <li>Despawna após 30s</li>
 * </ul>
 */
public class DimensionalWormEntity extends Monster {

    private int lifetimeTicks = 0;
    private static final int MAX_LIFETIME = 600;

    public DimensionalWormEntity(EntityType<? extends DimensionalWormEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.40)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.5D, true));
        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        lifetimeTicks++;
        if (lifetimeTicks >= MAX_LIFETIME) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(),
                        10, 0.3, 0.3, 0.3, 0.02);
            }
            this.discard();
            return;
        }
        // Particles cinzas (aura blinding)
        if (level() instanceof ServerLevel sl && tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 0.4, getZ(),
                    5, 0.5, 0.3, 0.5, 0.02);
            // Aura: blinda players num raio 4
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4))) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            this.heal(2.0F); // vampírico
            level().playSound(null, this.blockPosition(),
                    SoundEvents.SILVERFISH_HURT, SoundSource.HOSTILE, 0.5F, 1.8F);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(), target.getY() + 1, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.1);
            }
        }
        return ok;
    }
}
