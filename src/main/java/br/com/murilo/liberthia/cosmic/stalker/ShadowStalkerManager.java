package br.com.murilo.liberthia.cosmic.stalker;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r55: <b>Shadow Stalker</b> — entidade sombra que SÓ aparece pra
 * um player específico. Comportamento:
 *
 * <ol>
 *   <li>Spawna a 30-40 blocos do player (silhueta distante)</li>
 *   <li>Quando o player olha pra ela, ela se aproxima quando ele desvia
 *       o olhar (next teleport ~5 blocos mais perto)</li>
 *   <li>Quando atinge 10 blocos: dano alto + grito + despawn</li>
 *   <li>Existe só pro player atual — outros players próximos NÃO vêem
 *       (entity é invisible/glowing pra eles)</li>
 * </ol>
 *
 * <p>Implementação simplificada: usa um Vex com nome "§0§lSombra" e
 * MobEffect.INVISIBILITY pra simular silhueta. Particle SOUL spawnam
 * envolvendo ela quando o player olha.
 *
 * <h2>Trigger</h2>
 * Disparado quando o player tem sanity {@code < 40} em qualquer dimensão.
 * Cooldown de 5 min por player.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ShadowStalkerManager {

    /** Player UUID → distância atual da sombra. */
    private static final Map<UUID, StalkerState> STATES = new ConcurrentHashMap<>();
    /** Próximo tick em que o player pode spawnar um stalker (cooldown). */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    /** Distância inicial. */
    private static final double START_DISTANCE = 35.0;
    /** Distância em que mata o player. */
    private static final double KILL_DISTANCE = 9.0;
    /** Passo de aproximação quando o player desvia. */
    private static final double STEP_CLOSER = 4.0;
    /** Cooldown entre spawns. */
    private static final long COOLDOWN_TICKS = 6000; // 5 min

    private ShadowStalkerManager() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        // Tick rate baixo
        if (sp.tickCount % 20 != 0) return;

        UUID id = sp.getUUID();
        StalkerState state = STATES.get(id);

        if (state == null) {
            // Tenta spawnar — chance baixa, baseado em sanity
            tryToSpawn(sp, level, id);
            return;
        }

        // Tem stalker ativo — verifica se player está olhando pra ele
        Vec3 toStalker = state.position.subtract(sp.position()).normalize();
        Vec3 look = sp.getLookAngle();
        double dot = toStalker.dot(look);

        // Player olhando direto: stalker fica congelado
        if (dot > 0.85) {
            // Spawn particles em volta
            level.sendParticles(ParticleTypes.SOUL,
                    state.position.x, state.position.y + 1, state.position.z,
                    3, 0.3, 0.6, 0.3, 0.01);
            state.lastSeenAtTick = sp.tickCount;
            return;
        }

        // Player desviou e estava olhando há pouco — STALKER se aproxima
        if (sp.tickCount - state.lastSeenAtTick > 20 && state.lastSeenAtTick > 0) {
            state.lastSeenAtTick = -1;
            // Move stalker ~4 blocos mais perto
            Vec3 dirToPlayer = sp.position().subtract(state.position).normalize();
            state.position = state.position.add(dirToPlayer.scale(STEP_CLOSER));
            double dist = state.position.distanceTo(sp.position());

            // Som de teleport
            CosmicSoundManager.playTendrilMovement(sp);

            // Check se chegou
            if (dist < KILL_DISTANCE) {
                onContact(sp, level, state);
                STATES.remove(id);
                return;
            }
            // Particles na nova posição
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    state.position.x, state.position.y + 1.5, state.position.z,
                    20, 0.4, 0.8, 0.4, 0.02);
            // r80: removido chat spam "algo se moveu..." — player percebe pelos
            // sons + particles. Mensagem ficava aparecendo em loop na Spirit World.
        }

        // Particles permanentes na posição (subtle)
        if (sp.tickCount % 40 == 0) {
            level.sendParticles(ParticleTypes.SMOKE,
                    state.position.x, state.position.y + 1, state.position.z,
                    2, 0.2, 0.3, 0.2, 0);
        }
    }

    private static void tryToSpawn(ServerPlayer sp, ServerLevel level, UUID id) {
        // Cooldown
        Long cd = COOLDOWNS.get(id);
        if (cd != null && sp.tickCount < cd) return;

        // Sanity-based trigger
        int sanity = SpiritDimension.getSanity(sp);
        boolean inSpirit = SpiritDimension.isInSpiritWorld(sp);
        // Sanity baixo OU está no spirit world
        if (sanity > 40 && !inSpirit) return;

        // Chance: a cada tick (20Hz), 0.5% se sanity baixo, 2% se spirit
        double chance = inSpirit ? 0.02 : 0.005;
        if (Math.random() > chance) return;

        // Spawn position — atrás do player a START_DISTANCE
        double angle = Math.random() * Math.PI * 2;
        Vec3 pos = sp.position().add(Math.cos(angle) * START_DISTANCE,
                0, Math.sin(angle) * START_DISTANCE);

        StalkerState state = new StalkerState();
        state.position = pos;
        state.spawnTick = sp.tickCount;
        state.lastSeenAtTick = -1;
        STATES.put(id, state);
        COOLDOWNS.put(id, (long) sp.tickCount + COOLDOWN_TICKS);

        LiberthiaMod.LOGGER.debug("[ShadowStalker] spawned for {} at {} (sanity={})",
                sp.getName().getString(), pos, sanity);

        // r80: removido chat spam "você sente um olhar..." — som distante
        // (whispers) é suficiente pra indicar o spawn sem floodar o chat.
        CosmicSoundManager.playDistantWhispers(sp);
    }

    private static void onContact(ServerPlayer sp, ServerLevel level, StalkerState state) {
        // Grito ensurdecedor
        CosmicSoundManager.playDistantScream(sp);
        // Dano e blindness
        sp.hurt(sp.damageSources().magic(), 12.0F);
        sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));

        // Burst particle
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                sp.getX(), sp.getY() + 1, sp.getZ(), 60, 1, 1, 1, 0.1);

        sp.displayClientMessage(Component.literal(
                "§4§l✦ Algo chegou perto demais."), true);
    }

    /** Limpa state quando player desloga. */
    public static void cleanup(UUID playerId) {
        STATES.remove(playerId);
        COOLDOWNS.remove(playerId);
    }

    private static class StalkerState {
        Vec3 position;
        int spawnTick;
        int lastSeenAtTick; // -1 = não olhou ainda; >0 = tick que viu pela última vez
    }
}
