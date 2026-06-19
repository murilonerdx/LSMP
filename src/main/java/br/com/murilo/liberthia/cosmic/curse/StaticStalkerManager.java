package br.com.murilo.liberthia.cosmic.curse;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r45: <b>Static Stalker Manager</b> — phantoms invisíveis que
 * spawnam ATRÁS de players e DESAPARECEM quando o player olha.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Phantom é uma POSIÇÃO no mundo (não entidade real)</li>
 *   <li>A cada tick, renderiza partículas {@code vulto_shadow} na posição</li>
 *   <li>Se o player TARGET olha pra posição (dot > 0.85), phantom é REMOVIDO</li>
 *   <li>Lifetime máximo: 200 ticks (10s) — depois despawn silencioso</li>
 *   <li>Quando olha = chance de hallucination</li>
 * </ul>
 *
 * <h2>Spawn API</h2>
 * {@link #spawnAt(ServerLevel, double, double, double, UUID)} cria um phantom
 * visível apenas pelo target player.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class StaticStalkerManager {

    public static final int LIFETIME_TICKS = 200;

    /** Phantom data. */
    public static class Phantom {
        public final Vec3 pos;
        public final UUID targetPlayerId;
        public final long spawnTick;
        public Phantom(Vec3 pos, UUID target, long now) {
            this.pos = pos;
            this.targetPlayerId = target;
            this.spawnTick = now;
        }
    }

    /** Per-level list de phantoms ativos. */
    private static final Map<UUID, java.util.List<Phantom>> ACTIVE =
            new ConcurrentHashMap<>();

    private StaticStalkerManager() {}

    /** Cria um phantom visível só pelo target em (x, y, z). */
    public static void spawnAt(ServerLevel level, double x, double y, double z, UUID targetId) {
        UUID worldId = level.dimension().location().hashCode() == 0
                ? UUID.nameUUIDFromBytes("ow".getBytes())
                : UUID.nameUUIDFromBytes(level.dimension().location().toString().getBytes());
        Phantom p = new Phantom(new Vec3(x, y, z), targetId, level.getGameTime());
        ACTIVE.computeIfAbsent(worldId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(p);
    }

    /** Cleanup quando player desloga. */
    public static void cleanup(UUID playerId) {
        for (var list : ACTIVE.values()) {
            list.removeIf(p -> p.targetPlayerId.equals(playerId));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = event.getServer().overworld().getGameTime();

        for (ServerLevel level : event.getServer().getAllLevels()) {
            UUID worldId = level.dimension().location().hashCode() == 0
                    ? UUID.nameUUIDFromBytes("ow".getBytes())
                    : UUID.nameUUIDFromBytes(level.dimension().location().toString().getBytes());
            java.util.List<Phantom> phantoms = ACTIVE.get(worldId);
            if (phantoms == null || phantoms.isEmpty()) continue;

            Iterator<Phantom> it = phantoms.iterator();
            while (it.hasNext()) {
                Phantom p = it.next();
                // Expiration
                if (now - p.spawnTick > LIFETIME_TICKS) {
                    phantoms.remove(p);
                    continue;
                }
                ServerPlayer target = event.getServer().getPlayerList().getPlayer(p.targetPlayerId);
                if (target == null || target.serverLevel() != level) continue;
                // Distance check — fora de 64b despawn
                if (target.position().distanceTo(p.pos) > 64) {
                    phantoms.remove(p);
                    continue;
                }
                // Check if target is LOOKING at phantom
                Vec3 lookDir = target.getLookAngle();
                Vec3 toPhantom = p.pos.subtract(target.getEyePosition()).normalize();
                double dot = lookDir.dot(toPhantom);
                if (dot > 0.85) {
                    // Observado — despawn + hallucination flash
                    phantoms.remove(p);
                    HallucinationManager.force(target, HallucinationType.SCREEN_GLITCH_BURST,
                            0.4F, 15, "");
                    HallucinationManager.force(target, HallucinationType.FAKE_WHISPER,
                            0.5F, 30, "");
                    continue;
                }
                // Spawn vulto_shadow particles na posição — só o target vê (sendParticles é positional)
                try {
                    level.sendParticles(target,
                            ModParticles.VULTO_SHADOW.get(),
                            true, // forceSpawn (overrides distance limits for this player)
                            p.pos.x, p.pos.y, p.pos.z,
                            3, 0.2, 0.5, 0.2, 0);
                } catch (Throwable ignored) {}

                // 10% chance per tick de heartbeat hallucination
                if (now % 20 == 0 && Math.random() < 0.10) {
                    HallucinationManager.force(target, HallucinationType.HEARTBEAT_PULSE,
                            0.6F, 20, "");
                }
            }
        }
    }
}
