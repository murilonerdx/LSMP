package br.com.murilo.liberthia.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * A humanoid entity rendered as a real player (PlayerModel + player skin texture).
 * Used by WorkerCloneManager. No AI — positions are driven from the manager.
 */
public class ClonePlayerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ClonePlayerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(ClonePlayerEntity.class, EntityDataSerializers.STRING);

    public ClonePlayerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setNoGravity(true);
    }

    @Override
    public void travel(Vec3 movementInput) {
        // block all physics movement
    }

    @Override
    public void aiStep() {
        // block AI step entirely
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        // block all external movement
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(OWNER_NAME, "");
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

    @Override
    protected void registerGoals() {
        // no AI — driven externally
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUuid")) setOwnerUuid(tag.getUUID("OwnerUuid"));
        if (tag.contains("OwnerName")) setOwnerName(tag.getString("OwnerName"));
    }
}
