package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Blood Warden — final-tier boss inspired by the vanilla Warden but designed
 * to be much harder.
 *
 * <p><b>Stats</b>: 1000 HP, 26 ATK, armor 22, KB resist 1.0, fire/wither immune.
 *
 * <p><b>Phases</b>:
 * <ul>
 *   <li>Phase 1 (>50%): teleport to target every 4s, melee, sonic boom every 6s.</li>
 *   <li>Phase 2 (≤50%): + ground pound every 5s, AOE darkness every 8s.</li>
 *   <li><b>Phase 3 / ENRAGE (≤25%)</b>: 90% damage reduction +
 *       Regeneration of 4HP/s + every cooldown halved. Spectacular announcement.</li>
 * </ul>
 */
public class BloodWardenBossEntity extends Monster {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§4§lGuarda Sanguíneo"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    private int teleportCooldown = 16000;
    private int sonicCooldown = 500;
    private int poundCooldown = 200;
    private int darknessCooldown = 260;
    private boolean enrageTriggered = false;

    public BloodWardenBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 350;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.FOLLOW_RANGE, 38.0)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canBeAffected(MobEffectInstance e) {
        if (e.getEffect() == MobEffects.POISON) return false;
        if (e.getEffect() == MobEffects.WITHER) return false;
        if (e.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) return false;
        if (e.getEffect() == MobEffects.WEAKNESS) return false;
        if (ModEffects.BLOOD_INFECTION.get() != null
                && e.getEffect() == ModEffects.BLOOD_INFECTION.get()) return false;
        return super.canBeAffected(e);
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override protected void pushEntities() {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL)) return false;
        if (source.is(DamageTypes.DROWN)) return false;
        if (source.is(DamageTypes.IN_WALL)) return false;
        if (source.is(DamageTypes.LIGHTNING_BOLT)) return false;

        float hpFrac = getHealth() / getMaxHealth();
        // Phase 1/2 standard reduction (40%).
        if (hpFrac > 0.25F) amount *= 0.6F;
        // ENRAGE: 90% reduction below 25% HP.
        else amount *= 0.10F;

        // 8% chance to teleport away on big hits at full HP+.
        if (amount > 6F && getHealth() > 200F && random.nextFloat() < 0.20F
                && getTarget() instanceof Player p) {
            tryTeleportNearTarget(p, 6.0);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());

        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.SCULK_SOUL,
                        getX() + (random.nextDouble() - 0.5) * 1.6,
                        getY() + 1.0 + random.nextDouble() * 1.5,
                        getZ() + (random.nextDouble() - 0.5) * 1.6,
                        0, 0.05, 0);
            }
            return;
        }

        float hpFrac = getHealth() / getMaxHealth();
        boolean phase2 = hpFrac <= 0.50F;
        boolean phase3 = hpFrac <= 0.25F;

        // ENRAGE announcement (one-shot).
        if (phase3 && !enrageTriggered) {
            enrageTriggered = true;
            ServerLevel sl = (ServerLevel) level();
            sl.players().forEach(p ->
                    p.displayClientMessage(
                            Component.literal("§4§lO GUARDA ENTRA EM FÚRIA!"), false));
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    getX(), getY() + 1.5, getZ(), 30, 1.5, 1.5, 1.5, 0.0);
            sl.playSound(null, blockPosition(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
            // Self-haste + slowness II to all nearby players.
            for (Player p : sl.getEntitiesOfClass(Player.class,
                    new AABB(blockPosition()).inflate(20.0))) {
                if (p.isCreative() || p.isSpectator()) continue;
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
            }
        }

        // ENRAGE regen — 4HP/s while below 25%.
        if (phase3 && tickCount % 5 == 0) {
            heal(1.0F);
        }

        Player nearest = level().getNearestPlayer(this, 64.0);
        if (nearest != null) setTarget(nearest);

        int phaseMul = phase3 ? 2 : (phase2 ? 1 : 1); // not used as multiplier; we manually shorten cd in p3

        // --- Teleport to target ---
        if (--teleportCooldown <= 0 && nearest != null) {
            teleportCooldown = phase3 ? 40 : (phase2 ? 60 : 80);
            double horiz = Math.hypot(nearest.getX() - getX(), nearest.getZ() - getZ());
            if (horiz > 5.0) tryTeleportNearTarget(nearest, 3.0);
        }

        // --- Sonic Boom (line damage 16 blocks) ---
        if (--sonicCooldown <= 0 && nearest != null) {
            sonicCooldown = phase3 ? 60 : (phase2 ? 100 : 140);
            sonicBoom(nearest);
        }

        // --- Ground Pound (phase 2+) ---
        if (phase2 && --poundCooldown <= 0) {
            poundCooldown = phase3 ? 90 : 160;
            groundPound();
        }

        // --- Darkness AOE (phase 2+) ---
        if (phase2 && --darknessCooldown <= 0) {
            darknessCooldown = phase3 ? 120 : 200;
            darknessAura();
        }
    }

    // ---------------------------------------------------------------- attacks

    private void tryTeleportNearTarget(Player target, double minDist) {
        if (!(level() instanceof ServerLevel sl)) return;
        BlockPos best = null;
        for (int i = 0; i < 12; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = minDist + random.nextDouble() * 2.5;
            int dx = (int) (Math.cos(a) * r);
            int dz = (int) (Math.sin(a) * r);
            BlockPos cand = target.blockPosition().offset(dx, 0, dz);
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos c = cand.offset(0, dy, 0);
                if (sl.getBlockState(c).isAir()
                        && sl.getBlockState(c.above()).isAir()
                        && sl.getBlockState(c.above().above()).isAir()
                        && !sl.getBlockState(c.below()).isAir()) {
                    best = c; break;
                }
            }
            if (best != null) break;
        }
        if (best == null) return;

        sl.sendParticles(ParticleTypes.PORTAL,
                getX(), getY() + 1.0, getZ(), 30, 0.6, 1.0, 0.6, 0.4);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 1.4F, 0.6F);

        teleportTo(best.getX() + 0.5, best.getY() + 0.1, best.getZ() + 0.5);

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                getX(), getY() + 1.0, getZ(), 30, 1.0, 1.0, 1.0, 0.05);
    }

    private void sonicBoom(Player target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 origin = new Vec3(getX(), getY() + 1.5, getZ());
        Vec3 dir = target.getEyePosition().subtract(origin).normalize();
        double range = 16.0;

        for (int i = 1; i <= range; i++) {
            Vec3 p = origin.add(dir.scale(i));
            sl.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 2.5F, 1.0F);

        AABB box = new AABB(origin, origin.add(dir.scale(range))).inflate(2.5);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            Vec3 toP = p.position().add(0, 1, 0).subtract(origin);
            double along = toP.dot(dir);
            if (along < 0 || along > range) continue;
            Vec3 perp = toP.subtract(dir.scale(along));
            if (perp.length() > 2.0) continue;

            p.hurt(damageSources().sonicBoom(this), 16.0F);
            p.push(dir.x * 1.6, 0.5, dir.z * 1.6);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        }
    }

    private void groundPound() {
        if (!(level() instanceof ServerLevel sl)) return;
        AABB box = new AABB(blockPosition()).inflate(7.0);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            double dist = p.position().distanceTo(this.position());
            float dmg = (float) Math.max(3.0, 9.0 - dist * 0.6);
            p.hurt(damageSources().mobAttack(this), dmg);
            double dx = p.getX() - getX();
            double dz = p.getZ() - getZ();
            double d = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            p.push(dx / d * 0.9, 1.4, dz / d * 0.9);
            p.hurtMarked = true;
            p.fallDistance = 0F;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
        for (int i = 0; i < 24; i++) {
            double a = (i / 24.0) * Math.PI * 2;
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    getX() + Math.cos(a) * 5.0, getY() + 0.2,
                    getZ() + Math.sin(a) * 5.0, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 2.5F, 0.7F);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 1.8F, 0.6F);
    }

    private void darknessAura() {
        if (!(level() instanceof ServerLevel sl)) return;
        AABB box = new AABB(blockPosition()).inflate(12.0);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        }
        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                getX(), getY() + 1.5, getZ(), 60, 6.0, 2.0, 6.0, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_AGITATED,
                SoundSource.HOSTILE, 1.5F, 0.7F);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) {
            // Each melee hit applies a stack of debuffs.
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            if (ModEffects.BLOOD_INFECTION.get() != null) {
                le.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 200, 1));
            }
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_CORE.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_ESSENCE.get(), 6 + random.nextInt(6)));
        spawnAtLocation(new ItemStack(ModItems.HEART_OF_THE_MOTHER.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.PULSING_HEART.get(), 1));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
}
