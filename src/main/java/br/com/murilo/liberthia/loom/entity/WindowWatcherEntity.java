package br.com.murilo.liberthia.loom.entity;

import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * r174: <b>Window Watcher</b> — a entidade que te observa pela janela.
 *
 * <p>Ao surgir, procura um <b>vidro</b> perto do alvo e se posiciona do
 * <b>outro lado</b> dele, mais longe do player — dando a impressão de algo
 * parado lá fora, encarando pela janela. Fica imóvel observando. Quando o
 * player a <b>encara por ~3s</b>, ela some (fumaça). Algumas variantes
 * <b>abrem uma porta</b> próxima e somem em seguida.
 */
public class WindowWatcherEntity extends Monster {

    private static final int MAX_LIFE = 2400;       // 2 min failsafe
    private static final int OBSERVE_VANISH = 60;   // 3s de observação → some

    private UUID targetPlayer = null;
    private int lifeTicks = 0;
    private int observedTicks = 0;
    private boolean positioned = false;
    private boolean doorVariant = false;
    private boolean doorOpened = false;
    private int vanishAt = -1;

    public WindowWatcherEntity(EntityType<? extends WindowWatcherEntity> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void startWatch(UUID player) {
        this.targetPlayer = player;
        this.doorVariant = this.random.nextInt(3) == 0; // 1/3 abre porta
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void checkDespawn() {
        if (targetPlayer == null) super.checkDespawn();
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource src, float amt) {
        // se atacada, simplesmente some — não luta
        if (!level().isClientSide) vanish();
        return false;
    }

    @Override public boolean canBeAffected(MobEffectInstance e) { return false; }

    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        lifeTicks++;
        if (lifeTicks >= MAX_LIFE) { vanish(); return; }

        Player target = (targetPlayer != null) ? level().getPlayerByUUID(targetPlayer)
                : level().getNearestPlayer(this, 48.0);
        if (target == null || !target.isAlive()) {
            if (targetPlayer == null) discard();
            return;
        }

        if (!positioned) {
            positioned = true;
            if (!positionAtWindow(target)) loiter(target);
        }

        // encara o player
        this.getLookControl().setLookAt(target.position());
        this.setYRot((float) angleTo(target));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        // variante que abre porta
        if (doorVariant && !doorOpened && lifeTicks > 40) {
            if (tryOpenNearbyDoor()) {
                doorOpened = true;
                vanishAt = lifeTicks + 40; // some logo depois
            }
        }
        if (vanishAt > 0 && lifeTicks >= vanishAt) { vanish(); return; }

        // observação: se o player encara a entidade, conta
        Vec3 toMe = this.position().add(0, 1.0, 0).subtract(target.getEyePosition()).normalize();
        double dot = toMe.dot(target.getLookAngle());
        boolean observed = dot > 0.6 && target.distanceTo(this) < 40 && this.hasLineOfSight(target);
        if (observed) {
            observedTicks++;
            if (observedTicks >= OBSERVE_VANISH) { vanish(); return; }
        } else {
            observedTicks = Math.max(0, observedTicks - 1);
        }
    }

    private double angleTo(Player target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        return Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
    }

    private void vanish() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 24, 0.3, 0.6, 0.3, 0.04);
            sl.playSound(null, blockPosition(), ModSounds.PERIPHERAL_WHISPER.get(),
                    SoundSource.HOSTILE, 0.3F, 0.5F);
        }
        discard();
    }

    /** Acha vidro perto do player e fica do outro lado (mais longe). */
    private boolean positionAtWindow(Player target) {
        if (!(level() instanceof ServerLevel sl)) return false;
        BlockPos pp = target.blockPosition();
        BlockPos bestGlass = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos bp = pp.offset(dx, dy, dz);
                    if (!isGlass(sl.getBlockState(bp))) continue;
                    double d = bp.distSqr(pp);
                    if (d > 4 && d < bestD) { bestD = d; bestGlass = bp.immutable(); }
                }
            }
        }
        if (bestGlass == null) return false;

        Vec3 g = Vec3.atCenterOf(bestGlass);
        Vec3 dir = new Vec3(g.x - target.getX(), 0, g.z - target.getZ());
        if (dir.lengthSqr() < 1e-4) return false;
        dir = dir.normalize();
        for (int ext = 2; ext <= 5; ext++) {
            int x = (int) Math.floor(g.x + dir.x * ext);
            int z = (int) Math.floor(g.z + dir.z * ext);
            BlockPos stand = findStanding(sl, x, bestGlass.getY(), z);
            if (stand != null) {
                moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
                spawnPoof(sl);
                return true;
            }
        }
        return false;
    }

    /** Sem janela: fica parado a 8–12 blocos observando. */
    private void loiter(Player target) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 16; i++) {
            double ang = sl.random.nextDouble() * Math.PI * 2;
            double dist = 8 + sl.random.nextDouble() * 4;
            int x = (int) (target.getX() + Math.cos(ang) * dist);
            int z = (int) (target.getZ() + Math.sin(ang) * dist);
            BlockPos stand = findStanding(sl, x, target.getBlockY(), z);
            if (stand != null) {
                moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
                spawnPoof(sl);
                return;
            }
        }
    }

    private void spawnPoof(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 6, 0.2, 0.4, 0.2, 0.01);
    }

    private boolean tryOpenNearbyDoor() {
        if (!(level() instanceof ServerLevel sl)) return false;
        BlockPos base = blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos bp = base.offset(dx, dy, dz);
                    BlockState st = sl.getBlockState(bp);
                    if (st.getBlock() instanceof DoorBlock door) {
                        door.setOpen(this, sl, st, bp, true);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BlockPos findStanding(ServerLevel sl, int x, int y0, int z) {
        for (int dy = 3; dy >= -4; dy--) {
            int y = y0 + dy;
            BlockPos feet = new BlockPos(x, y, z);
            BlockState floor = sl.getBlockState(feet.below());
            if (floor.blocksMotion() && sl.getBlockState(feet).isAir() && sl.getBlockState(feet.above()).isAir()) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isGlass(BlockState state) {
        return state.getBlock() instanceof AbstractGlassBlock
                || state.getBlock() instanceof IronBarsBlock; // panes/bars
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putBoolean("Door", doorVariant);
        if (targetPlayer != null) tag.putUUID("Target", targetPlayer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        doorVariant = tag.getBoolean("Door");
        if (tag.hasUUID("Target")) targetPlayer = tag.getUUID("Target");
        positioned = true; // já posicionada se carregada do save
    }
}
