package br.com.murilo.liberthia.magic.spell.voidspell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * r165: <b>Void Tentacle Entity</b> — entidade que renderiza um tentáculo
 * eldritch saindo do chão. Vive 80 ticks; durante esse tempo:
 *
 * <ul>
 *   <li>Frame 0-15: tentáculo cresce (windup)</li>
 *   <li>Frame 16-65: tentáculo ativo — puxa entidades próximas, causa dano periódico</li>
 *   <li>Frame 66-80: retração</li>
 * </ul>
 *
 * <p>Sprite animado: 8 frames cycling via {@link #tickCount}.
 * Cada tentáculo cobre raio de 3 blocos. Damage tick a cada 10t.
 *
 * <p>Spawn pattern: castVoidTentacle spawna 5 tentáculos em arco frontal
 * (-60°, -30°, 0°, +30°, +60°), 4 blocos de distância do caster.
 */
public class VoidTentacleEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(VoidTentacleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(VoidTentacleEntity.class, EntityDataSerializers.FLOAT);

    public static final int TOTAL_LIFE = 80;
    public static final int DAMAGE_INTERVAL = 10;
    public static final float DAMAGE_RADIUS = 3.0f;

    private UUID ownerUuid;

    public VoidTentacleEntity(EntityType<VoidTentacleEntity> type, Level level) {
        super(type, level);
    }

    public VoidTentacleEntity(Level level, LivingEntity owner, Vec3 pos, float damage) {
        this(br.com.murilo.liberthia.registry.ModEntities.VOID_TENTACLE.get(), level);
        this.setPos(pos.x, pos.y, pos.z);
        this.ownerUuid = owner.getUUID();
        this.entityData.set(DATA_DAMAGE, damage);
        this.entityData.set(DATA_LIFE, TOTAL_LIFE);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LIFE, TOTAL_LIFE);
        this.entityData.define(DATA_DAMAGE, 12.0f);
    }

    public int getLifeRemaining() { return entityData.get(DATA_LIFE); }
    public float getDamage()       { return entityData.get(DATA_DAMAGE); }

    /** Returns 0..1 progress through the lifetime (for animation frame selection). */
    public float getLifeProgress() {
        int remaining = entityData.get(DATA_LIFE);
        return 1f - (remaining / (float) TOTAL_LIFE);
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

        // Server-side: damage + pull
        if (level() instanceof ServerLevel sl) {
            int age = TOTAL_LIFE - life;
            // Active phase: 15 → 65 (50 ticks of active grabbing)
            if (age >= 15 && age <= 65 && (age - 15) % DAMAGE_INTERVAL == 0) {
                LivingEntity owner = ownerUuid == null ? null
                        : (LivingEntity) sl.getEntity(ownerUuid);
                damageNearby(sl, owner);
            }
            // Idle particles every 3 ticks (sustained VFX)
            if (age % 3 == 0) {
                emitVoidParticles(sl);
            }
        }
    }

    private void damageNearby(ServerLevel sl, LivingEntity owner) {
        Vec3 center = position();
        List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(DAMAGE_RADIUS));
        DamageSource ds = owner != null
                ? sl.damageSources().indirectMagic(this, owner)
                : sl.damageSources().magic();
        float dmg = entityData.get(DATA_DAMAGE);
        for (LivingEntity le : victims) {
            if (le == owner) continue;
            if (le.distanceToSqr(center) > DAMAGE_RADIUS * DAMAGE_RADIUS) continue;
            le.hurt(ds, dmg);
            // Pull toward tentacle center
            Vec3 pull = center.subtract(le.position()).normalize().scale(0.6);
            le.setDeltaMovement(le.getDeltaMovement().add(pull.x, 0.25, pull.z));
            le.hurtMarked = true;
            // Custom void grasp effect
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false));
            le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        }
    }

    private void emitVoidParticles(ServerLevel sl) {
        Vec3 c = position();
        // Purple/cyan oozing particles around the tentacle's vertical column
        double radius = 0.5 + Math.random() * 0.8;
        double angle = Math.random() * Math.PI * 2;
        double height = Math.random() * 3.0;
        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                c.x + Math.cos(angle) * radius,
                c.y + height,
                c.z + Math.sin(angle) * radius,
                1, 0.05, 0.05, 0.05, 0.005);
        // Tip sparkle
        if (Math.random() < 0.3) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    c.x + (Math.random() - 0.5) * 0.4,
                    c.y + 2.8,
                    c.z + (Math.random() - 0.5) * 0.4,
                    1, 0, 0, 0, 0.01);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("owner")) this.ownerUuid = tag.getUUID("owner");
        if (tag.contains("life"))   this.entityData.set(DATA_LIFE, tag.getInt("life"));
        if (tag.contains("damage")) this.entityData.set(DATA_DAMAGE, tag.getFloat("damage"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUuid != null) tag.putUUID("owner", ownerUuid);
        tag.putInt("life", entityData.get(DATA_LIFE));
        tag.putFloat("damage", entityData.get(DATA_DAMAGE));
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override public boolean isPickable()           { return false; }
    @Override public boolean canBeCollidedWith()    { return false; }
}
