package br.com.murilo.liberthia.magic.spell.voidspell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * v0.1.152 r120: <b>VoidLarvaEntity</b> — larva voidica spawnada pelo
 * VoidInfectionEffect. Extende Silverfish pra reusar AI + modelo (com textura
 * roxa via renderer custom).
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Persegue exclusivamente o alvo marcado (NBT target_uuid)</li>
 *   <li>Despawna após {@code lifespan} ticks</li>
 *   <li>Ataca por 4 dmg a cada melee hit</li>
 *   <li>Quando morre: emite burst de partículas roxas</li>
 * </ul>
 */
public class VoidLarvaEntity extends Silverfish {

    private static final EntityDataAccessor<String> TARGET_UUID =
            SynchedEntityData.defineId(VoidLarvaEntity.class, EntityDataSerializers.STRING);

    private int lifespan = 200; // 10 segundos default
    private UUID targetUuidCache = null;

    public VoidLarvaEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // não despawna naturalmente
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_UUID, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4D, true));
        // Target apenas o player marcado
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10,
                false, false, le -> {
                    if (le instanceof Player p) {
                        UUID id = getTargetUUID();
                        return id != null && id.equals(p.getUUID());
                    }
                    return false;
                }));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    public void setTargetUUID(UUID id) {
        this.entityData.set(TARGET_UUID, id == null ? "" : id.toString());
        this.targetUuidCache = id;
    }

    public UUID getTargetUUID() {
        if (targetUuidCache != null) return targetUuidCache;
        String s = this.entityData.get(TARGET_UUID);
        if (s.isEmpty()) return null;
        try {
            targetUuidCache = UUID.fromString(s);
            return targetUuidCache;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setLifespan(int t) { this.lifespan = t; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // VFX trail roxo cada tick
            if (random.nextFloat() < 0.4F) {
                level().addParticle(ParticleTypes.PORTAL,
                        getX() + (random.nextDouble() - 0.5) * 0.3,
                        getY() + 0.15,
                        getZ() + (random.nextDouble() - 0.5) * 0.3,
                        0, 0.02, 0);
            }
            return;
        }

        // Server: lifespan countdown
        if (--lifespan <= 0) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        br.com.murilo.liberthia.registry.ModParticles.VOID_INFECTION.get(),
                        getX(), getY() + 0.2, getZ(),
                        20, 0.3, 0.3, 0.3, 0.1);
            }
            this.discard();
            return;
        }

        // Verifica se alvo ainda existe — senão despawna
        UUID id = getTargetUUID();
        if (id != null && level() instanceof ServerLevel sl) {
            Player target = sl.getPlayerByUUID(id);
            if (target == null || !target.isAlive()) {
                this.discard();
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            // Buff bite: WITHER curto pra dramatic effect
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WITHER, 60, 0, false, false, true));
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(
                        br.com.murilo.liberthia.registry.ModParticles.VOID_INFECTION.get(),
                        living.getX(), living.getY() + 1, living.getZ(),
                        8, 0.3, 0.3, 0.3, 0.05);
            }
        }
        return hit;
    }

    /** Imune ao efeito Void Infection (não pode infectar a si mesma). */
    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance e) {
        if (e.getEffect() instanceof VoidInfectionEffect) return false;
        return super.canBeAffected(e);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Lifespan", lifespan);
        UUID id = getTargetUUID();
        if (id != null) tag.putUUID("TargetUUID", id);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifespan = tag.contains("Lifespan") ? tag.getInt("Lifespan") : 200;
        if (tag.hasUUID("TargetUUID")) setTargetUUID(tag.getUUID("TargetUUID"));
    }
}
