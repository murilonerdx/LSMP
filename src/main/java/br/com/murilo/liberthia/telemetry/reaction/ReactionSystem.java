package br.com.murilo.liberthia.telemetry.reaction;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.telemetry.inference.PlayerInference;
import br.com.murilo.liberthia.telemetry.session.PlayerSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Sistema de reação. Recebe a inferência atual + sessão e decide se
 * EXECUTA alguma ação no mundo.
 *
 * MVP — esta versão só LOGA o que faria. Implementações reais (gerar
 * estrutura, spawnar NPC, drop item, sussurro) ficam como hooks
 * registráveis via {@link #registerHandler(String, BiConsumer)}.
 *
 * COOLDOWN GLOBAL POR PLAYER: nunca dispara 2 reações no mesmo player
 * em menos de 60s (configurável). Player focado não vira espetáculo.
 *
 * Thread-safety: chamado pelo worker do TelemetryManager (não main thread).
 * Quando precisar tocar no mundo MC, use server.execute(Runnable).
 */
public class ReactionSystem {

    /** Cooldown mínimo entre 2 reações pro mesmo player (ms). */
    private static final long COOLDOWN_MS = 60_000;

    /** Confidence mínimo pra acionar. Abaixo disso é só logado. */
    private static final double MIN_CONFIDENCE = 0.5;

    private final MinecraftServer server;
    private final Map<UUID, Long> lastReactionAt = new ConcurrentHashMap<>();
    private final Map<String, BiConsumer<ServerPlayer, PlayerSession>> handlers = new ConcurrentHashMap<>();

    public ReactionSystem(MinecraftServer server) {
        this.server = server;
        registerDefaults();
    }

    /**
     * Registra um handler pra uma ação. Hooks externos podem substituir os
     * defaults pra integrar com features existentes do mod (ex: Memory Echo,
     * Dimensional Rift, etc).
     */
    public void registerHandler(String action, BiConsumer<ServerPlayer, PlayerSession> handler) {
        handlers.put(action, handler);
    }

    public void process(PlayerSession session) {
        PlayerInference inf = session.getInference();
        if (inf == null) return;
        if ("none".equals(inf.suggestedAction)) return;
        if (inf.actionConfidence < MIN_CONFIDENCE) return;

        // Cooldown
        long now = System.currentTimeMillis();
        Long last = lastReactionAt.get(session.getUuid());
        if (last != null && (now - last) < COOLDOWN_MS) return;

        // Executa SEMPRE no thread principal do server — handlers tocam world.
        server.execute(() -> dispatch(session, inf));
    }

    private void dispatch(PlayerSession session, PlayerInference inf) {
        ServerPlayer p = server.getPlayerList().getPlayer(session.getUuid());
        if (p == null) return; // desconectou

        BiConsumer<ServerPlayer, PlayerSession> h = handlers.get(inf.suggestedAction);
        if (h == null) {
            LiberthiaMod.LOGGER.debug("[Telemetry] sem handler pra ação '{}', só logando", inf.suggestedAction);
            logOnly(session, inf);
            return;
        }

        lastReactionAt.put(session.getUuid(), System.currentTimeMillis());
        try {
            h.accept(p, session);
            LiberthiaMod.LOGGER.info("[Telemetry] reação '{}' disparada pra {} (conf={})",
                    inf.suggestedAction, p.getName().getString(),
                    String.format("%.2f", inf.actionConfidence));
        } catch (Exception e) {
            LiberthiaMod.LOGGER.warn("[Telemetry] reação '{}' falhou: {}", inf.suggestedAction, e.getMessage());
        }
    }

    private void logOnly(PlayerSession s, PlayerInference inf) {
        LiberthiaMod.LOGGER.info("[Telemetry] DRY-RUN reação='{}' player={} goal={} mood={} conf={}",
                inf.suggestedAction, s.getUuid(), inf.currentGoal, inf.currentMood,
                String.format("%.2f", inf.actionConfidence));
    }

    private void registerDefaults() {
        // Stubs — só logam. Substitua via registerHandler() pra integração real.
        handlers.put("cooldown", (p, s) -> { /* no-op, intencional */ });
        handlers.put("send_lore_whisper", (p, s) -> {
            // TODO: integrar com Memory Echo / Echo Whispers do mod
            LiberthiaMod.LOGGER.info("[Telemetry][stub] enviaria sussurro pra {}", p.getName().getString());
        });
        handlers.put("drop_hint_item", (p, s) -> {
            // TODO: dropar item de dica perto do player
            LiberthiaMod.LOGGER.info("[Telemetry][stub] dropearia hint item pra {}", p.getName().getString());
        });
        handlers.put("drop_healing", (p, s) -> {
            // TODO: dropar poção de cura
            LiberthiaMod.LOGGER.info("[Telemetry][stub] dropearia healing pra {}", p.getName().getString());
        });
        handlers.put("spawn_structure_hint", (p, s) -> {
            // TODO: marcar estrutura próxima na bússola
            LiberthiaMod.LOGGER.info("[Telemetry][stub] daria hint de estrutura pra {}", p.getName().getString());
        });
    }

    public Map<UUID, Long> snapshotCooldowns() {
        return new HashMap<>(lastReactionAt);
    }
}
