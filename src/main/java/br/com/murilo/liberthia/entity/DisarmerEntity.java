package br.com.murilo.liberthia.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Disarmer — humanoid mob whose attack snatches the target's main-hand item,
 * drops it on the ground a couple blocks behind the player, and applies brief
 * Mining Fatigue. Inspired by SnatchersFromTheSnatcherers / Pillager-style
 * raiders that strip your loadout.
 *
 * <p>40 HP, 6 ATK, fast walker (0.32). Melee attack rolls a snatch every
 * {@link #SNATCH_INTERVAL_TICKS} successful hits; cooldown stored in NBT to
 * survive chunk reload.
 */
public class DisarmerEntity extends Monster {

    private static final String NBT_LAST_SNATCH = "liberthia_disarmer_last_snatch";
    /** Min ticks between snatches against the same victim, ish. */
    private static final int SNATCH_INTERVAL_TICKS = 60;
    /** v0.1.22: tick interval pro teleport behavior. */
    private static final int TELEPORT_INTERVAL_TICKS = 140; // 7s
    /** Distância mínima do target pra disparar teleport (não TP se já tá perto). */
    private static final double TELEPORT_MIN_DIST = 4.0;
    /** Distância máxima pra TP (sumir do horizonte se target tá longe). */
    private static final double TELEPORT_MAX_DIST = 32.0;
    private int teleportCooldown = 80; // primeiro TP demora um pouco mais

    public DisarmerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.60)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (!hit || !(target instanceof Player victim)) return hit;
        if (victim.isCreative() || victim.isSpectator()) return hit;
        if (!(level() instanceof ServerLevel sl)) return hit;

        long now = victim.tickCount;
        long last = victim.getPersistentData().getLong(NBT_LAST_SNATCH);
        if (now - last < SNATCH_INTERVAL_TICKS) return hit;

        ItemStack mainHand = victim.getMainHandItem();
        if (mainHand.isEmpty()) return hit;

        // Don't disarm cursed/curseofbinding items? — vanilla "binding" is on armor only.
        // Skip if held item has the binding curse though (just in case).
        if (net.minecraft.world.item.enchantment.EnchantmentHelper
                .hasBindingCurse(mainHand)) return hit;

        // Take 1 stack from the player's main hand.
        ItemStack stolen = mainHand.copy();
        victim.setItemInHand(victim.getUsedItemHand(), ItemStack.EMPTY);
        // The above might choose USE_ITEM hand; safer:
        victim.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        victim.getPersistentData().putLong(NBT_LAST_SNATCH, now);

        // Drop the item near the victim with outward push (1.5 blocks behind them).
        Vec3 look = victim.getLookAngle().normalize();
        double dropX = victim.getX() - look.x * 1.6;
        double dropY = victim.getY() + 0.5;
        double dropZ = victim.getZ() - look.z * 1.6;
        ItemEntity drop = new ItemEntity(sl, dropX, dropY, dropZ, stolen);
        drop.setDeltaMovement(-look.x * 0.25, 0.18, -look.z * 0.25);
        drop.setDefaultPickUpDelay();
        sl.addFreshEntity(drop);

        // Effects + particles + sound.
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1, false, true, true));
        sl.sendParticles(ParticleTypes.CRIT,
                victim.getX(), victim.getY() + 1.0, victim.getZ(),
                14, 0.4, 0.4, 0.4, 0.1);
        sl.playSound(null, victim.blockPosition(),
                SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1.0F, 0.5F);
        sl.playSound(null, victim.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.4F);

        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextFloat() < 0.4F
                && br.com.murilo.liberthia.registry.ModItems.CONGEALED_BLOOD.get() != null) {
            spawnAtLocation(new ItemStack(
                    br.com.murilo.liberthia.registry.ModItems.CONGEALED_BLOOD.get()));
        }
    }

    /**
     * v0.1.22: tick override. (1) teleport periódico — se há target válido,
     * cooldown zerou e dist > MIN_DIST, TP atrás/ao lado do target.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (teleportCooldown > 0) { teleportCooldown--; return; }

        var target = getTarget();
        if (!(target instanceof Player p) || p.isCreative() || p.isSpectator()) {
            teleportCooldown = 40;
            return;
        }
        double dist = position().distanceTo(p.position());
        if (dist < TELEPORT_MIN_DIST || dist > TELEPORT_MAX_DIST) {
            teleportCooldown = 40;
            return;
        }
        attemptTeleport(sl, p);
        teleportCooldown = TELEPORT_INTERVAL_TICKS;
    }

    /**
     * Tenta teleportar pra perto do target. Random spot num raio 3 atrás dele.
     */
    private void attemptTeleport(ServerLevel sl, Player target) {
        // Origem atual — pra particles
        Vec3 from = position();

        Vec3 look = target.getLookAngle().normalize();
        // Spot atrás do player, mais um pouco de variação random pra não TP sempre no mesmo ponto
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 2.0 + random.nextDouble() * 1.5;
        double tx = target.getX() - look.x * 2.0 + Math.cos(angle) * radius;
        double tz = target.getZ() - look.z * 2.0 + Math.sin(angle) * radius;
        double ty = target.getY();

        // Particles na origem + destino
        sl.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 0.5, from.z,
                20, 0.4, 0.6, 0.4, 0.1);
        sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE, 1.0F, 0.8F);

        // Vanilla teleportTo respeita colisão de bloco — não atravessa parede
        if (this.randomTeleport(tx, ty, tz, true)) {
            sl.sendParticles(ParticleTypes.PORTAL, tx, ty + 0.5, tz,
                    24, 0.4, 0.6, 0.4, 0.1);
        }
    }

    /**
     * v0.1.22: quando morre, explode em pulso de carne + dano AoE. Drop continua
     * acontecendo via {@link #dropCustomDeathLoot}.
     */
    @Override
    public void die(DamageSource cause) {
        if (level() instanceof ServerLevel sl) {
            // Visual: dois bursts de smoke + dust
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + 0.8, getZ(),
                    25, 0.6, 0.6, 0.6, 0.02);
            sl.sendParticles(ParticleTypes.CRIT,
                    getX(), getY() + 1.0, getZ(),
                    18, 0.5, 0.5, 0.5, 0.2);
            // Som tipo "wet pop"
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 1.2F, 1.4F);

            // Pulso de dano AoE — raio 3.5, 6 HP de dano nos players próximos
            // (não dano contínuo, só pulso único). Não atinge outros mobs de
            // sangue pra não causar cascata. Skip creative/spectator/sigil.
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                    blockPosition()).inflate(3.5);
            for (Player nearby : sl.getEntitiesOfClass(Player.class, box)) {
                if (nearby.isCreative() || nearby.isSpectator()) continue;
                if (br.com.murilo.liberthia.logic.BloodKinPassage.hasSigil(nearby)) continue;
                nearby.hurt(damageSources().mobAttack(this), 6.0F);
                // Empurra
                Vec3 push = nearby.position().subtract(position()).normalize().scale(0.8);
                nearby.push(push.x, 0.4, push.z);
                nearby.hurtMarked = true;
            }
        }
        super.die(cause);
    }
}
