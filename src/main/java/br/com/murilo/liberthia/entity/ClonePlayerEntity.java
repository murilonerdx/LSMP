package br.com.murilo.liberthia.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Humanoid entity rendered as a real player. Used pelo WorkerCloneManager.
 *
 * <p>r161: Adicionado <b>Observer Mode</b> — quando ativo, o clone:
 * <ul>
 *   <li>Só se move quando <b>não está sendo observado</b> pelo player</li>
 *   <li>Causa <b>dano enorme</b> ao tocar o player (8 hearts/tick em contato)</li>
 *   <li>Quando atacado por algo que <b>não é machado</b>, teleporta atrás do player
 *       a 10 blocos de distância e fica invulnerável àquele dano</li>
 *   <li>Só morre com <b>machado</b> (axe item)</li>
 * </ul>
 */
public class ClonePlayerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ClonePlayerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(ClonePlayerEntity.class, EntityDataSerializers.STRING);
    /** r161: Marca se este clone é um "Observer". */
    private static final EntityDataAccessor<Boolean> OBSERVER_MODE =
            SynchedEntityData.defineId(ClonePlayerEntity.class, EntityDataSerializers.BOOLEAN);

    public ClonePlayerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setNoGravity(true);
    }

    @Override
    public void travel(Vec3 movementInput) {
        // block default physics — driven externally OR by observer tick
    }

    @Override
    public void aiStep() {
        // r161: Observer behavior — só "anda" quando não está sendo observado
        if (!this.level().isClientSide && isObserverMode()) {
            tickObserver();
        }
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // block external movement (except our own observer step)
        if (isObserverMode() && type == MoverType.SELF) {
            super.move(type, movement);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0);  // 8 hearts on touch
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(OBSERVER_MODE, false);
    }

    public void setOwnerUuid(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerUuid() {
        return this.entityData.get(OWNER_UUID).orElse(this.getUUID());
    }

    public void setOwnerName(String name) {
        this.entityData.set(OWNER_NAME, name);
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public boolean isObserverMode() {
        return this.entityData.get(OBSERVER_MODE);
    }

    public void setObserverMode(boolean v) {
        this.entityData.set(OBSERVER_MODE, v);
        if (v) {
            this.setNoAi(false);          // permite tick
            this.setNoGravity(false);
            this.setInvulnerable(false);   // pode ser danificado por axe
        }
    }

    @Override
    protected void registerGoals() {
        // no AI goals — driven externally
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_UUID).ifPresent(u -> tag.putUUID("OwnerUuid", u));
        tag.putString("OwnerName", getOwnerName());
        tag.putBoolean("ObserverMode", isObserverMode());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUuid")) setOwnerUuid(tag.getUUID("OwnerUuid"));
        if (tag.contains("OwnerName")) setOwnerName(tag.getString("OwnerName"));
        if (tag.contains("ObserverMode")) setObserverMode(tag.getBoolean("ObserverMode"));
    }

    // ─── r161: Observer behavior ───────────────────────────────────────────

    /**
     * Logic: every tick, scan nearby players. If at least one is LOOKING AT this
     * clone (within FOV cone), freeze. Otherwise, slowly walk toward closest.
     * On contact, damage. On hit by non-axe damage, teleport behind closest player.
     */
    private void tickObserver() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Player target = findClosestPlayer(32);
        if (target == null) return;

        boolean beingWatched = isWatchedBy(target);
        if (!beingWatched) {
            // Walk toward player slowly
            Vec3 dir = target.position().subtract(this.position()).normalize().scale(0.15);
            this.setDeltaMovement(dir.x, this.getDeltaMovement().y - 0.05, dir.z);
            super.move(MoverType.SELF, this.getDeltaMovement());
            // Face target
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90F;
            this.setYRot(yaw);
            this.yHeadRot = yaw;
        } else {
            // Static when watched — only Y from gravity
            this.setDeltaMovement(0, this.getDeltaMovement().y - 0.05, 0);
            super.move(MoverType.SELF, this.getDeltaMovement());
        }

        // Contact damage
        if (this.distanceToSqr(target) < 2.0) {
            target.hurt(this.damageSources().mobAttack(this), 16.0F);
            // Knockback player
            Vec3 push = target.position().subtract(this.position()).normalize().scale(1.2);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.4, push.z));
        }
    }

    private Player findClosestPlayer(double radius) {
        List<Player> nearby = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(radius));
        Player closest = null;
        double bestD = radius * radius;
        for (Player p : nearby) {
            if (p.isSpectator() || p.isCreative()) continue;
            double d = this.distanceToSqr(p);
            if (d < bestD) { bestD = d; closest = p; }
        }
        return closest;
    }

    /** True se o player está olhando para este clone (FOV ~45°). */
    private boolean isWatchedBy(Player p) {
        Vec3 lookDir = p.getLookAngle().normalize();
        Vec3 toClone = this.position().add(0, this.getBbHeight() * 0.5, 0)
                .subtract(p.getEyePosition()).normalize();
        double dot = lookDir.dot(toClone);
        // dot > 0.7 ≈ within 45° cone
        if (dot < 0.7) return false;
        // Line of sight check (simple distance — could add raycast for occlusion)
        return p.distanceToSqr(this) < 32 * 32;
    }

    /**
     * r161: Sobrescreve hurt — só axe causa dano. Tudo else faz o clone
     * teleportar atrás do attacker e ficar bem longe.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isObserverMode()) return super.hurt(source, amount);

        boolean isAxe = false;
        if (source.getDirectEntity() instanceof Player p) {
            isAxe = p.getMainHandItem().getItem() instanceof AxeItem;
        }
        if (isAxe) {
            // Normal damage path — only axe kills
            return super.hurt(source, amount);
        }
        // Teleport behind attacker (if it's a LivingEntity)
        if (source.getEntity() instanceof LivingEntity attacker) {
            teleportBehind(attacker, 10);
            // Visual + sound
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        this.getX(), this.getY() + 1, this.getZ(),
                        30, 0.5, 1.0, 0.5, 0.3);
                sl.playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.5F, 0.7F);
            }
        }
        return false;  // damage ignored
    }

    private void teleportBehind(LivingEntity target, double distance) {
        // Direction opposite to where target is looking
        Vec3 lookDir = target.getLookAngle().normalize();
        // Behind = subtract lookDir × distance from target pos
        double tx = target.getX() - lookDir.x * distance;
        double ty = target.getY();
        double tz = target.getZ() - lookDir.z * distance;
        this.teleportTo(tx, ty, tz);
        // Face the target after teleport
        double dx = target.getX() - tx;
        double dz = target.getZ() - tz;
        float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90F;
        this.setYRot(yaw);
        this.yHeadRot = yaw;
    }
}
