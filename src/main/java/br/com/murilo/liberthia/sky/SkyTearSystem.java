package br.com.murilo.liberthia.sky;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r34: <b>SKY TEAR SYSTEM</b> — sistema de "rasgar o céu" pra invocar
 * entidades colossais. Demo inspirado no YouTube short referenciado.
 *
 * <h2>Sequência cinemática (server-side coordenada)</h2>
 * <ol>
 *   <li><b>Tick 0</b>: Sky Tear triggered no caster. Save state.</li>
 *   <li><b>Tick 0-60</b> (3s): <b>Phase 1 — BUILDUP</b>
 *     <ul>
 *       <li>Tremor: weather thunder + small shake (velocity micro-pulse)</li>
 *       <li>Particles ASH descendendo do céu em volta do caster</li>
 *       <li>Som baixo: PORTAL_AMBIENT pitch 0.3 a cada 5 ticks</li>
 *     </ul>
 *   </li>
 *   <li><b>Tick 60-180</b> (6s): <b>Phase 2 — RASGAR</b>
 *     <ul>
 *       <li>Spawn particles FUNNEL no céu (cone descendente) — DRAGON_BREATH</li>
 *       <li>Camera shake INTENSO: random velocity pulse 0.1 cada tick</li>
 *       <li>Som: WARDEN_ROAR + ENDER_DRAGON_GROWL em pitch 0.2</li>
 *       <li>Sky color override: rojo profundo via DimensionSpecialEffects flag</li>
 *       <li>Lightning estratégico em volta do caster (5 raios sem dano)</li>
 *     </ul>
 *   </li>
 *   <li><b>Tick 180</b>: <b>Phase 3 — DESCIDA</b>
 *     <ul>
 *       <li>Spawn da HeraldEntity no céu (Y +50 acima do caster)</li>
 *       <li>Entity desce devagar (0.2 m/tick) com particles trail</li>
 *       <li>Boss bar aparece</li>
 *       <li>Camera shake reduzida mas constante</li>
 *     </ul>
 *   </li>
 *   <li><b>Tick 180+</b>: <b>Phase 4 — COMBATE</b> — Herald começa AI normal</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SkyTearSystem {

    /** Estados ativos por UUID do caster. */
    private static final Map<UUID, TearEvent> ACTIVE = new HashMap<>();

    public static class TearEvent {
        public final UUID caster;
        public final BlockPos epicenter;
        public final long startTick;
        public int phase = 0;

        public TearEvent(UUID caster, BlockPos epicenter, long startTick) {
            this.caster = caster;
            this.epicenter = epicenter;
            this.startTick = startTick;
        }
    }

    private SkyTearSystem() {}

    /** Inicia uma Sky Tear no caster. */
    public static void trigger(ServerPlayer caster) {
        if (ACTIVE.containsKey(caster.getUUID())) return;
        TearEvent ev = new TearEvent(caster.getUUID(),
                caster.blockPosition(), caster.level().getGameTime());
        ACTIVE.put(caster.getUUID(), ev);
        caster.displayClientMessage(Component.literal(
                "§4§l✦ §r§4O VÉU SE RASGA. §r§coraculo dimensional aberto."), false);
        ((ServerLevel) caster.level()).playSound(null, caster.blockPosition(),
                SoundEvents.WARDEN_EMERGE, SoundSource.MASTER, 4.0F, 0.3F);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;

        java.util.Iterator<Map.Entry<UUID, TearEvent>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            TearEvent ev = it.next().getValue();
            // Acha o caster
            ServerPlayer caster = null;
            ServerLevel level = null;
            for (var lvl : event.getServer().getAllLevels()) {
                caster = lvl.getServer().getPlayerList().getPlayer(ev.caster);
                if (caster != null) {
                    level = (ServerLevel) caster.level();
                    break;
                }
            }
            if (caster == null || level == null) { it.remove(); continue; }

            long elapsed = level.getGameTime() - ev.startTick;
            // Phases
            if (elapsed < 60) {
                phaseBuildup(level, caster, ev, elapsed);
            } else if (elapsed < 180) {
                phaseRasgar(level, caster, ev, elapsed);
            } else if (elapsed == 180) {
                phaseDescend(level, caster, ev);
            } else if (elapsed < 300) {
                phaseCombatPrep(level, caster, ev, elapsed);
            } else {
                it.remove();
            }
        }
    }

    /** Phase 1 — buildup: tremor + ash particles + ominous sound. */
    private static void phaseBuildup(ServerLevel level, ServerPlayer caster,
                                      TearEvent ev, long elapsed) {
        // Particles ASH descendendo
        if (elapsed % 4 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = 5 + level.random.nextDouble() * 10;
                level.sendParticles(ParticleTypes.WHITE_ASH,
                        caster.getX() + Math.cos(a) * r,
                        caster.getY() + 30,
                        caster.getZ() + Math.sin(a) * r,
                        2, 0.3, 0.5, 0.3, 0.05);
            }
        }
        // Tremor leve via velocity pulse (faz player tremer)
        if (elapsed % 8 == 0) {
            Vec3 jitter = new Vec3(
                    (level.random.nextDouble() - 0.5) * 0.05,
                    0,
                    (level.random.nextDouble() - 0.5) * 0.05);
            caster.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(
                    caster.getId(), jitter));
        }
        // Som baixo
        if (elapsed % 15 == 0) {
            level.playSound(null, caster.blockPosition(),
                    SoundEvents.PORTAL_AMBIENT, SoundSource.MASTER, 2.0F, 0.25F);
        }
    }

    /** Phase 2 — rasgar: cone de partículas + camera shake + sky red + lightning. */
    private static void phaseRasgar(ServerLevel level, ServerPlayer caster,
                                     TearEvent ev, long elapsed) {
        long localT = elapsed - 60;
        // CONE DE PARTÍCULAS no céu (descendo do alto)
        double height = 60 - localT * 0.2;  // cone descendo do céu pro caster
        double radius = 8 - localT * 0.05;
        for (int i = 0; i < 12; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = level.random.nextDouble() * radius;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                    caster.getX() + Math.cos(a) * r,
                    caster.getY() + height,
                    caster.getZ() + Math.sin(a) * r,
                    1, 0, -0.3, 0, 0.05);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    caster.getX() + Math.cos(a) * r,
                    caster.getY() + height - 1,
                    caster.getZ() + Math.sin(a) * r,
                    1, 0, -0.1, 0, 0.02);
        }
        // CAMERA SHAKE intenso (todos players no raio 32)
        if (elapsed % 4 == 0) {
            for (ServerPlayer p : level.getPlayers(pl -> pl.distanceTo(caster) < 32)) {
                Vec3 shake = new Vec3(
                        (level.random.nextDouble() - 0.5) * 0.2,
                        (level.random.nextDouble() - 0.5) * 0.05,
                        (level.random.nextDouble() - 0.5) * 0.2);
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(
                        p.getId(), shake));
            }
        }
        // SONS pesados
        if (localT % 20 == 0) {
            level.playSound(null, caster.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.MASTER, 4.0F, 0.2F);
        }
        if (localT % 35 == 0) {
            level.playSound(null, caster.blockPosition(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 3.0F, 0.2F);
        }
        // LIGHTNINGS estratégicos (sem dano)
        if (localT % 25 == 0) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 5 + level.random.nextDouble() * 8;
            var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(caster.getX() + Math.cos(a) * r,
                        caster.getY(),
                        caster.getZ() + Math.sin(a) * r);
                bolt.setVisualOnly(true);  // sem dano
                level.addFreshEntity(bolt);
            }
        }
    }

    /** Phase 3 — descida: spawn Herald + boss bar + comeca a descer. */
    private static void phaseDescend(ServerLevel level, ServerPlayer caster, TearEvent ev) {
        // EXPLOSÃO DE PARTÍCULAS no ponto de spawn (cone fechando)
        for (int i = 0; i < 200; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = level.random.nextDouble() * 6;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    caster.getX() + Math.cos(a) * r,
                    caster.getY() + 50 + level.random.nextDouble() * 5,
                    caster.getZ() + Math.sin(a) * r,
                    2, 0.2, 0.2, 0.2, 0.1);
        }
        // SCREAM ascendente
        level.playSound(null, caster.blockPosition(),
                SoundEvents.WARDEN_ROAR, SoundSource.MASTER, 6.0F, 0.4F);
        // SPAWN HERALD (placeholder — usa Warden + tag pra ser custom)
        var herald = net.minecraft.world.entity.EntityType.WARDEN.create(level);
        if (herald != null) {
            herald.moveTo(caster.getX(), caster.getY() + 45, caster.getZ(),
                    level.random.nextFloat() * 360, 0);
            herald.setCustomName(Component.literal("§4§lHerald do Véu Rasgado"));
            herald.setCustomNameVisible(true);
            herald.setNoGravity(false); // cai devagar
            herald.getPersistentData().putBoolean("liberthia.herald_descending", true);
            herald.getPersistentData().putString("liberthia.herald_caster", ev.caster.toString());
            level.addFreshEntity(herald);
        }
        caster.displayClientMessage(Component.literal(
                "§4§l✦ §r§4ALGO DESCE DO RASGO..."), false);
    }

    /** Phase 4 — sustains screen shake + ambient sound enquanto Herald desce. */
    private static void phaseCombatPrep(ServerLevel level, ServerPlayer caster,
                                         TearEvent ev, long elapsed) {
        if (elapsed % 6 == 0) {
            for (ServerPlayer p : level.getPlayers(pl -> pl.distanceTo(caster) < 48)) {
                Vec3 shake = new Vec3(
                        (level.random.nextDouble() - 0.5) * 0.08,
                        0,
                        (level.random.nextDouble() - 0.5) * 0.08);
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(
                        p.getId(), shake));
            }
        }
    }
}
