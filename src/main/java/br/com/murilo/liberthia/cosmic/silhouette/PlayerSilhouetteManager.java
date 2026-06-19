package br.com.murilo.liberthia.cosmic.silhouette;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r55: <b>Player Silhouette Manager</b> — silhuetas de player
 * que aparecem e somem aleatoriamente perto do player paranóico.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>A cada ~30s (low sanity) ou ~10s (spirit world), tem chance de
 *       spawnar uma silhueta a 10-20 blocos do player</li>
 *   <li>Silhueta é PARTICLE-based (não entity) — só aparece por 3-5s</li>
 *   <li>Cluster de particles formando vagamente uma forma humanóide</li>
 *   <li>Som de breath/whisper acompanha</li>
 *   <li>Se o player olha direto: silhueta some imediatamente</li>
 * </ul>
 *
 * <h2>Por que particles e não entity</h2>
 * Entity tem hitbox, colliders, registro. Particle é leve, cliente-only,
 * e dá o mood perfeito de "vi algo mas sumiu" sem performance overhead.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PlayerSilhouetteManager {

    /** Player UUID → estado da silhueta ativa. */
    private static final Map<UUID, SilhouetteState> ACTIVE = new ConcurrentHashMap<>();
    /** Próximo tick em que pode spawnar nova silhueta. */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    /** Duração mínima de uma silhueta. */
    private static final int MIN_DURATION = 60;  // 3s
    private static final int MAX_DURATION = 100; // 5s

    private PlayerSilhouetteManager() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (sp.tickCount % 10 != 0) return;

        UUID id = sp.getUUID();
        SilhouetteState state = ACTIVE.get(id);

        // Tick ativa silhueta
        if (state != null) {
            state.age++;
            // Verifica se player olhou direto
            Vec3 dir = state.position.subtract(sp.position()).normalize();
            Vec3 look = sp.getLookAngle();
            if (dir.dot(look) > 0.95) {
                // Player viu — some imediatamente
                ACTIVE.remove(id);
                level.sendParticles(ParticleTypes.SMOKE,
                        state.position.x, state.position.y + 1,
                        state.position.z, 8, 0.2, 0.5, 0.2, 0.02);
                return;
            }
            // Spawn cluster de particles formando silhueta humanóide
            spawnSilhouette(level, state.position);
            if (state.age >= state.maxAge) {
                ACTIVE.remove(id);
            }
            return;
        }

        // Sem silhueta — tenta spawnar
        Long cd = COOLDOWNS.get(id);
        if (cd != null && sp.tickCount < cd) return;

        int sanity = SpiritDimension.getSanity(sp);
        boolean inSpirit = SpiritDimension.isInSpiritWorld(sp);
        if (sanity > 50 && !inSpirit) return;

        double chance = inSpirit ? 0.12 : 0.04;
        if (Math.random() > chance) return;

        // Spawn position — lateral random a 10-20b
        double angle = Math.random() * Math.PI * 2;
        double dist = 10 + Math.random() * 10;
        Vec3 pos = sp.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

        SilhouetteState newState = new SilhouetteState();
        newState.position = pos;
        newState.age = 0;
        newState.maxAge = MIN_DURATION + (int)(Math.random() * (MAX_DURATION - MIN_DURATION));
        ACTIVE.put(id, newState);
        COOLDOWNS.put(id, (long)sp.tickCount + (inSpirit ? 200L : 600L));

        // Som soft de presença
        if (Math.random() < 0.5) {
            CosmicSoundManager.playVoidBreathing(sp);
        }
    }

    /** Desenha cluster de particles que forma vagamente uma silhueta humanóide. */
    private static void spawnSilhouette(ServerLevel level, Vec3 pos) {
        // Corpo — 5 particles em cluster vertical (cabeça, peito, abdômen, pernas)
        for (int i = 0; i < 5; i++) {
            double y = i * 0.4; // 0 a 2 blocos
            double dx = (Math.random() - 0.5) * 0.2;
            double dz = (Math.random() - 0.5) * 0.2;
            level.sendParticles(ParticleTypes.SOUL,
                    pos.x + dx, pos.y + y, pos.z + dz,
                    1, 0.05, 0.05, 0.05, 0);
        }
        // Aura escura em volta
        level.sendParticles(ParticleTypes.SMOKE,
                pos.x, pos.y + 1, pos.z,
                3, 0.3, 0.6, 0.3, 0.005);
    }

    public static void cleanup(UUID playerId) {
        ACTIVE.remove(playerId);
        COOLDOWNS.remove(playerId);
    }

    private static class SilhouetteState {
        Vec3 position;
        int age;
        int maxAge;
    }
}
