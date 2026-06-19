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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
 * <b>O Caçador</b> — piloto do <b>SmartBrainLib</b>. Stalker movido por
 * sensors/memories/behaviours em vez de Goals: enxerga players e seres por
 * perto, persegue o alvo de forma inteligente, revida quem o machuca e
 * vagueia/observa quando ocioso. Serve de molde pra migrar os demais monstros.
 */
public class CacadorEntity extends Monster implements SmartBrainOwner<CacadorEntity> {

    public CacadorEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    // SmartBrainLib substitui as Goals da vanilla — não registramos nenhuma.
    @Override
    protected void registerGoals() {}

    @Override
    protected Brain.Provider<?> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    // r175: escavação PROGRESSIVA (igual player quebrando na mão, não instantâneo)
    private BlockPos digTarget = null;
    private float digProgress = 0F;
    private int digStage = -1;

    @Override
    protected void customServerAiStep() {
        tickBrain(this);
        if (!this.level().isClientSide) {
            tickDigging();
            tickGazeFreeze();
        }
    }

    /**
     * r175: <b>Reagir ao olhar (Anjo Chorão)</b>. Se um player está te encarando
     * (cone + linha de visão), o Caçador congela — para de andar e fica imóvel,
     * te encarando de volta. Quando você desvia, ele volta a perseguir.
     */
    private void tickGazeFreeze() {
        Player p = this.level().getNearestPlayer(this, 24.0);
        if (p == null || p.isCreative() || p.isSpectator()) return;
        if (this.distanceToSqr(p) > 24.0 * 24.0) return;
        Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.6, 0).subtract(p.getEyePosition());
        if (toMe.lengthSqr() < 1.0e-4) return;
        double dot = toMe.normalize().dot(p.getLookAngle());
        if (dot > 0.6 && this.hasLineOfSight(p)) {
            this.getNavigation().stop();
            this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            this.setXxa(0);
            this.setZza(0);
            this.getLookControl().setLookAt(p, 30F, 30F);
        }
    }

    /**
     * Inteligência de escavação: se o player se fecha atrás de blocos (parede,
     * torre), o Caçador quebra o que estiver no caminho pra alcançá-lo — inclusive
     * cavando pra cima quando o alvo está mais alto. Não toca em blocos
     * inquebráveis/duros demais (bedrock/obsidian) nem em block-entities (baús).
     */
    private void tickDigging() {
        LivingEntity target = this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > 20.0 * 20.0) { clearDig(); return; }

        boolean blockedHoriz = this.horizontalCollision;
        boolean targetAbove = target.getY() > this.getY() + 1.2;
        if (!blockedHoriz && !targetAbove) { clearDig(); return; }
        if (!(this.level() instanceof ServerLevel sl)) return;

        BlockPos pick = pickDigBlock(target, targetAbove, sl);
        if (pick == null) { clearDig(); return; }
        if (!pick.equals(digTarget)) { clearDig(); digTarget = pick; }

        BlockState state = sl.getBlockState(digTarget);
        if (!breakable(state, sl, digTarget)) { clearDig(); return; }

        // r175: progride como um player quebrando NA MÃO (não instantâneo).
        float hardness = Math.max(0.2F, state.getDestroySpeed(sl, digTarget));
        digProgress += 1.0F / (hardness * 32F);  // ~1.6s por bloco de pedra

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

    private BlockPos pickDigBlock(LivingEntity target, boolean targetAbove, ServerLevel sl) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        Direction dir = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos base = this.blockPosition();
        BlockPos[] order = targetAbove
                ? new BlockPos[]{ base.relative(dir), base.relative(dir).above(), base.above(2), base.relative(dir).above(2) }
                : new BlockPos[]{ base.relative(dir), base.relative(dir).above() };
        for (BlockPos p : order) {
            if (breakable(sl.getBlockState(p), sl, p)) return p.immutable();
        }
        return null;
    }

    private boolean breakable(BlockState state, ServerLevel sl, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.is(BlockTags.WITHER_IMMUNE)) return false;   // bedrock/barrier/etc
        if (state.hasBlockEntity()) return false;              // não destrói baús/máquinas
        float hardness = state.getDestroySpeed(sl, pos);
        return hardness >= 0 && hardness <= 5.0F;              // nada inquebrável/duro demais
    }

    /** Limpa o alvo e apaga a animação de rachadura. */
    private void clearDig() {
        if (digTarget != null && this.level() instanceof ServerLevel sl) {
            sl.destroyBlockProgress(this.getId(), digTarget, -1);
        }
        digTarget = null;
        digProgress = 0F;
        digStage = -1;
    }

    @Override
    public List<ExtendedSensor<CacadorEntity>> getSensors() {
        return ObjectArrayList.of(
                new NearbyPlayersSensor<>(),
                new NearbyLivingEntitySensor<>(),
                new HurtBySensor<>());
    }

    @Override
    public BrainActivityGroup<CacadorEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(
                new FloatToSurfaceOfFluid<>(),
                new LookAtTarget<>(),
                new MoveToWalkTarget<>());
    }

    @Override
    public BrainActivityGroup<CacadorEntity> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FirstApplicableBehaviour<CacadorEntity>(
                        new TargetOrRetaliate<>(),
                        new SetPlayerLookTarget<>(),
                        new SetRandomLookTarget<>()),
                new OneRandomBehaviour<>(
                        new SetRandomWalkTarget<>(),
                        new Idle<>().runFor(e -> e.getRandom().nextInt(60) + 30)));
    }

    @Override
    public BrainActivityGroup<CacadorEntity> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<>(),
                new SetWalkTargetToAttackTarget<>(),
                new AnimatableMeleeAttack<>(0));
    }
}
