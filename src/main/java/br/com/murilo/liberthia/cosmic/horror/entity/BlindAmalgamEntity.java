package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * r180: <b>O Amalgamado Cego</b> — massa carnuda estilo Flukemarm. É CEGO: não te vê
 * por conta própria — gera <b>Olhos Parasitas</b> ({@link ParasiticEyeEntity}) que
 * flutuam, te observam e transmitem tua posição. Lento e tanque. Em vida baixa (&lt;30%)
 * entra em <b>fúria</b> (mais rápido/forte + jorra olhos). Ao morrer, <b>explode</b>
 * (dano em área, sem destruir blocos).
 */
public class BlindAmalgamEntity extends Monster {

    private static final UUID RAGE_SPD = UUID.fromString("a3a10ade-0001-4eee-9a01-0e0e0e0e0001");
    private static final UUID RAGE_ATK = UUID.fromString("a3a10ade-0002-4eee-9a01-0e0e0e0e0002");
    private static final int EYE_CAP = 5;

    private int spawnEyeCd = 80;
    private boolean raging = false;
    /** Última vez (game-time) que um olho reportou OU que sentiu presa colada. */
    private long lastEyeReport = -100000L;

    public BlindAmalgamEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.14)   // lento
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        // CEGO: sem NearestAttackableTargetGoal por visão — só alvo via olhos (relay) ou HurtBy
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.0D, 40.0F));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.5D));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;

        // CEGO: sente presa COLADA (tato) ou enxerga pelos olhos (relay). Sem nenhum
        // dos dois por >5s, perde o rastro e volta a vagar às cegas.
        long now = sl.getGameTime();
        Player feel = sl.getNearestPlayer(this, 3.5);
        if (feel != null && feel.isAlive() && !feel.isSpectator() && !feel.isCreative()) {
            setTarget(feel);
            lastEyeReport = now;
        } else if (getTarget() instanceof Player && now - lastEyeReport > 100) {
            setTarget(null);
        }

        // Fúria em vida baixa
        if (!raging && getHealth() < getMaxHealth() * 0.30F) enterRage(sl);

        // Jorra olhos parasitas (mais rápido em fúria), respeitando o cap
        if (--spawnEyeCd <= 0) {
            spawnEyeCd = raging ? 50 : 120;
            long count = sl.getEntitiesOfClass(ParasiticEyeEntity.class, getBoundingBox().inflate(48.0)).size();
            if (count < EYE_CAP) spawnEye(sl);
        }

        if (this.tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + getBbHeight() * 0.6, getZ(),
                    4, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.0);
        }
    }

    /** Relay dos Olhos Parasitas: o Amalgamado "vê" o player pelos olhos (refresca a visão). */
    public void reportTarget(Player p, long now) {
        if (p == null || !p.isAlive()) return;
        this.setTarget(p);
        this.lastEyeReport = now;
    }

    private void spawnEye(ServerLevel sl) {
        ParasiticEyeEntity eye = br.com.murilo.liberthia.registry.ModEntities.PARASITIC_EYE.get().create(sl);
        if (eye == null) return;
        eye.moveTo(getX() + (random.nextDouble() - 0.5) * 2, getY() + 1.0 + random.nextDouble(),
                getZ() + (random.nextDouble() - 0.5) * 2, random.nextFloat() * 360F, 0);
        if (getTarget() != null) eye.setTarget(getTarget());
        sl.addFreshEntity(eye);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 0.8F, 0.6F);
    }

    private void enterRage(ServerLevel sl) {
        raging = true;
        setMod(Attributes.MOVEMENT_SPEED, RAGE_SPD, 0.12);
        setMod(Attributes.ATTACK_DAMAGE, RAGE_ATK, 4.0);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4F, 0.5F);
        for (int i = 0; i < 3; i++) {
            long count = sl.getEntitiesOfClass(ParasiticEyeEntity.class, getBoundingBox().inflate(48.0)).size();
            if (count < EYE_CAP) spawnEye(sl);
        }
    }

    private void setMod(net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id, double amount) {
        AttributeInstance inst = getAttribute(attr);
        if (inst == null || inst.getModifier(id) != null) return;
        inst.addTransientModifier(new AttributeModifier(id, "amalgam_rage", amount, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !level().isClientSide) {
            level().playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    SoundEvents.SCULK_SHRIEKER_SHRIEK,
                    getSoundSource(),
                    1.0F,
                    1.0F
            );
        }

        return result;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 1, getZ(), 80, 1.2, 1.0, 1.2, 0.1);
            sl.explode(this, getX(), getY() + 0.5, getZ(), 3.2F, Level.ExplosionInteraction.NONE);
        }
        if (!level().isClientSide) {
            level().playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    SoundEvents.WARDEN_DEATH,
                    getSoundSource(),
                    1.0F,
                    1.0F
            );
        }

        super.die(source);
    }

    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putBoolean("Raging", raging); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); raging = t.getBoolean("Raging"); }
}
