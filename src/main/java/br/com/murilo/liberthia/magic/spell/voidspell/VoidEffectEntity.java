package br.com.murilo.liberthia.magic.spell.voidspell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * r165: <b>VoidEffectEntity</b> — entity genérica que renderiza um efeito de
 * sprite-animado para os feitiços do vazio.
 *
 * <p>O tipo é definido por um inteiro sincronizado (DATA_TYPE):
 * <ul>
 *   <li>0 = MIND_SPIKE — spike vertical em cima do alvo</li>
 *   <li>1 = SOUL_TEAR — aura swirling no alvo, drena alma</li>
 *   <li>2 = MADNESS_WAVE — onda horizontal expandindo do caster</li>
 *   <li>3 = COSMIC_VOID — vortex vertical no ponto</li>
 *   <li>4 = ELDRITCH_BLAST — burst de impacto curto</li>
 * </ul>
 *
 * <p>Lifetime e particles variam por tipo. Renderer (client-side) decide qual
 * sprite array carregar baseado no DATA_TYPE.
 */
public class VoidEffectEntity extends Entity {

    public static final int TYPE_MIND_SPIKE     = 0;
    public static final int TYPE_SOUL_TEAR      = 1;
    public static final int TYPE_MADNESS_WAVE   = 2;
    public static final int TYPE_COSMIC_VOID    = 3;
    public static final int TYPE_ELDRITCH_BLAST = 4;

    /** Default lifetime per type (ticks). */
    private static final int[] LIFETIME = { 30, 80, 40, 100, 15 };

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(VoidEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(VoidEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(VoidEffectEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUuid;

    public VoidEffectEntity(EntityType<VoidEffectEntity> type, Level level) {
        super(type, level);
    }

    public VoidEffectEntity(Level level, LivingEntity owner, Vec3 pos, int effectType, float scale) {
        this(br.com.murilo.liberthia.registry.ModEntities.VOID_EFFECT.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.entityData.set(DATA_TYPE, effectType);
        this.entityData.set(DATA_LIFE, totalLifetime(effectType));
        this.entityData.set(DATA_SCALE, scale);
    }

    public static int totalLifetime(int type) {
        if (type < 0 || type >= LIFETIME.length) return 40;
        return LIFETIME[type];
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TYPE, TYPE_MIND_SPIKE);
        this.entityData.define(DATA_LIFE, 30);
        this.entityData.define(DATA_SCALE, 1.0f);
    }

    public int getEffectType() { return entityData.get(DATA_TYPE); }
    public int getLifeRemaining() { return entityData.get(DATA_LIFE); }
    public float getScale()      { return entityData.get(DATA_SCALE); }
    public int getTotalLifetime() { return totalLifetime(getEffectType()); }

    /** 0..1 progress through lifetime — used by renderer to pick frame. */
    public float getProgress() {
        int total = getTotalLifetime();
        int remaining = entityData.get(DATA_LIFE);
        return 1f - (remaining / (float) total);
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

        // Server-side: type-specific particle emission for ambient effect
        if (level() instanceof ServerLevel sl) {
            int type = entityData.get(DATA_TYPE);
            int age = getTotalLifetime() - life;
            switch (type) {
                case TYPE_MIND_SPIKE   -> tickMindSpike(sl, age);
                case TYPE_SOUL_TEAR    -> tickSoulTear(sl, age);
                case TYPE_MADNESS_WAVE -> tickMadnessWave(sl, age);
                case TYPE_COSMIC_VOID  -> tickCosmicVoid(sl, age);
                case TYPE_ELDRITCH_BLAST -> tickEldritchBlast(sl, age);
            }
        }
    }

    // ── Per-type tick (custom particle emission using VOID_LEAK + DIMENSIONAL_CRACK) ───────

    private void tickMindSpike(ServerLevel sl, int age) {
        if (age % 3 == 0) {
            Vec3 c = position();
            sl.sendParticles(br.com.murilo.liberthia.registry.ModParticles.VOID_LEAK.get(),
                    c.x + (Math.random() - 0.5) * 0.4,
                    c.y + 1.5 + Math.random() * 0.5,
                    c.z + (Math.random() - 0.5) * 0.4,
                    2, 0.05, 0.1, 0.05, 0.02);
        }
    }

    private void tickSoulTear(ServerLevel sl, int age) {
        if (age % 4 == 0) {
            Vec3 c = position();
            double a = (age * 0.3) % (Math.PI * 2);
            double r = 0.7;
            sl.sendParticles(br.com.murilo.liberthia.registry.ModParticles.VOID_LEAK.get(),
                    c.x + Math.cos(a) * r, c.y + 1.0,
                    c.z + Math.sin(a) * r,
                    2, 0.03, 0.1, 0.03, 0.04);
            if (Math.random() < 0.3) {
                sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                        c.x, c.y + 1.2, c.z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    private void tickMadnessWave(ServerLevel sl, int age) {
        // Wave expands outward — emit particles at current radius
        float progress = age / (float) getTotalLifetime();
        double radius = progress * 8.0;
        int particles = 12;
        for (int i = 0; i < particles; i++) {
            double angle = (i / (double) particles) * Math.PI * 2;
            Vec3 c = position();
            sl.sendParticles(br.com.murilo.liberthia.registry.ModParticles.DIMENSIONAL_CRACK.get(),
                    c.x + Math.cos(angle) * radius,
                    c.y + 0.5,
                    c.z + Math.sin(angle) * radius,
                    1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private void tickCosmicVoid(ServerLevel sl, int age) {
        if (age % 2 == 0) {
            Vec3 c = position();
            double a = (age * 0.5) % (Math.PI * 2);
            double r = 1.5 + Math.sin(age * 0.1) * 0.5;
            sl.sendParticles(br.com.murilo.liberthia.registry.ModParticles.VOID_LEAK.get(),
                    c.x + Math.cos(a) * r,
                    c.y + 1.0 + Math.sin(age * 0.2) * 0.5,
                    c.z + Math.sin(a) * r,
                    1, 0.05, 0.1, 0.05, 0.03);
            sl.sendParticles(br.com.murilo.liberthia.registry.ModParticles.DIMENSIONAL_CRACK.get(),
                    c.x + (Math.random() - 0.5) * 4,
                    c.y + Math.random() * 4,
                    c.z + (Math.random() - 0.5) * 4,
                    1, 0, 0, 0, 0.01);
        }
    }

    private void tickEldritchBlast(ServerLevel sl, int age) {
        if (age == 0) {
            Vec3 c = position();
            sl.sendParticles(br.com.murilo.liberthia.registry.ModParticles.DIMENSIONAL_CRACK.get(),
                    c.x, c.y, c.z, 15, 0.5, 0.5, 0.5, 0.2);
        }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("owner"))  this.ownerUuid = tag.getUUID("owner");
        if (tag.contains("type"))   this.entityData.set(DATA_TYPE, tag.getInt("type"));
        if (tag.contains("life"))   this.entityData.set(DATA_LIFE, tag.getInt("life"));
        if (tag.contains("scale"))  this.entityData.set(DATA_SCALE, tag.getFloat("scale"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("owner", ownerUuid);
        tag.putInt("type",   entityData.get(DATA_TYPE));
        tag.putInt("life",   entityData.get(DATA_LIFE));
        tag.putFloat("scale", entityData.get(DATA_SCALE));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable()        { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double sqrDist) { return sqrDist < 4096.0; }
}
