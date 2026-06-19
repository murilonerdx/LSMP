package br.com.murilo.liberthia.magic.spells;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * v0.1.24 r99: <b>Frozen Humanoid</b> — statue entity de ice tomb.
 *
 * <p>Spawnado quando um entity é congelado por IceTombSpell. Renderiza visual
 * de "statue" no lugar do entity original (que fica embaixo invisible por
 * tempo). Quebra com hit melee → desbloqueia entity original.
 *
 * <p>Tem 30 HP, sem AI. Despawn após 30s.
 */
public class FrozenHumanoidEntity extends Monster {

    private static final EntityDataAccessor<Integer> CAPTURED_ENTITY_TYPE_ID =
            SynchedEntityData.defineId(FrozenHumanoidEntity.class, EntityDataSerializers.INT);

    public FrozenHumanoidEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 5.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAPTURED_ENTITY_TYPE_ID, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;

        // Particles de gelo flutuantes
        if (tickCount % 8 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE,
                    getX(), getY() + 0.5, getZ(),
                    2, 0.3, 0.5, 0.3, 0.01);
        }

        // Despawn após 30s
        if (tickCount > 600) {
            discard();
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Crystal-like — fácil de quebrar com qualquer hit
        if (amount >= 3) {
            // Smash effect
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        getX(), getY() + 1, getZ(),
                        30, 0.5, 1.0, 0.5, 0.1);
            }
            discard();
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CapturedTypeId", entityData.get(CAPTURED_ENTITY_TYPE_ID));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CapturedTypeId")) {
            entityData.set(CAPTURED_ENTITY_TYPE_ID, tag.getInt("CapturedTypeId"));
        }
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return false; // fire pode quebrar
    }
}
