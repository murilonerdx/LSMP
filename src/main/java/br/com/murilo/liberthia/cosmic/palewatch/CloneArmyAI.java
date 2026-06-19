package br.com.murilo.liberthia.cosmic.palewatch;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r58: <b>Clone Army AI</b> — drives ReflectionEntity clones that
 * were spawned via CloneArmyItem. They:
 *
 * <ol>
 *   <li>Track entities that hurt their master</li>
 *   <li>Move toward those aggressors</li>
 *   <li>Attack on contact (4 damage + knockback)</li>
 *   <li>Explode in soul-flame + AOE damage on death</li>
 *   <li>Despawn after 5 minutes</li>
 * </ol>
 *
 * <h2>Aggressor tracking</h2>
 * When the master (CloneMaster player) takes damage, we record the attacker
 * UUID into per-master "current_aggressor" map. Clones then path toward
 * that aggressor on next tick.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CloneArmyAI {

    /** Master UUID → current aggressor UUID (entity that just hit master). */
    private static final Map<UUID, UUID> CURRENT_AGGRESSOR = new ConcurrentHashMap<>();
    /** Master UUID → expiration tick of current aggressor entry. */
    private static final Map<UUID, Long> AGGRESSOR_EXPIRY = new HashMap<>();

    private CloneArmyAI() {}

    /** When master takes damage, record attacker. */
    @SubscribeEvent
    public static void onMasterDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player master)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (attacker == master) return;
        CURRENT_AGGRESSOR.put(master.getUUID(), attacker.getUUID());
        AGGRESSOR_EXPIRY.put(master.getUUID(), (long)master.tickCount + 200L); // 10s window
    }

    /** Tick clones: pathfind toward aggressor + attack on contact. */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 10 != 0) return; // 0.5Hz tick

        for (var ent : sl.getAllEntities()) {
            if (!ent.getPersistentData().getBoolean("liberthia.attack_aggressors")) continue;
            if (!(ent instanceof br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity clone)) continue;

            // Despawn check
            long despawnAt = clone.getPersistentData().getLong("liberthia.clone_despawn_at");
            if (despawnAt > 0 && sl.getGameTime() >= despawnAt) {
                onCloneDeath(clone, sl);
                clone.discard();
                continue;
            }

            UUID masterUuid = clone.getPersistentData().getUUID("liberthia.clone_master");
            if (masterUuid == null) continue;

            // Find current aggressor of master
            UUID aggressorUuid = CURRENT_AGGRESSOR.get(masterUuid);
            Long expiry = AGGRESSOR_EXPIRY.get(masterUuid);
            if (expiry != null && clone.tickCount > expiry) {
                CURRENT_AGGRESSOR.remove(masterUuid);
                AGGRESSOR_EXPIRY.remove(masterUuid);
                aggressorUuid = null;
            }

            if (aggressorUuid != null) {
                var aggressor = sl.getEntity(aggressorUuid);
                if (aggressor instanceof LivingEntity le && le.isAlive()) {
                    // Move toward aggressor
                    Vec3 dir = le.position().subtract(clone.position()).normalize();
                    double dist = clone.distanceTo(le);
                    if (dist > 2.0) {
                        clone.setDeltaMovement(dir.x * 0.4, clone.getDeltaMovement().y, dir.z * 0.4);
                        // Look at aggressor
                        clone.getLookControl().setLookAt(le, 30F, 30F);
                    } else {
                        // Attack on contact (every other tick at this rate)
                        if (sl.getGameTime() % 20 == 0) {
                            le.hurt(clone.damageSources().mobAttack(clone), 4.0F);
                            clone.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                            // Particles
                            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                    le.getX(), le.getY() + 1, le.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
                        }
                    }
                    continue;
                }
            }

            // No aggressor — pacify near master (find master and stay close)
            var master = sl.getEntity(masterUuid);
            if (master instanceof Player m) {
                double dist = clone.distanceTo(m);
                if (dist > 8) {
                    Vec3 dirM = m.position().subtract(clone.position()).normalize();
                    clone.setDeltaMovement(dirM.x * 0.2, clone.getDeltaMovement().y, dirM.z * 0.2);
                }
            }
        }
    }

    /** Clone dies — explosion + damage AoE. */
    @SubscribeEvent
    public static void onCloneDieEvent(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof br.com.murilo.liberthia.cosmic.observatory.ReflectionEntity clone)) return;
        if (!clone.getPersistentData().getBoolean("liberthia.explodes_on_death")) return;
        if (!(clone.level() instanceof ServerLevel sl)) return;
        onCloneDeath(clone, sl);
    }

    private static void onCloneDeath(LivingEntity clone, ServerLevel sl) {
        UUID masterUuid = clone.getPersistentData().hasUUID("liberthia.clone_master")
                ? clone.getPersistentData().getUUID("liberthia.clone_master") : null;
        // VFX boom
        sl.sendParticles(ParticleTypes.EXPLOSION,
                clone.getX(), clone.getY() + 0.5, clone.getZ(),
                5, 0.5, 0.5, 0.5, 0);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                clone.getX(), clone.getY() + 0.5, clone.getZ(),
                40, 1, 1, 1, 0.1);
        sl.playSound(null, clone.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 1.0F, 1.2F);

        // AoE damage — exclude master
        for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class,
                clone.getBoundingBox().inflate(3.0))) {
            if (nearby == clone) continue;
            if (masterUuid != null && nearby.getUUID().equals(masterUuid)) continue;
            // Skip other clones
            if (nearby.getPersistentData().getBoolean("liberthia.attack_aggressors")) continue;
            nearby.hurt(clone.damageSources().magic(), 8.0F);
        }
    }
}
