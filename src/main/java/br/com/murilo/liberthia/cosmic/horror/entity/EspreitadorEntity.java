package br.com.murilo.liberthia.cosmic.horror.entity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FloatToSurfaceOfFluid;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetPlayerLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;

import java.util.List;

/**
 * <b>O Espreitador</b> — terror movido a SmartBrainLib com a mecânica clássica
 * de "anjo que chora" (Weeping Angel): <b>congela completamente enquanto algum
 * player olha pra ele</b>, e <b>avança rápido (e cava blocos) quando ninguém
 * está vendo</b>. Pega você no momento em que vira as costas.
 */
public class EspreitadorEntity extends Monster implements SmartBrainOwner<EspreitadorEntity> {

    private int digCooldown = 0;

    public EspreitadorEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F); // r179: sobe degraus de 1 bloco SEM quebrar (só quebra parede)
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)   // rápido quando não observado
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {}

    @Override
    protected Brain.Provider<?> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    @Override
    protected void customServerAiStep() {
        if (!this.level().isClientSide && isWatched()) {
            // Observado → congela: não pensa, não anda, não cava.
            this.getNavigation().stop();
            this.setDeltaMovement(0, Math.min(this.getDeltaMovement().y, 0.0), 0);
            this.setXxa(0);
            this.setZza(0);
            return;
        }
        tickBrain(this);
        if (!this.level().isClientSide) {
            tickDigging();
        }
    }

    /** Algum player está olhando pra mim (no campo de visão + linha de visão)? */
    private boolean isWatched() {
        for (Player p : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(40.0), p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) {
            Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.5, 0)
                    .subtract(p.getEyePosition()).normalize();
            Vec3 look = p.getViewVector(1.0F);
            if (look.dot(toMe) > 0.55 && p.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    // r179: estado da escavação LENTA (progressiva, como um player na mão)
    private BlockPos digTarget = null;
    private float digProgress = 0F;
    private int digStage = -1;

    private void tickDigging() {
        LivingEntity target = this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > 22.0 * 22.0) { clearDig(); return; }

        boolean blockedHoriz = this.horizontalCollision;
        boolean targetAbove = target.getY() > this.getY() + 1.2;
        if (!blockedHoriz && !targetAbove) { clearDig(); return; }
        if (!(this.level() instanceof ServerLevel sl)) return;

        BlockPos pick = pickDigBlock(target, targetAbove, sl);
        if (pick == null) { clearDig(); return; } // 1-high / sem parede → sobe via maxUpStep, NÃO quebra
        if (!pick.equals(digTarget)) { clearDig(); digTarget = pick; }

        BlockState state = sl.getBlockState(digTarget);
        if (!breakable(state, sl, digTarget)) { clearDig(); return; }

        // r179: progride DEVAGAR (não instantâneo) — como um player quebrando na mão.
        float hardness = Math.max(0.2F, state.getDestroySpeed(sl, digTarget));
        digProgress += 1.0F / (hardness * 32F);
        int stage = (int) (digProgress * 10F);
        if (stage != digStage) {
            digStage = stage;
            sl.destroyBlockProgress(this.getId(), digTarget, Math.min(9, stage)); // rachadura
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (stage % 3 == 0) {
                sl.playSound(null, digTarget, state.getSoundType().getHitSound(),
                        SoundSource.HOSTILE, 0.5F, 0.6F + this.getRandom().nextFloat() * 0.2F);
            }
        }
        if (digProgress >= 1.0F) {
            sl.playSound(null, digTarget, state.getSoundType().getBreakSound(), SoundSource.HOSTILE, 0.7F, 0.7F);
            sl.destroyBlock(digTarget, true, this);
            clearDig();
            this.getJumpControl().jump();
        }
    }

    /**
     * r179: escolhe o bloco a cavar. Só cava PAREDE = bloco na ALTURA DA CABEÇA à
     * frente (obstáculo de 2+ blocos) ou coluna pra cima se o alvo está acima.
     * Obstáculo de 1 bloco de altura (só nos pés) é IGNORADO → sobe via maxUpStep.
     */
    private BlockPos pickDigBlock(LivingEntity target, boolean targetAbove, ServerLevel sl) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos base = this.blockPosition();
        BlockPos feet = base.relative(dir);
        BlockPos head = feet.above();
        if (targetAbove) {
            for (BlockPos p : new BlockPos[]{ head, base.above(2), feet.above(2) }) {
                if (breakable(sl.getBlockState(p), sl, p)) return p.immutable();
            }
            return null;
        }
        // PAREDE de verdade só se houver bloco na altura da CABEÇA à frente.
        if (sl.getBlockState(head).isAir()) return null;     // só-pés (1-high) → degrau, não quebra
        if (breakable(sl.getBlockState(head), sl, head)) return head.immutable();
        if (breakable(sl.getBlockState(feet), sl, feet)) return feet.immutable();
        return null;
    }

    private boolean breakable(BlockState state, ServerLevel sl, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.is(BlockTags.WITHER_IMMUNE)) return false;   // bedrock/barrier/etc
        if (state.hasBlockEntity()) return false;              // não destrói baús/máquinas
        float hardness = state.getDestroySpeed(sl, pos);
        return hardness >= 0 && hardness <= 5.0F;
    }

    private void clearDig() {
        if (digTarget != null && this.level() instanceof ServerLevel sl) {
            sl.destroyBlockProgress(this.getId(), digTarget, -1);
        }
        digTarget = null;
        digProgress = 0F;
        digStage = -1;
    }

    @Override
    public List<ExtendedSensor<EspreitadorEntity>> getSensors() {
        return ObjectArrayList.of(
                new NearbyPlayersSensor<>(),
                new NearbyLivingEntitySensor<>(),
                new HurtBySensor<>());
    }

    @Override
    public BrainActivityGroup<EspreitadorEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(
                new FloatToSurfaceOfFluid<>(),
                new LookAtTarget<>(),
                new MoveToWalkTarget<>());
    }

    @Override
    public BrainActivityGroup<EspreitadorEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<EspreitadorEntity>(
                        new TargetOrRetaliate<>(),
                        new SetPlayerLookTarget<>(),
                        new SetRandomLookTarget<>()),
                new OneRandomBehaviour<>(
                        new SetRandomWalkTarget<>(),
                        new Idle<>().runFor(e -> e.getRandom().nextInt(40) + 20)));
    }

    @Override
    public BrainActivityGroup<EspreitadorEntity> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new SetWalkTargetToAttackTarget<>(),
                new AnimatableMeleeAttack<>(0));
    }
}
