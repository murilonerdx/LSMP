package br.com.murilo.liberthia.telemetry.features;

import br.com.murilo.liberthia.telemetry.events.EventType;
import br.com.murilo.liberthia.telemetry.events.PlayerEvent;
import br.com.murilo.liberthia.telemetry.session.PlayerSession;

import java.util.List;

/**
 * Extrator de features. Recebe a sessão atual + timeline e devolve
 * PlayerFeatures normalizadas.
 *
 * NÃO usa ML — usa heurísticas claras com pesos manuais. Quando tivermos
 * dados suficientes, treinamos um modelo offline pra substituir.
 *
 * Cada score tenta ficar no range [0, 1]. clamp() garante isso.
 */
public class FeatureExtractor {

    /** Janela default: últimos 5 minutos. */
    public static final long DEFAULT_WINDOW_MS = 5 * 60 * 1000L;

    public PlayerFeatures extract(PlayerSession s) {
        return extract(s, DEFAULT_WINDOW_MS);
    }

    public PlayerFeatures extract(PlayerSession s, long windowMs) {
        long now = System.currentTimeMillis();
        long sinceTs = now - windowMs;
        List<PlayerEvent> recent = s.getTimeline().since(sinceTs);

        PlayerFeatures f = new PlayerFeatures();
        f.windowMs = windowMs;
        f.computedAt = now;

        // === EXPLORATION ===
        // Distância percorrida nos últimos windowMs, normalizada.
        // ~500m em 5min = 1.0 (jogando ativamente).
        double distSinceWindow = sumMoveDistance(recent);
        f.explorationScore = clamp(distSinceWindow / 500.0);

        // === AGGRESSION ===
        // Razão dano dado / (dano dado + recebido). 1.0 = só dá dano.
        // Se nenhum combate, 0.
        int dealt = countByType(recent, EventType.DAMAGE_DEALT);
        int taken = countByType(recent, EventType.DAMAGE_TAKEN);
        int kills = countByType(recent, EventType.KILL);
        int totalCombat = dealt + taken;
        if (totalCombat > 0) {
            double ratio = (double) dealt / totalCombat;
            // Boost se também matou — kills indicam vitória, não só dano
            f.aggressionScore = clamp(ratio + (kills * 0.1));
        } else {
            f.aggressionScore = 0;
        }

        // === EFFICIENCY ===
        // Blocos quebrados por minuto ÷ 30 (30/min = farming ativo)
        int blocks = countByType(recent, EventType.BLOCK_BREAK);
        double minutes = windowMs / 60_000.0;
        double blocksPerMin = blocks / Math.max(0.1, minutes);
        f.efficiencyScore = clamp(blocksPerMin / 30.0);

        // === CONFUSION ===
        // Heurística: muitas mudanças de direção + inv abre/fecha sem craft + idle.
        // Pega quantas vezes o yaw mudou mais que 90 graus em 1 segundo.
        int sharpTurns = countSharpTurns(recent);
        int invOpens = countByType(recent, EventType.INVENTORY_OPEN);
        int crafts = countByType(recent, EventType.CRAFT);
        // Inv aberto sem crafting → procurando algo
        double invConfusion = invOpens > 0 ? clamp((double) (invOpens - crafts) / Math.max(1, invOpens)) : 0;
        double turnConfusion = clamp(sharpTurns / 20.0);
        f.confusionScore = clamp((invConfusion + turnConfusion) / 2.0);

        // === FRUSTRATION ===
        // Mortes + idle longo + muitos inv opens sem craft
        int deaths = countByType(recent, EventType.DEATH);
        long idleMs = s.idleTimeMs();
        double deathScore = clamp(deaths / 3.0);          // 3 mortes em 5min = 1.0
        double idleScore = clamp(idleMs / 120_000.0);     // 2min parado = 1.0
        f.frustrationScore = clamp(deathScore * 0.5 + idleScore * 0.2 + f.confusionScore * 0.3);

        // === SOCIAL ===
        // Chat + proximidade de outros
        int chats = countByType(recent, EventType.CHAT);
        int nearby = countByType(recent, EventType.PLAYER_NEARBY);
        f.socialScore = clamp((chats * 0.3 + nearby * 0.1));

        // === RISK ===
        // Dano recebido recente + queda + low health (não temos health direto aqui;
        // proxy = dano recebido alto sem inventário aberto pra comer)
        f.riskBehavior = clamp(taken / 5.0);

        return f;
    }

    private double sumMoveDistance(List<PlayerEvent> events) {
        double total = 0;
        PlayerEvent prev = null;
        for (PlayerEvent e : events) {
            if (e.getType() == EventType.MOVE) {
                if (prev != null) {
                    double d = e.distanceTo(prev);
                    if (d < 100) total += d; // ignora teleports
                }
                prev = e;
            }
        }
        return total;
    }

    private int countByType(List<PlayerEvent> events, EventType t) {
        int n = 0;
        for (PlayerEvent e : events) if (e.getType() == t) n++;
        return n;
    }

    private int countSharpTurns(List<PlayerEvent> events) {
        int turns = 0;
        PlayerEvent prev = null;
        for (PlayerEvent e : events) {
            if (e.getType() != EventType.MOVE) continue;
            if (prev != null) {
                float dyaw = Math.abs(e.getYaw() - prev.getYaw());
                if (dyaw > 180) dyaw = 360 - dyaw; // wrap-around
                long dt = e.getTimestamp() - prev.getTimestamp();
                if (dyaw > 90 && dt < 1000) turns++;
            }
            prev = e;
        }
        return turns;
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) return 0;
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
