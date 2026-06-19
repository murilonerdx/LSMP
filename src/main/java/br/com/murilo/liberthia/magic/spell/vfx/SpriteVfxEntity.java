package br.com.murilo.liberthia.magic.spell.vfx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * r166: <b>SpriteVfxEntity</b> — visual-only entity that renders one effect
 * from {@link SpriteVfxRegistry}.
 *
 * <p>Server side: ticks down lifetime, removes itself when expired.
 * Client side: renderer flipbooks through frames based on age.
 *
 * <p>Synced data:
 * <ul>
 *   <li>DATA_TYPE — ordinal of {@link SpriteVfxRegistry.Type}</li>
 *   <li>DATA_LIFE — ticks remaining</li>
 *   <li>DATA_SCALE — extra scale multiplier (atop the type's base scale)</li>
 * </ul>
 *
 * <p>Not collidable, not pickable. Pure VFX.
 */
public class SpriteVfxEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(SpriteVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(SpriteVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(SpriteVfxEntity.class, EntityDataSerializers.FLOAT);
    // r167: pack selector (0 = pack-1 SpriteVfxRegistry, 1 = pack-2 Pack2VfxCatalog)
    private static final EntityDataAccessor<Byte> DATA_PACK =
            SynchedEntityData.defineId(SpriteVfxEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_COLOR =
            SynchedEntityData.defineId(SpriteVfxEntity.class, EntityDataSerializers.BYTE);

    public static final byte PACK_1 = 0;
    public static final byte PACK_2 = 1;

    private UUID ownerUuid;

    public SpriteVfxEntity(EntityType<SpriteVfxEntity> type, Level level) {
        super(type, level);
    }

    // ── Pack-1 constructors ──────────────────────────────────────────────────

    public SpriteVfxEntity(Level level, LivingEntity owner, Vec3 pos, SpriteVfxRegistry.Type type) {
        this(level, owner, pos, type, 1.0F);
    }

    public SpriteVfxEntity(Level level, LivingEntity owner, Vec3 pos,
                            SpriteVfxRegistry.Type type, float scaleMul) {
        this(br.com.murilo.liberthia.registry.ModEntities.SPRITE_VFX.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.entityData.set(DATA_TYPE, type.ordinal());
        this.entityData.set(DATA_LIFE, type.effect.lifetimeTicks);
        this.entityData.set(DATA_SCALE, scaleMul);
        this.entityData.set(DATA_PACK, PACK_1);
        this.entityData.set(DATA_COLOR, (byte) 0);
    }

    // ── Pack-2 constructor ───────────────────────────────────────────────────

    public SpriteVfxEntity(Level level, LivingEntity owner, Vec3 pos,
                            Pack2VfxCatalog.Type type, int colorIdx, float scaleMul) {
        this(br.com.murilo.liberthia.registry.ModEntities.SPRITE_VFX.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.entityData.set(DATA_TYPE, type.ordinal());
        this.entityData.set(DATA_LIFE, type.lifetimeTicks);
        this.entityData.set(DATA_SCALE, scaleMul);
        this.entityData.set(DATA_PACK, PACK_2);
        this.entityData.set(DATA_COLOR, (byte) Math.max(0, Math.min(Pack2VfxCatalog.COLORS - 1, colorIdx)));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TYPE, 0);
        this.entityData.define(DATA_LIFE, 40);
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_PACK, PACK_1);
        this.entityData.define(DATA_COLOR, (byte) 0);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public byte getPack()      { return entityData.get(DATA_PACK); }
    public int getColorIdx()   { return entityData.get(DATA_COLOR) & 0xFF; }
    public int getTypeOrdinal() { return entityData.get(DATA_TYPE); }
    public int getLifeRemaining() { return entityData.get(DATA_LIFE); }
    public float getScaleMul()    { return entityData.get(DATA_SCALE); }

    /** Pack-1 type accessor — only valid if {@link #getPack()} == PACK_1. */
    public SpriteVfxRegistry.Type getVfxType() {
        return SpriteVfxRegistry.Type.fromOrdinalSafe(entityData.get(DATA_TYPE));
    }

    /** Pack-2 type accessor — only valid if {@link #getPack()} == PACK_2. */
    public Pack2VfxCatalog.Type getPack2Type() {
        return Pack2VfxCatalog.Type.fromOrdinalSafe(entityData.get(DATA_TYPE));
    }

    public int getTotalLifetime() {
        if (getPack() == PACK_2) return getPack2Type().lifetimeTicks;
        return getVfxType().effect.lifetimeTicks;
    }

    /** 0..1 progress through the configured lifetime. */
    public float getProgress() {
        int total = getTotalLifetime();
        int rem = entityData.get(DATA_LIFE);
        return 1F - (rem / (float) total);
    }

    /** Total ticks this entity has been alive (server + client agree on this). */
    public int getAge() {
        return getTotalLifetime() - entityData.get(DATA_LIFE);
    }

    @Override
    public void tick() {
        super.tick();
        int life = entityData.get(DATA_LIFE);
        if (life <= 0) {
            if (!level().isClientSide()) discard();
            return;
        }
        entityData.set(DATA_LIFE, life - 1);
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("owner"))   this.ownerUuid = tag.getUUID("owner");
        if (tag.contains("type"))   this.entityData.set(DATA_TYPE, tag.getInt("type"));
        if (tag.contains("life"))   this.entityData.set(DATA_LIFE, tag.getInt("life"));
        if (tag.contains("scale"))  this.entityData.set(DATA_SCALE, tag.getFloat("scale"));
        if (tag.contains("pack"))   this.entityData.set(DATA_PACK, tag.getByte("pack"));
        if (tag.contains("color"))  this.entityData.set(DATA_COLOR, tag.getByte("color"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("owner", ownerUuid);
        tag.putInt("type",   entityData.get(DATA_TYPE));
        tag.putInt("life",   entityData.get(DATA_LIFE));
        tag.putFloat("scale", entityData.get(DATA_SCALE));
        tag.putByte("pack",  entityData.get(DATA_PACK));
        tag.putByte("color", entityData.get(DATA_COLOR));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable()                  { return false; }
    @Override public boolean canBeCollidedWith()           { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double sqrDist) { return sqrDist < 6400.0; }
}
