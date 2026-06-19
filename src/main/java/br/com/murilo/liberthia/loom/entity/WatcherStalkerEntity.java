package br.com.murilo.liberthia.loom.entity;

import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.22 r36 (REWRITE): Watcher Stalker.
 *
 * <p><b>Fix r36:</b> antes setTarget(null) quando observado quebrava o
 * MoveTowardsTargetGoal. Agora apenas para via getNavigation().stop()
 * mantendo target — quando jogador vira, AI retoma instantâneo.
 */
public class WatcherStalkerEntity extends Monster {

    private int stepSoundCooldown = 0;
    private int pillarCooldown = 0;
    private boolean isObserved = false;

    public WatcherStalkerEntity(EntityType<? extends WatcherStalkerEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F); // sobe degraus de 1 bloco sem precisar pular
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.45)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        // r175: pode abrir portas pra te alcançar dentro de casa
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation gpn) {
            gpn.setCanOpenDoors(true);
            gpn.setCanPassDoors(true);
        }
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5D, true));
        // r175: espreita — vai até a última posição vista quando some de vista
        this.goalSelector.addGoal(2, new br.com.murilo.liberthia.cosmic.ai.StalkMemoryGoal(this, 1.35D));
        this.goalSelector.addGoal(3, new MoveTowardsTargetGoal(this, 1.5D, 32.0F));
        // r175: cerca por ângulos distintos (matilha de Watchers fecha o cerco)
        this.goalSelector.addGoal(4, new br.com.murilo.liberthia.cosmic.ai.SurroundTargetGoal(this, 1.25D, 6.0D));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(
                this, Player.class, true));
    }

    /**
     * r37 FIX DEFINITIVO via setNoAi() toggle.
     * Detecta observation ANTES de super.tick(), toggle NoAi → AI inteira
     * congelada quando observado. Impossível mexer.
     */
    @Override
    public void tick() {
        if (level().isClientSide) { super.tick(); return; }

        Player target = level().getNearestPlayer(this, 64.0);
        boolean shouldBeObserved = false;
        if (target != null && !target.isCreative() && !target.isSpectator()) {
            // Usa eye-to-eye vetores pra accuracy
            Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.5, 0)
                    .subtract(target.getEyePosition());
            double dot = toMe.normalize().dot(target.getLookAngle());
            // dot > 0.6 = cone ~53° (mais lenient que 0.85 anterior)
            shouldBeObserved = dot > 0.6 && target.distanceTo(this) < 48
                    && this.hasLineOfSight(target);
            if (this.getTarget() != target && !shouldBeObserved) {
                this.setTarget(target);
            }
        }

        // r37: setNoAi toggle — bloqueia AI inteira quando observado
        if (shouldBeObserved && !this.isNoAi()) {
            this.setNoAi(true);
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else if (!shouldBeObserved && this.isNoAi()) {
            this.setNoAi(false);
        }
        isObserved = shouldBeObserved;

        super.tick();

        if (target == null) return;
        if (isObserved) {
            // Mantém pose congelada após super.tick (defensive)
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            if (this.tickCount % 40 == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 80, 0, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, true));
                ((ServerLevel) level()).sendParticles(ParticleTypes.SOUL,
                        this.getX(), this.getY() + 1.5, this.getZ(),
                        4, 0.3, 0.3, 0.3, 0.05);
                level().playSound(null, this.blockPosition(),
                        ModSounds.WATCHER_BREATH.get(), SoundSource.HOSTILE, 0.6F, 1.0F);
            }
        } else {
            stepSoundCooldown--;
            double dist = target.distanceTo(this);
            if (stepSoundCooldown <= 0 && dist > 1.5 && dist < 25) {
                level().playSound(null, this.blockPosition(),
                        ModSounds.WATCHER_STEP.get(), SoundSource.HOSTILE,
                        1.2F, 0.8F + level().random.nextFloat() * 0.4F);
                stepSoundCooldown = 10 + level().random.nextInt(8);
            }
            // r173: se o player se escondeu empilhando blocos, o Watcher empilha
            // madeira embaixo de si pra subir atrás dele. Implacável.
            tryPillarUp(target);
        }
    }

    /**
     * r173: "torre de cerco" — quando o alvo está acima e quase em cima do Watcher
     * (player pilarou pra fugir), coloca uma tábua sob os pés e sobe 1 bloco.
     */
    private void tryPillarUp(Player target) {
        if (pillarCooldown > 0) { pillarCooldown--; return; }
        if (!this.onGround()) return;
        double dy = target.getY() - this.getY();
        if (dy < 1.2) return; // alvo não está significativamente acima
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        if (dx * dx + dz * dz > 9.0) return; // tem que estar ~em cima (≤3 blocos)

        BlockPos feet = this.blockPosition();
        BlockState at = level().getBlockState(feet);
        if (!at.canBeReplaced()) return;                 // só substitui ar/grama etc.
        if (!level().getBlockState(feet.above(3)).isAir()) return; // headroom ao subir

        level().setBlock(feet, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        this.setPos(this.getX(), feet.getY() + 1.0, this.getZ());
        this.setDeltaMovement(0, 0.12, 0);
        this.getNavigation().stop();
        pillarCooldown = 14;
        level().playSound(null, feet, SoundEvents.WOOD_PLACE, SoundSource.HOSTILE,
                0.9F, 0.6F + level().random.nextFloat() * 0.2F);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance e) {
        if (e.getEffect() == MobEffects.WEAKNESS) return false;
        return super.canBeAffected(e);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        return ok;
    }
}
