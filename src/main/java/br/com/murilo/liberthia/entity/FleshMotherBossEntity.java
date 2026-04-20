package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.entity.projectile.HemoBoltEntity;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
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

/**
 * Boss entity spawned from the 4-heart FleshMother ritual. Static (no
 * movement AI), 400 HP, 3 phases.
 *   - Phase 1 (100→66%): summon worms + area blood pulses that damage nearby players
 *   - Phase 2 (66→33%):  phase 1 + spit HemoBolts at nearest player; regen shield
 *   - Phase 3 (<33%):    phase 2 + enrage — constant worm summons, periodic
 *                        AOE wither bursts. Shield drops.
 */
public class FleshMotherBossEntity extends Monster {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§4A Mãe de Carne"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    private int wormCooldown = 80;
    private int boltCooldown = 60;
    private int pulseCooldown = 40;
    private int shieldHealTick = 0;

    public FleshMotherBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty — the boss does not path. All combat is tick-driven.
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void pushEntities() {}

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == MobEffects.POISON) return false;
        if (effect.getEffect() == MobEffects.WITHER) return false;
        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()) return false;
        return super.canBeAffected(effect);
    }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Phase 1 & 2: incoming damage reduced by 50% while shield is up
        float phase = getHealth() / getMaxHealth();
        if (phase > 0.33F) {
            amount *= 0.5F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());

        if (level().isClientSide) {
            // Ambient particles
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.DAMAGE_INDICATOR,
                        getX() + (random.nextDouble() - 0.5) * 2,
                        getY() + 1.0 + random.nextDouble() * 1.5,
                        getZ() + (random.nextDouble() - 0.5) * 2,
                        0, 0.05, 0);
            }
            return;
        }

        float hpFrac = getHealth() / getMaxHealth();
        boolean phase2 = hpFrac <= 0.66F;
        boolean phase3 = hpFrac <= 0.33F;

        // --- Target acquisition: nearest living player within 32 ---
        Player nearest = level().getNearestPlayer(this, 32.0);
        if (nearest != null) setTarget(nearest);

        // --- Worm summon ---
        wormCooldown--;
        if (wormCooldown <= 0) {
            wormCooldown = phase3 ? 80 : (phase2 ? 120 : 160);
            summonWorm();
            if (phase3) summonWorm();
        }

        // --- Blood pulse (AOE damage) ---
        pulseCooldown--;
        if (pulseCooldown <= 0) {
            pulseCooldown = phase3 ? 40 : 80;
            bloodPulse(phase3 ? 8.0 : 6.0, phase3 ? 7.0F : 5.0F);
        }

        // --- Hemo bolt (phase 2+) ---
        if (phase2 && nearest != null) {
            boltCooldown--;
            if (boltCooldown <= 0) {
                boltCooldown = phase3 ? 30 : 50;
                fireBolt(nearest);
            }
        }

        // --- Phase 1/2 shield self-heal trickle (caps progress loss) ---
        if (!phase3) {
            shieldHealTick++;
            if (shieldHealTick >= 60) {
                shieldHealTick = 0;
                heal(1.0F);
            }
        }
    }

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
            p.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 120, 1));
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    getX(), getY() + 0.5, getZ(),
                    80, radius / 2, 0.4, radius / 2, 0.3);
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.HOSTILE, 0.8F, 1.3F);
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

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.HEART_OF_THE_MOTHER.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_CORE.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_ESSENCE.get(), 3 + random.nextInt(4)));
        spawnAtLocation(new ItemStack(ModItems.TOME_OF_THE_MOTHER.get(), 1));

        // Desecrated relic drop if killer wore >= 2 Order armor pieces
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player p) {
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
