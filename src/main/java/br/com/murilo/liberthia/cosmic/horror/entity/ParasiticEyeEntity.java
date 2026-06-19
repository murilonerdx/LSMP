package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * r180b: <b>Olho Parasita</b> — gerado pelo {@link BlindAmalgamEntity}. CAI no chão e
 * <b>rasteja</b>, <b>sobe paredes</b> (estilo aranha, {@link WallClimberNavigation} +
 * {@code onClimbable}). Observa o player <b>à distância</b> e, só quando tem
 * <b>linha de visão</b>, <b>transmite a posição</b> ao Amalgamado (relay). Frágil
 * (4 HP) — matar/cegar os olhos deixa o Amalgamado cego. Some sozinho após ~40s.
 */
public class ParasiticEyeEntity extends Monster {

    private int life = 0;
    private int navCd = 0;
    private static final int MAX_LIFE = 800; // 40s
    private static final double OBSERVE_DIST = 6.0;

    public ParasiticEyeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);   // sobe degraus
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level); // pathing que sobe paredes
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this)); // não afoga; movimento é manual no tick
    }

    /** Sobe paredes ao encostar nelas (como aranha). */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    /** Olho frágil não toma dano de queda (cai do corpo do Amalgamado). */
    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;

        if (++life > MAX_LIFE) { vanish(sl); return; }
        if (navCd > 0) navCd--;

        Player p = level().getNearestPlayer(this, 24.0);
        if (p == null || !p.isAlive() || p.isCreative() || p.isSpectator()) {
            if (navCd == 0) { getNavigation().stop(); navCd = 20; }
            return;
        }

        // rasteja mantendo distância de observação
        double d = this.distanceTo(p);
        getLookControl().setLookAt(p, 60F, 60F);
        if (navCd == 0) {
            navCd = 8;
            if (d > OBSERVE_DIST + 2) {
                getNavigation().moveTo(p, 1.0);                 // aproxima pra observar
            } else if (d < OBSERVE_DIST - 2) {
                Vec3 away = position().subtract(p.position());
                if (away.lengthSqr() > 1.0e-3) {
                    away = away.normalize().scale(6.0);
                    getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.0); // recua
                }
            } else {
                getNavigation().stop();                          // observa parado
            }
        }

        // TRANSMITE a posição — só com linha de visão (matar/cegar o olho corta o relay)
        if (life % 20 == 0 && this.hasLineOfSight(p)) {
            long now = sl.getGameTime();
            for (BlindAmalgamEntity a : sl.getEntitiesOfClass(BlindAmalgamEntity.class,
                    getBoundingBox().inflate(64.0))) {
                a.reportTarget(p, now);
            }
            sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, getX(), getY() + 0.2, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (this.tickCount % 8 == 0) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 0.2, getZ(), 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    private void vanish(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        discard();
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

    @Override public boolean removeWhenFarAway(double dist) { return true; }
    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("Life", life); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); life = t.getInt("Life"); }
}
