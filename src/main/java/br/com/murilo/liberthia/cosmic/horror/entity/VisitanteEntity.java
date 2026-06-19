package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * <b>O Visitante</b> — entidade de terror psicológico. <b>Nunca ataca</b>: o
 * terror vem de estar sempre perto, mas nunca de forma clara.
 *
 * <p>r180b (request "O Visitante"): ganhou <b>modos</b> dirigidos pelo
 * {@link br.com.murilo.liberthia.cosmic.visitante.VisitantePresenceManager}:
 * <ul>
 *   <li>{@link Mode#STALK} — clássico Herobrine: encara e some quando notado
 *       (comportamento original; é o default p/ ovo e spawns naturais).</li>
 *   <li>{@link Mode#PASSING} — "A Figura que Passa": atravessa o campo de visão
 *       ao longe; some se você chega perto (&lt;6b) ou ao terminar a travessia.
 *       NÃO some só de olhar (você deve vê-la passar).</li>
 *   <li>{@link Mode#PEEK} — para, <b>vira lentamente a cabeça</b>, te encara e
 *       desaparece.</li>
 *   <li>{@link Mode#CHASE} — clímax "RUNN!!": brilha vermelho, persegue por uns
 *       segundos (sem causar dano) e some.</li>
 * </ul>
 */
public class VisitanteEntity extends Monster {

    public enum Mode { STALK, PASSING, PEEK, CHASE }

    private static final DustParticleOptions RED_GLOW =
            new DustParticleOptions(new Vector3f(0.85F, 0.02F, 0.02F), 1.6F);

    /** r180: spawnado por OVO → fica persistente (não some), pra dar pra ver/testar. */
    private boolean manualSpawn = false;
    private Mode mode = Mode.STALK;
    private int modeAge = 0;
    private boolean hasCross = false;
    private double crossTx, crossTz;
    private int navCooldown = 0;

    public VisitanteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    // ──────────── API para o PresenceManager ────────────
    public void setMode(Mode m) { this.mode = m; this.modeAge = 0; }
    public Mode getMode() { return this.mode; }
    public void setCrossTarget(double x, double z) { this.crossTx = x; this.crossTz = z; this.hasCross = true; }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance diff,
            net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData data,
            net.minecraft.nbt.CompoundTag tag) {
        if (reason == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG) this.manualSpawn = true;
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("ManualSpawn", manualSpawn);
        t.putByte("VMode", (byte) mode.ordinal());
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
        super.readAdditionalSaveData(t);
        manualSpawn = t.getBoolean("ManualSpawn");
        int mi = t.getByte("VMode");
        if (mi >= 0 && mi < Mode.values().length) mode = Mode.values()[mi];
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)   // r180b: precisa >0 p/ PASSING/CHASE
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;
        this.modeAge++;
        if (navCooldown > 0) navCooldown--;

        switch (mode) {
            case PASSING -> tickPassing();
            case PEEK -> tickPeek();
            case CHASE -> tickChase();
            default -> tickStalk();
        }
    }

    /** Clássico Herobrine (comportamento original). */
    private void tickStalk() {
        Player p = this.level().getNearestPlayer(this, 48);
        if (p == null) {
            if (!manualSpawn && this.tickCount > 100) this.discard();
            return;
        }
        faceInstant(p.getX(), p.getZ());

        Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.5, 0)
                .subtract(p.getEyePosition()).normalize();
        Vec3 look = p.getViewVector(1.0F);
        if (!manualSpawn && this.tickCount > 120
                && look.dot(toMe) > 0.5 && p.distanceTo(this) < 40.0 && p.hasLineOfSight(this)) {
            playPoof(0.4F);
            this.discard();
            return;
        }
        if (!manualSpawn && this.tickCount > 600) this.discard();
    }

    /** A Figura que Passa — atravessa ao longe; some se chegam perto. */
    private void tickPassing() {
        Player p = this.level().getNearestPlayer(this, 64);
        if (p == null || p.distanceTo(this) < 6.0 || modeAge > 160) { this.discard(); return; }
        if (hasCross) {
            faceInstant(crossTx, crossTz);
            if (navCooldown == 0) {
                this.getNavigation().moveTo(crossTx, this.getY(), crossTz, 1.0);
                navCooldown = 20;
            }
            double dx = crossTx - this.getX(), dz = crossTz - this.getZ();
            if (dx * dx + dz * dz < 2.25) this.discard(); // chegou ao outro lado
        }
    }

    /** Para, vira lentamente a cabeça, te encara e some. */
    private void tickPeek() {
        Player p = this.level().getNearestPlayer(this, 64);
        if (p == null || p.distanceTo(this) < 6.0 || modeAge > 80) { playPoof(0.4F); this.discard(); return; }
        if (modeAge >= 18) {
            // vira a cabeça devagar pro player
            double dx = p.getX() - this.getX(), dz = p.getZ() - this.getZ();
            float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float ny = approachYaw(this.getYRot(), target, 6.0F);
            this.setYRot(ny); this.setYHeadRot(ny); this.setYBodyRot(ny);
        }
    }

    /** Clímax — brilha vermelho e persegue (sem dano), depois some. */
    private void tickChase() {
        this.setGlowingTag(true);
        Player p = this.level().getNearestPlayer(this, 64);
        if (p == null || modeAge > 90) {
            if (this.level() instanceof ServerLevel sl)
                sl.sendParticles(RED_GLOW, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.8, 0.4, 0.0);
            playPoof(0.7F);
            this.discard();
            return;
        }
        faceInstant(p.getX(), p.getZ());
        if (navCooldown == 0) { this.getNavigation().moveTo(p, 1.45); navCooldown = 8; }
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(RED_GLOW, this.getX(), this.getY() + 1.0, this.getZ(), 4, 0.3, 0.6, 0.3, 0.0);
        }
    }

    // ──────────── helpers ────────────
    private void faceInstant(double tx, double tz) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - this.getX()), tz - this.getZ()));
        this.setYRot(yaw); this.setYHeadRot(yaw); this.setYBodyRot(yaw);
    }

    private static float approachYaw(float cur, float target, float maxStep) {
        float d = Mth.wrapDegrees(target - cur);
        d = Mth.clamp(d, -maxStep, maxStep);
        return cur + d;
    }

    private void playPoof(float vol) {
        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.blockPosition(), SoundEvents.AMBIENT_CAVE.value(),
                    SoundSource.HOSTILE, vol, 0.4F);
        }
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity e) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // CHASE é intocável de perto (deixa o clímax rolar); /kill e void ainda funcionam.
        if (mode == Mode.CHASE) {
            if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity) return false;
            return super.hurt(source, amount);
        }
        this.discard();
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
