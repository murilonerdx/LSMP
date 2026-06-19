package br.com.murilo.liberthia.magic.spell.voidspell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * v0.1.152 r120: <b>MiniBlackHoleEntity</b> — mini buraco negro do feitiço Vazio.
 *
 * <p>Mecânica:
 * <ul>
 *   <li>Lifespan ~40 ticks (2s)</li>
 *   <li>Puxa entities num raio de 5 blocos com força crescente</li>
 *   <li>No tick 30, EXPLODE: ondas radiais de partículas + dano AOE</li>
 *   <li>Dano: 80 por explosão (modificável via setDamage)</li>
 *   <li>Caster armazenado pra evitar self-damage</li>
 * </ul>
 *
 * <p>Visual: orbital de partículas pretas/roxas formando swirl pra dentro,
 * culminando em flash branco antes de sumir.
 */
public class MiniBlackHoleEntity extends Entity {

    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(MiniBlackHoleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> AGE =
            SynchedEntityData.defineId(MiniBlackHoleEntity.class, EntityDataSerializers.INT);

    public static final int LIFESPAN = 40;
    public static final int EXPLODE_TICK = 30;
    public static final double PULL_RADIUS = 5.0;
    public static final double DAMAGE_RADIUS = 4.0;

    private float damage = 80F;
    private UUID casterUuid = null;

    public MiniBlackHoleEntity(EntityType<MiniBlackHoleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SCALE, 0.5F);
        this.entityData.define(AGE, 0);
    }

    public void setDamage(float d) { this.damage = d; }
    public float getDamage() { return damage; }
    public void setCaster(LivingEntity caster) { this.casterUuid = caster.getUUID(); }
    public int getAge() { return entityData.get(AGE); }
    public float getScale() { return entityData.get(SCALE); }

    @Override
    public void tick() {
        super.tick();
        int age = getAge();
        entityData.set(AGE, age + 1);

        // Crescer scale durante primeiros 20 ticks
        if (age < 20) {
            entityData.set(SCALE, 0.5F + (age / 20F) * 1.5F); // 0.5 → 2.0
        } else {
            // Encolher rápido nos últimos
            entityData.set(SCALE, Math.max(0.1F, 2.0F - (age - 20) * 0.05F));
        }

        if (!level().isClientSide && level() instanceof ServerLevel sl) {
            // r137 fix #14: pull APENAS antes da explosao.
            // Continuar puxando depois do bang ficava esquisito visualmente
            // (entities mortas/vivas sendo jogadas no centro de algo que ja sumiu)
            // e podia jogar player no AOE de novo se ele reapareceu no tick certo.
            if (age < EXPLODE_TICK) {
                pullEntities();
            }

            // Explosão no tick exato
            if (age == EXPLODE_TICK) {
                explode(sl);
            }

            // Despawn
            if (age >= LIFESPAN) {
                this.discard();
            }
        } else if (level().isClientSide) {
            // VFX swirl client-side
            spawnSwirl();
        }
    }

    private void pullEntities() {
        AABB box = getBoundingBox().inflate(PULL_RADIUS);
        for (LivingEntity le : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (casterUuid != null && casterUuid.equals(le.getUUID())) continue;
            Vec3 toMe = position().subtract(le.position());
            double dist = toMe.length();
            if (dist < 0.1 || dist > PULL_RADIUS) continue;
            double force = 0.3 * (1.0 - dist / PULL_RADIUS);
            Vec3 pull = toMe.normalize().scale(force);
            le.setDeltaMovement(le.getDeltaMovement().add(pull));
        }
    }

    private void explode(ServerLevel sl) {
        // Dano AOE
        AABB box = getBoundingBox().inflate(DAMAGE_RADIUS);
        DamageSource ds = damageSources().magic();
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (casterUuid != null && casterUuid.equals(le.getUUID())) continue;
            le.hurt(ds, damage);
            // Aplica VoidInfection
            var effect = br.com.murilo.liberthia.registry.ModMobEffects.VOID_INFECTION.get();
            if (effect != null) {
                le.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, 200, 0));
            }
        }

        // VFX explosão
        for (int i = 0; i < 60; i++) {
            double ang = (i / 60.0) * Math.PI * 2;
            for (double r = 0; r < DAMAGE_RADIUS; r += 0.5) {
                sl.sendParticles(ParticleTypes.PORTAL,
                        getX() + Math.cos(ang) * r,
                        getY() + 0.5,
                        getZ() + Math.sin(ang) * r,
                        1, 0.05, 0.1, 0.05, 0.15);
            }
        }
        for (int i = 0; i < 40; i++) {
            sl.sendParticles(
                    br.com.murilo.liberthia.registry.ModParticles.MINI_BLACK_HOLE.get(),
                    getX() + (sl.random.nextDouble() - 0.5) * 6,
                    getY() + (sl.random.nextDouble() - 0.5) * 4,
                    getZ() + (sl.random.nextDouble() - 0.5) * 6,
                    1, 0.1, 0.1, 0.1, 0.05);
        }
        // Sound
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 0.7F, 1.5F);
    }

    private void spawnSwirl() {
        // Cria swirl orbital de partículas no client
        double age = getAge();
        for (int i = 0; i < 4; i++) {
            double ang = (random.nextDouble() * Math.PI * 2) + age * 0.15;
            double r = 0.3 + random.nextDouble() * 1.2;
            double dx = Math.cos(ang) * r;
            double dz = Math.sin(ang) * r;
            // Velocidade aponta pra dentro (efeito sucção)
            level().addParticle(ParticleTypes.PORTAL,
                    getX() + dx,
                    getY() + (random.nextDouble() - 0.5) * 0.6,
                    getZ() + dz,
                    -dx * 0.1, 0, -dz * 0.1);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Damage")) damage = tag.getFloat("Damage");
        if (tag.hasUUID("Caster")) casterUuid = tag.getUUID("Caster");
        entityData.set(AGE, tag.getInt("Age"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", damage);
        if (casterUuid != null) tag.putUUID("Caster", casterUuid);
        tag.putInt("Age", getAge());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
