package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * r178: <b>O Vizinho</b> — nunca ataca. Fica te observando de longe e, quando você NÃO
 * está olhando, se aproxima devagar. Se você o encara, ele congela e devolve o olhar.
 * Se chegar perto demais sem você ver, simplesmente... some. Presença pura, sem violência.
 */
public class NeighborEntity extends Monster {

    public NeighborEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 36.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 36.0);
        if (p == null || p.isCreative() || p.isSpectator()) return;

        boolean watched = isWatchedBy(p);
        if (watched) {
            // encarado → congela e devolve o olhar
            this.getNavigation().stop();
            this.setXxa(0); this.setZza(0);
            this.getLookControl().setLookAt(p);
            return;
        }

        // não observado → se aproxima devagar
        this.getNavigation().moveTo(p, 0.85);

        // perto demais e sem ser visto → desaparece
        if (this.distanceTo(p) < 2.6) {
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0, this.getZ(),
                        12, 0.25, 0.6, 0.25, 0.02);
                sl.playSound(null, this.blockPosition(), ModSounds.WATCHER_STEP.get(),
                        SoundSource.HOSTILE, 0.6F, 0.5F);
            }
            this.discard();
        }
    }

    private boolean isWatchedBy(Player p) {
        Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.6, 0).subtract(p.getEyePosition());
        if (toMe.lengthSqr() < 1.0e-4) return false;
        return toMe.normalize().dot(p.getLookAngle()) > 0.55 && this.hasLineOfSight(p);
    }

    /** r180 #71: O Vizinho NUNCA dá dano — é só um mob que segue e assusta. */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false;
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean canBeAffected(MobEffectInstance e) { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
}
