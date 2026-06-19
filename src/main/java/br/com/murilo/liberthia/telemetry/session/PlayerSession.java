package br.com.murilo.liberthia.telemetry.session;

import br.com.murilo.liberthia.telemetry.events.EventType;
import br.com.murilo.liberthia.telemetry.events.PlayerEvent;
import br.com.murilo.liberthia.telemetry.features.PlayerFeatures;
import br.com.murilo.liberthia.telemetry.inference.PlayerInference;
import br.com.murilo.liberthia.telemetry.timeline.Timeline;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estado vivo de um player. Mantido na memória do TelemetryManager por
 * toda a sessão (entre login → logout).
 *
 * Diferente de Timeline (eventos crus), aqui acumulamos métricas agregadas:
 *   • lastPos / lastDim / lastSeenAt → derivam idle/AFK e velocity
 *   • deathCount / kills / blocksBroken → contadores cumulativos da sessão
 *   • lastMoveAt → throttle de MOVE
 *   • totalDistanceXZ → exploração
 *   • features/inference cached → atualizados pelo worker async
 *
 * A timeline mora aqui (1 por player). Quando o player desloga, a
 * sessão é descartada (mas a timeline é flushada pra disco antes).
 */
public class PlayerSession {

    private final UUID uuid;
    private final long startedAt;
    private final Timeline timeline;

    // Última posição conhecida (atualizada por MOVE / LOGIN / DIM_CHANGE)
    private volatile double lastX, lastY, lastZ;
    private volatile float lastYaw, lastPitch;
    private volatile String lastDim = "";
    private volatile long lastSeenAt = System.currentTimeMillis();
    private volatile long lastMoveAt = 0;  // pra throttle MOVE

    // Contadores cumulativos da sessão
    private final AtomicLong deathCount = new AtomicLong(0);
    private final AtomicLong killCount = new AtomicLong(0);
    private final AtomicLong damageDealt = new AtomicLong(0);
    private final AtomicLong damageTaken = new AtomicLong(0);
    private final AtomicLong blocksBroken = new AtomicLong(0);
    private final AtomicLong blocksPlaced = new AtomicLong(0);
    private final AtomicLong chatMessages = new AtomicLong(0);
    private final AtomicLong inventoryOpens = new AtomicLong(0);
    private final AtomicLong craftCount = new AtomicLong(0);
    private final AtomicLong totalDistanceXZ = new AtomicLong(0); // em metros

    // Features computadas + última inferência (atualizado pelo worker)
    private volatile PlayerFeatures features;
    private volatile PlayerInference inference;

    public PlayerSession(UUID uuid) {
        this.uuid = uuid;
        this.startedAt = System.currentTimeMillis();
        this.timeline = new Timeline(2048);  // ~últimas 2k ações; ~5-15min em jogo ativo
    }

    /** Hook chamado pelo TelemetryManager pra cada evento — atualiza estado. */
    public void onEvent(PlayerEvent e) {
        long now = e.getTimestamp();
        lastSeenAt = now;

        switch (e.getType()) {
            case MOVE -> {
                if (lastMoveAt > 0) {
                    double dx = e.getX() - lastX;
                    double dz = e.getZ() - lastZ;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d < 100) totalDistanceXZ.addAndGet((long) d); // ignora teleports
                }
                lastX = e.getX();
                lastY = e.getY();
                lastZ = e.getZ();
                lastYaw = e.getYaw();
                lastPitch = e.getPitch();
                lastMoveAt = now;
                if (!e.getDimension().isEmpty()) lastDim = e.getDimension();
            }
            case LOGIN -> {
                lastX = e.getX(); lastY = e.getY(); lastZ = e.getZ();
                lastDim = e.getDimension();
            }
            case DIMENSION_CHANGE -> lastDim = e.getDimension();
            case DEATH -> deathCount.incrementAndGet();
            case KILL -> killCount.incrementAndGet();
            case DAMAGE_DEALT -> {
                Object dmg = e.getData().get("damage");
                if (dmg instanceof Number n) damageDealt.addAndGet((long) n.doubleValue());
            }
            case DAMAGE_TAKEN -> {
                Object dmg = e.getData().get("damage");
                if (dmg instanceof Number n) damageTaken.addAndGet((long) n.doubleValue());
            }
            case BLOCK_BREAK -> blocksBroken.incrementAndGet();
            case BLOCK_PLACE -> blocksPlaced.incrementAndGet();
            case CHAT -> chatMessages.incrementAndGet();
            case INVENTORY_OPEN, CONTAINER_OPEN -> inventoryOpens.incrementAndGet();
            case CRAFT -> craftCount.incrementAndGet();
            default -> { /* sem ação dedicada — só vai pra timeline */ }
        }

        timeline.add(e);
    }

    // ---- Getters ----
    public UUID getUuid() { return uuid; }
    public long getStartedAt() { return startedAt; }
    public Timeline getTimeline() { return timeline; }
    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getLastZ() { return lastZ; }
    public float getLastYaw() { return lastYaw; }
    public float getLastPitch() { return lastPitch; }
    public String getLastDim() { return lastDim; }
    public long getLastSeenAt() { return lastSeenAt; }
    public long getLastMoveAt() { return lastMoveAt; }
    public long getDeathCount() { return deathCount.get(); }
    public long getKillCount() { return killCount.get(); }
    public long getDamageDealt() { return damageDealt.get(); }
    public long getDamageTaken() { return damageTaken.get(); }
    public long getBlocksBroken() { return blocksBroken.get(); }
    public long getBlocksPlaced() { return blocksPlaced.get(); }
    public long getChatMessages() { return chatMessages.get(); }
    public long getInventoryOpens() { return inventoryOpens.get(); }
    public long getCraftCount() { return craftCount.get(); }
    public long getTotalDistanceXZ() { return totalDistanceXZ.get(); }
    public PlayerFeatures getFeatures() { return features; }
    public PlayerInference getInference() { return inference; }

    public void setFeatures(PlayerFeatures f) { this.features = f; }
    public void setInference(PlayerInference i) { this.inference = i; }

    /** Tempo de sessão até agora em ms. */
    public long sessionDurationMs() { return System.currentTimeMillis() - startedAt; }

    /** Tempo ocioso (sem MOVE) em ms. */
    public long idleTimeMs() {
        if (lastMoveAt == 0) return 0;
        return System.currentTimeMillis() - lastMoveAt;
    }
}
