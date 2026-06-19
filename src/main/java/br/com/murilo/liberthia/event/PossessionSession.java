package br.com.murilo.liberthia.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * v0.1.22 r21: Sessão de posse — UM objeto por par possessor/possuído com TODO
 * o estado relacionado. Antes esse estado estava espalhado em 4 maps estáticos
 * (ACTIVE/REVERSE/ATTACK_BYPASS/LAST_RECOVERY) + iterar levels a cada packet.
 *
 * <h2>Por que esse design</h2>
 * <ul>
 *   <li><b>Cache de entidade:</b> guarda referência ao target. Antes
 *       {@code findEntity} iterava todos os levels a cada packet (20×/s).
 *       Agora é O(1) — só revalida se ficou null/removed.</li>
 *   <li><b>Rate-limit de recovery:</b> {@code lastRecoverySendTick} impede que
 *       servidor mande EndPossession em loop quando client está dessincronizado
 *       (feedback loop que travava o thread).</li>
 *   <li><b>Input buffering:</b> packet handler só atualiza os campos. Server
 *       tick aplica física a 20 Hz fixo (não depende da taxa de packets).</li>
 *   <li><b>Anti-cheat suave:</b> input validation no setInput previne valores
 *       impossíveis (forward=100 etc) que causariam física maluca.</li>
 * </ul>
 */
public final class PossessionSession {

    /** Min ticks entre packets EndPossession enviados pro client (rate-limit). */
    public static final int RECOVERY_COOLDOWN_TICKS = 100; // 5 segundos

    public final UUID possessor;
    public final UUID target;

    /** Cache da entidade target — refrescado quando null/removed. */
    private Entity cachedTarget;

    /** Última input recebida do client. Pode ser lida concorrentemente pelo tick. */
    public volatile float forward = 0f;
    public volatile float strafe = 0f;
    public volatile boolean jump = false;
    public volatile boolean sneak = false;
    public volatile float yaw = 0f;
    public volatile float pitch = 0f;

    /** True se recebemos algum input — primeira física só roda quando true. */
    public volatile boolean hasInput = false;

    /** Tick do último recovery packet enviado (pra rate-limit). */
    public long lastRecoverySendTick = -RECOVERY_COOLDOWN_TICKS;

    /** True enquanto o possuído está executando ATTACK via posse (bypass do handler). */
    public volatile boolean attackInProgress = false;

    public PossessionSession(UUID possessor, UUID target) {
        this.possessor = possessor;
        this.target = target;
    }

    /**
     * Retorna a entidade target. Usa cache, só busca nos levels se necessário.
     * Server-side only — não chamar do thread de network.
     */
    public Entity resolveTarget(MinecraftServer server) {
        if (cachedTarget != null && !cachedTarget.isRemoved()) {
            return cachedTarget;
        }
        // Re-resolve via findEntity
        if (server == null) return null;
        ServerPlayer asPlayer = server.getPlayerList().getPlayer(target);
        if (asPlayer != null) {
            cachedTarget = asPlayer;
            return asPlayer;
        }
        for (var lvl : server.getAllLevels()) {
            Entity e = lvl.getEntity(target);
            if (e != null) {
                cachedTarget = e;
                return e;
            }
        }
        return null;
    }

    /**
     * Atualiza input com validação. Clampa valores fora dos limites pra
     * proteger contra packets bugados (input.forward=100, etc).
     */
    public void setInput(float fw, float st, boolean j, boolean sn, float y, float p) {
        // Clamp forward/strafe pra [-1, 1] — vanilla nunca passa disso
        this.forward = Math.max(-1f, Math.min(1f, fw));
        this.strafe = Math.max(-1f, Math.min(1f, st));
        this.jump = j;
        this.sneak = sn;
        // Yaw/pitch livres (qualquer ângulo válido)
        this.yaw = y;
        // Pitch clamp pra [-90, 90] (vanilla limit)
        this.pitch = Math.max(-90f, Math.min(90f, p));
        this.hasInput = true;
    }

    /** True se input é "neutro" (parado). Permite skip de física desnecessária. */
    public boolean isIdle() {
        return forward == 0f && strafe == 0f && !jump;
    }

    /**
     * True se podemos mandar recovery packet agora (rate-limit). Atualiza
     * lastRecoverySendTick se true.
     */
    public boolean tryRecoverySend(long currentTick) {
        if (currentTick - lastRecoverySendTick < RECOVERY_COOLDOWN_TICKS) {
            return false;
        }
        lastRecoverySendTick = currentTick;
        return true;
    }

    /** Invalida o cache de target (forçar re-resolve no próximo acesso). */
    public void invalidateTarget() {
        cachedTarget = null;
    }
}
