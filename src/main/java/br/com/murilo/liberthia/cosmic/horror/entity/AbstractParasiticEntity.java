package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.cosmic.ICosmicHorror;
import br.com.murilo.liberthia.cosmic.IScalableBillboard;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * r183 — <b>base das criaturas cósmicas tipo Olho Parasita</b> (14 variantes). Billboard
 * plano ({@link IScalableBillboard}), rasteja/sobe paredes, observa o player à distância,
 * solta partículas de infecção e INFECTA o chão com dimensional_flux. Habilidades criativas
 * por subclasse via {@link #tickAbility}. Corrige o bug "some quando morro": entidades
 * spawnadas por ovo/comando ficam PERSISTENTES (não despawnam por distância nem por tempo).
 */
public abstract class AbstractParasiticEntity extends Monster implements ICosmicHorror, IScalableBillboard {

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(AbstractParasiticEntity.class, EntityDataSerializers.FLOAT);

    protected int life = 0;
    protected int navCd = 0;

    protected AbstractParasiticEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.xpReward = 0;
    }

    // ── tunáveis (override por variante) ──
    protected int maxLifeTicks() { return 2400; }       // 120s efêmero; -1 = nunca expira
    protected double observeDist() { return 6.0; }
    protected ParticleOptions ambientParticle() { return ParticleTypes.SCULK_SOUL; }
    protected boolean infectsGround() { return true; }
    protected int groundInfectChance() { return 60; }   // 1/N por tick de chance de infectar o chão
    protected float baseScale() { return 1.0F; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CATALYST_BLOOM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SCULK_SHRIEKER_SHRIEK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); this.entityData.define(DATA_SCALE, 1.0F); }
    @Override public float billboardScale() { return this.entityData.get(DATA_SCALE); }
    protected void setScaleVal(float s) {
        float c = Mth.clamp(s, 0.4F, 4.0F);
        if (Math.abs(c - billboardScale()) > 0.001F) { this.entityData.set(DATA_SCALE, c); refreshDimensions(); }
    }
    protected void growBy(float d) { setScaleVal(billboardScale() + d); }
    @Override public EntityDimensions getDimensions(Pose pose) { return super.getDimensions(pose).scale(billboardScale()); }

    @Override protected PathNavigation createNavigation(Level level) { return new WallClimberNavigation(this, level); }
    @Override protected void registerGoals() { this.goalSelector.addGoal(0, new FloatGoal(this)); }
    @Override public boolean onClimbable() { return this.horizontalCollision; }
    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }

    // ── persistência: ovo/comando/spawner = NÃO some (corrige bug) ──
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor lvl, DifficultyInstance diff, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        if (reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND
                || reason == MobSpawnType.SPAWNER || reason == MobSpawnType.BUCKET || reason == MobSpawnType.DISPENSER) {
            this.setPersistenceRequired();
        }
        setScaleVal(baseScale());
        return super.finalizeSpawn(lvl, diff, reason, data, tag);
    }

    @Override public boolean removeWhenFarAway(double dist) { return !this.isPersistenceRequired(); }

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

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;

        life++;
        // efêmero só se NÃO for persistente (spawnado por ambush). Egg/comando ficam pra sempre.
        if (maxLifeTicks() > 0 && life > maxLifeTicks() && !this.isPersistenceRequired()) { vanish(sl); return; }
        if (navCd > 0) navCd--;

        // VFX ambiente: aura temática ao redor da criatura (escala com o tamanho)
        float sc = billboardScale();
        if (this.tickCount % 4 == 0) {
            double rad = 0.35 * sc;
            sl.sendParticles(ambientParticle(), getX(), getY() + getBbHeight() * 0.5, getZ(), 2, rad, rad, rad, 0.01);
        }
        if (this.tickCount % 14 == 0) {
            double rr = 0.6 * sc;
            for (int i = 0; i < 6; i++) {
                double ang = this.tickCount * 0.1 + i * Math.PI / 3.0;
                sl.sendParticles(ambientParticle(),
                        getX() + Math.cos(ang) * rr, getY() + getBbHeight() * 0.5, getZ() + Math.sin(ang) * rr,
                        1, 0.0, 0.02, 0.0, 0.0);
            }
        }

        // infecta o chão com dimensional_flux
        if (infectsGround() && sl.random.nextInt(groundInfectChance()) == 0) infectGround(sl);

        Player p = level().getNearestPlayer(this, 28.0);
        if (p == null || !p.isAlive() || p.isCreative() || p.isSpectator()) {
            if (navCd == 0) { getNavigation().stop(); navCd = 20; }
            return;
        }

        // rasteja mantendo distância de observação
        double d = this.distanceTo(p);
        getLookControl().setLookAt(p, 60F, 60F);
        if (navCd == 0) {
            navCd = 8;
            double od = observeDist();
            if (d > od + 2) getNavigation().moveTo(p, 1.0);
            else if (d < od - 2) {
                Vec3 away = position().subtract(p.position());
                if (away.lengthSqr() > 1.0e-3) {
                    away = away.normalize().scale(6.0);
                    getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.0);
                }
            } else getNavigation().stop();
        }

        tickAbility(sl, p);
    }

    /** Habilidade criativa da variante (chamada todo tick com o player mais próximo). */
    protected abstract void tickAbility(ServerLevel sl, Player target);

    // ── helpers p/ as variantes ──
    protected boolean isWatchedBy(Player p) {
        Vec3 look = p.getViewVector(1.0F).normalize();
        Vec3 to = this.position().add(0, getBbHeight() * 0.5, 0).subtract(p.getEyePosition()).normalize();
        return look.dot(to) > 0.965 && p.hasLineOfSight(this);
    }

    protected void infectGround(ServerLevel sl) {
        BlockPos below = blockPosition().below();
        var st = sl.getBlockState(below);
        if (st.isAir() || st.is(net.minecraft.world.level.block.Blocks.BEDROCK)) return;
        if (st.is(ModBlocks.DIMENSIONAL_FLUX.get())) return;
        if (st.getDestroySpeed(sl, below) < 0) return;
        sl.setBlock(below, ModBlocks.DIMENSIONAL_FLUX.get().defaultBlockState(), 3);
    }

    protected void vanish(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 8, 0.2, 0.3, 0.2, 0.01);
        discard();
    }

    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t); t.putInt("Life", life); t.putFloat("PScale", billboardScale());
    }
    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t); life = t.getInt("Life");
        if (t.contains("PScale")) setScaleVal(t.getFloat("PScale"));
    }
}
