package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.entity.projectile.HemoBoltEntity;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Final boss. Stationary giant flesh node that fights via:
 *
 * <ul>
 *   <li>Worm summons (more in late phases).</li>
 *   <li>Blood pulse AOE (push + damage + Blood Infection).</li>
 *   <li>Hemo bolts (phase 2+) at nearest player.</li>
 *   <li><b>Sonic Boom</b>: line attack dealing big damage in front.</li>
 *   <li><b>Ground Pound</b> (phase 2+): AOE knockup that flings players.</li>
 *   <li><b>Teleport-to-player</b> (phase 2+): pulls itself near a far player.</li>
 *   <li>Phase 3: rapid combo of all the above.</li>
 * </ul>
 */
public class FleshMotherBossEntity extends Monster {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§4§lA Mãe da Carne"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    private int wormCooldown = 80;
    private int boltCooldown = 60;
    private int pulseCooldown = 40;
    private int sonicCooldown = 100;
    private int poundCooldown = 200;
    private int teleportCooldown = 200;
    private int shieldHealTick = 0;

    public FleshMotherBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 18.5)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {}

    @Override
    public boolean isPushable() { return false; }
    @Override
    protected void pushEntities() {}

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == MobEffects.POISON) return false;
        if (effect.getEffect() == MobEffects.WITHER) return false;
        if (effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) return false;
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()) return false;
        return super.canBeAffected(effect);
    }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) return false;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)) return false;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
        float phase = getHealth() / getMaxHealth();
        if (phase > 0.33F) amount *= 0.5F;
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());

        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.DAMAGE_INDICATOR,
                        getX() + (random.nextDouble() - 0.5) * 2.5,
                        getY() + 1.0 + random.nextDouble() * 1.8,
                        getZ() + (random.nextDouble() - 0.5) * 2.5,
                        0, 0.05, 0);
            }
            return;
        }

        float hpFrac = getHealth() / getMaxHealth();
        boolean phase2 = hpFrac <= 0.66F;
        boolean phase3 = hpFrac <= 0.33F;

        Player nearest = level().getNearestPlayer(this, 64.0);
        if (nearest != null) setTarget(nearest);

        if (--wormCooldown <= 0) {
            wormCooldown = phase3 ? 50 : (phase2 ? 100 : 140);
            summonWorm();
            if (phase3) { summonWorm(); summonWorm(); }
        }

        if (--pulseCooldown <= 0) {
            pulseCooldown = phase3 ? 30 : 70;
            bloodPulse(phase3 ? 9.0 : 7.0, phase3 ? 8.0F : 6.0F);
        }

        if (phase2 && nearest != null && --boltCooldown <= 0) {
            boltCooldown = phase3 ? 25 : 45;
            fireBolt(nearest);
        }

        if (--sonicCooldown <= 0 && nearest != null) {
            sonicCooldown = phase3 ? 80 : (phase2 ? 120 : 180);
            sonicBoom(nearest);
        }

        if (phase2 && --poundCooldown <= 0) {
            poundCooldown = phase3 ? 120 : 200;
            groundPound();
        }

        if (phase2 && --teleportCooldown <= 0 && nearest != null) {
            teleportCooldown = phase3 ? 140 : 240;
            tryTeleportNearPlayer(nearest);
        }

        if (!phase3) {
            shieldHealTick++;
            if (shieldHealTick >= 60) {
                shieldHealTick = 0;
                heal(1.5F);
            }
        }
    }

    // ---------------------------------------------------------------- attacks

    private void summonWorm() {
        EntityType<?> type = random.nextInt(3) == 0
                ? ModEntities.GORE_WORM.get()
                : ModEntities.BLOOD_WORM.get();
        Entity worm = type.create(level());
        if (worm == null) return;
        double ox = (random.nextDouble() - 0.5) * 4;
        double oz = (random.nextDouble() - 0.5) * 4;
        worm.moveTo(getX() + ox, getY(), getZ() + oz, random.nextFloat() * 360F, 0);
        level().addFreshEntity(worm);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    worm.getX(), worm.getY() + 0.5, worm.getZ(),
                    18, 0.4, 0.4, 0.4, 0.1);
        }
    }

    private void bloodPulse(double radius, float damage) {
        AABB box = new AABB(blockPosition()).inflate(radius);
        for (Player p : level().getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.hurt(damageSources().indirectMagic(this, this), damage);
            double dx = p.getX() - getX();
            double dz = p.getZ() - getZ();
            double d = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            p.push(dx / d * 0.6, 0.3, dz / d * 0.6);
            if (ModEffects.BLOOD_INFECTION.get() != null) {
                p.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 120, 1));
            }
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    getX(), getY() + 0.5, getZ(),
                    80, radius / 2, 0.4, radius / 2, 0.3);
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    private void fireBolt(Player target) {
        double dx = target.getX() - getX();
        double dy = target.getY(0.5) - (getY() + 1.5);
        double dz = target.getZ() - getZ();
        HemoBoltEntity bolt = new HemoBoltEntity(level(), this, dx, dy, dz);
        bolt.setPos(getX(), getY() + 2.0, getZ());
        level().addFreshEntity(bolt);
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.HOSTILE, 1.2F, 0.5F);
        }
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
                SoundSource.HOSTILE, 2.0F, 1.0F);
        AABB lineBox = new AABB(origin, origin.add(dir.scale(range))).inflate(2.5);
        for (Player p : sl.getEntitiesOfClass(Player.class, lineBox)) {
            if (p.isCreative() || p.isSpectator()) continue;
            Vec3 toP = p.position().add(0, 1, 0).subtract(origin);
            double along = toP.dot(dir);
            if (along < 0 || along > range) continue;
            Vec3 perp = toP.subtract(dir.scale(along));
            if (perp.length() > 2.0) continue;
            p.hurt(damageSources().sonicBoom(this), 14.0F);
            p.push(dir.x * 1.5, 0.4, dir.z * 1.5);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
    }

    private void groundPound() {
        if (!(level() instanceof ServerLevel sl)) return;
        AABB box = new AABB(blockPosition()).inflate(8.0);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            double dist = p.position().distanceTo(this.position());
            float dmg = (float) Math.max(2.0, 8.0 - dist * 0.5);
            p.hurt(damageSources().mobAttack(this), dmg);
            double dx = p.getX() - getX();
            double dz = p.getZ() - getZ();
            double d = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            p.push(dx / d * 0.9, 1.6, dz / d * 0.9);
            p.hurtMarked = true;
            p.fallDistance = 0F;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
        for (int i = 0; i < 32; i++) {
            double a = (i / 32.0) * Math.PI * 2;
            double r = 6.0;
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    getX() + Math.cos(a) * r,
                    getY() + 0.2,
                    getZ() + Math.sin(a) * r,
                    1, 0, 0, 0, 0);
        }
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                getX(), getY() + 0.4, getZ(),
                120, 4, 0.4, 4, 0.3);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 2.5F, 0.7F);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private void tryTeleportNearPlayer(Player target) {
        if (!(level() instanceof ServerLevel sl)) return;
        double horizDist = Math.hypot(target.getX() - getX(), target.getZ() - getZ());
        if (horizDist <= 12.0) return;

        BlockPos best = null;
        for (int i = 0; i < 8; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2;
            double r = 5 + sl.random.nextDouble() * 3;
            int dx = (int) (Math.cos(a) * r);
            int dz = (int) (Math.sin(a) * r);
            BlockPos cand = target.blockPosition().offset(dx, 0, dz);
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos c = cand.offset(0, dy, 0);
                if (sl.getBlockState(c).isAir()
                        && sl.getBlockState(c.above()).isAir()
                        && sl.getBlockState(c.above().above()).isAir()
                        && !sl.getBlockState(c.below()).isAir()) {
                    best = c;
                    break;
                }
            }
            if (best != null) break;
        }
        if (best == null) return;

        sl.sendParticles(ParticleTypes.PORTAL,
                getX(), getY() + 1.5, getZ(),
                40, 1.0, 1.5, 1.0, 0.5);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 2.0F, 0.5F);

        teleportTo(best.getX() + 0.5, best.getY() + 0.1, best.getZ() + 0.5);

        sl.sendParticles(ParticleTypes.PORTAL,
                getX(), getY() + 1.5, getZ(),
                40, 1.0, 1.5, 1.0, 0.5);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                getX(), getY() + 1.5, getZ(),
                30, 1.2, 1.2, 1.2, 0.05);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.HEART_OF_THE_MOTHER.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_CORE.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_ESSENCE.get(), 4 + random.nextInt(5)));
        spawnAtLocation(new ItemStack(ModItems.TOME_OF_THE_MOTHER.get(), 1));
        if (random.nextFloat() < 0.5F && ModItems.CURSED_IDOL != null && ModItems.CURSED_IDOL.isPresent()) {
            spawnAtLocation(new ItemStack(ModItems.CURSED_IDOL.get(), 1));
        }
        if (random.nextFloat() < 0.35F && ModItems.VEILED_LANTERN != null && ModItems.VEILED_LANTERN.isPresent()) {
            spawnAtLocation(new ItemStack(ModItems.VEILED_LANTERN.get(), 1));
        }
        if (random.nextFloat() < 0.25F && ModItems.PULSING_HEART != null && ModItems.PULSING_HEART.isPresent()) {
            spawnAtLocation(new ItemStack(ModItems.PULSING_HEART.get(), 1));
        }

        if (source.getEntity() instanceof Player p) {
            int orderPieces = 0;
            for (ItemStack s : p.getArmorSlots()) {
                if (!s.isEmpty() && s.getItem() instanceof net.minecraft.world.item.ArmorItem ai) {
                    if (ai.getMaterial().getName().toLowerCase().contains("order")) orderPieces++;
                }
            }
            if (orderPieces >= 2) {
                spawnAtLocation(new ItemStack(ModItems.DESECRATED_HOLY_RELIC.get(), 1));
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }
}
