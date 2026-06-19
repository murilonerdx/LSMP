package br.com.murilo.liberthia.cosmic.palewatch;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicForceRotationS2CPacket;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
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
 * v0.1.22 r55: <b>Pale Watch Events</b> — handlers passivos pros
 * 5 itens da família Pale Watch.
 *
 * <h2>Funcionalidades</h2>
 * <ul>
 *   <li><b>StaredownPendant</b> (passive): se um player está olhando pro
 *       dono do pendant por >3s, mouse do observer começa a girar
 *       sozinho por 8s (via {@link CosmicForceRotationS2CPacket})</li>
 *   <li><b>ParalyzePendant</b> (passive): quando dono toma dano de uma
 *       entidade, atacante vira slowness 6 + jump prevent por 3s</li>
 *   <li><b>CloneArmy explosion</b>: quando um clone (vex marcado com
 *       NBT liberthia.explodes_on_death) morre, explode em particles
 *       + dano AoE</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PaleWatchEvents {

    /** Player UUID dono pendant → mapa observer UUID → ticks observando. */
    private static final Map<UUID, Map<UUID, Integer>> STARE_TIMERS = new ConcurrentHashMap<>();
    /** Cooldown: observer já foi rotacionado, espera N ticks pra de novo. */
    private static final Map<UUID, Long> ROTATE_COOLDOWNS = new HashMap<>();
    /** Cooldown do ParalyzePendant trigger por dono. */
    private static final Map<UUID, Long> PARALYZE_COOLDOWNS = new HashMap<>();

    private static final int STARE_THRESHOLD_TICKS = 60; // 3s

    private PaleWatchEvents() {}

    /** Driver do Staredown — checa olhar entre players todo segundo. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer owner)) return;
        if (owner.tickCount % 20 != 0) return; // 1Hz

        if (!hasItem(owner, ModItems.STAREDOWN_PENDANT.get())) {
            STARE_TIMERS.remove(owner.getUUID());
            return;
        }

        Map<UUID, Integer> timers = STARE_TIMERS.computeIfAbsent(
                owner.getUUID(), k -> new ConcurrentHashMap<>());

        if (!(owner.level() instanceof ServerLevel level)) return;

        // Pega players próximos
        for (Player nearby : level.players()) {
            if (nearby == owner) continue;
            if (!(nearby instanceof ServerPlayer obs)) continue;
            if (obs.distanceTo(owner) > 24) continue;

            // Checa se obs está olhando pro owner
            Vec3 toOwner = owner.position().subtract(obs.position()).normalize();
            Vec3 look = obs.getLookAngle();
            if (toOwner.dot(look) > 0.95) {
                int t = timers.getOrDefault(obs.getUUID(), 0) + 20;
                timers.put(obs.getUUID(), t);
                if (t >= STARE_THRESHOLD_TICKS) {
                    triggerStaredown(owner, obs);
                    timers.put(obs.getUUID(), 0);
                }
            } else {
                timers.remove(obs.getUUID());
            }
        }
    }

    private static void triggerStaredown(ServerPlayer owner, ServerPlayer observer) {
        UUID obsId = observer.getUUID();
        Long cd = ROTATE_COOLDOWNS.get(obsId);
        if (cd != null && observer.tickCount < cd) return;

        // Disparar mouse-spin: agenda 32 packets sobre 8s (4Hz) com deltas random
        // CosmicForceRotationS2CPacket aplica yaw/pitch delta no client.
        var server = observer.server;
        if (server != null) {
            for (int i = 0; i < 32; i++) {
                final int delay = i * 5; // 250ms entre packets
                server.tell(new net.minecraft.server.TickTask(
                        server.getTickCount() + delay,
                        () -> {
                            try {
                                float yawD = (float)((Math.random() - 0.5) * 18.0);
                                float pitchD = (float)((Math.random() - 0.5) * 8.0);
                                ModNetwork.sendToPlayer(observer,
                                        new CosmicForceRotationS2CPacket(yawD, pitchD));
                            } catch (Throwable ignored) {}
                        }));
            }
        }

        // Particles em volta do observer
        if (observer.level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                    observer.getX(), observer.getY() + 1.5, observer.getZ(),
                    30, 0.5, 1, 0.5, 0.02);
        }

        observer.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§5§o✦ Algo segura seu olhar..."), true);
        owner.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§7§oVocê sente §d" + observer.getName().getString()
                + "§r§7§o olhando pra você."), true);

        ROTATE_COOLDOWNS.put(obsId, (long)observer.tickCount + 400); // 20s cd
    }

    /** Trigger do ParalyzePendant — congela atacante quando dono toma dano. */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) return;
        if (!hasItem(owner, ModItems.PARALYZE_PENDANT.get())) return;

        UUID id = owner.getUUID();
        Long cd = PARALYZE_COOLDOWNS.get(id);
        if (cd != null && owner.tickCount < cd) return;

        // Identifica atacante
        var src = event.getSource();
        if (src == null) return;
        if (!(src.getEntity() instanceof LivingEntity attacker)) return;
        if (attacker == owner) return;

        // Aplica paralisia (3s)
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5, false, true));
        attacker.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128, false, true)); // jump amp negativo
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, false, true));
        attacker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true));

        // VFX
        if (owner.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                    25, 0.3, 0.6, 0.3, 0.03);
        }

        PARALYZE_COOLDOWNS.put(id, (long)owner.tickCount + 200); // 10s cd
        owner.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§5§l✦ §rParalisia Branca ativada."), true);
    }

    /** Clone vex morre — explode. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Vex vex)) return;
        if (!vex.getPersistentData().getBoolean("liberthia.explodes_on_death")) return;

        if (vex.level() instanceof ServerLevel sl) {
            // VFX boom
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    vex.getX(), vex.getY() + 0.5, vex.getZ(),
                    5, 0.5, 0.5, 0.5, 0);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    vex.getX(), vex.getY() + 0.5, vex.getZ(),
                    40, 1, 1, 1, 0.1);
            // Som de explosão
            sl.playSound(null, vex.blockPosition(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.2F);

            // Dano AoE 2.5 blocos
            for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class,
                    vex.getBoundingBox().inflate(2.5))) {
                // Não atinge owner
                if (vex.getPersistentData().hasUUID("liberthia.clone_master")
                        && nearby.getUUID().equals(vex.getPersistentData()
                                .getUUID("liberthia.clone_master"))) continue;
                nearby.hurt(vex.damageSources().magic(), 6.0F);
            }
        }
    }

    /** Helper: checa se o player tem o item em qualquer slot. */
    private static boolean hasItem(Player p, net.minecraft.world.item.Item item) {
        for (ItemStack s : p.getInventory().items) {
            if (s.getItem() == item) return true;
        }
        for (ItemStack s : p.getInventory().armor) {
            if (s.getItem() == item) return true;
        }
        // Offhand
        if (p.getOffhandItem().getItem() == item) return true;
        // TODO: integrar com Curios se disponível
        return false;
    }
}
