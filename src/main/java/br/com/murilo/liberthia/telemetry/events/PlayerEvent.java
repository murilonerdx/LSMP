package br.com.murilo.liberthia.telemetry.events;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Evento atômico capturado do player. Imutável após criação.
 *
 * Estrutura intencionalmente flat:
 *  - tipo + timestamp + uuid do player
 *  - posição (sempre presente quando relevante)
 *  - data extra em mapa key→value pra evitar 30 subclasses
 *
 * Serializa pra JSON facilmente (data é Map<String,Object>).
 */
public class PlayerEvent {

    private final EventType type;
    private final UUID playerUuid;
    private final long timestamp;   // System.currentTimeMillis()
    private final double x, y, z;
    private final float yaw, pitch;
    private final String dimension;
    private final Map<String, Object> data;

    public PlayerEvent(EventType type, UUID playerUuid, long timestamp,
                       double x, double y, double z,
                       float yaw, float pitch,
                       String dimension,
                       Map<String, Object> data) {
        this.type = type;
        this.playerUuid = playerUuid;
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension == null ? "" : dimension;
        this.data = data == null ? Map.of() : data;
    }

    // ---- Getters ----
    public EventType getType() { return type; }
    public UUID getPlayerUuid() { return playerUuid; }
    public long getTimestamp() { return timestamp; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getDimension() { return dimension; }
    public Map<String, Object> getData() { return data; }

    /** Distância 2D (XZ) em relação a outro evento. */
    public double distanceTo(PlayerEvent other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>(8);
        m.put("type", type.name());
        m.put("uuid", playerUuid.toString());
        m.put("ts", timestamp);
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        m.put("yaw", yaw);
        m.put("pitch", pitch);
        m.put("dim", dimension);
        if (!data.isEmpty()) m.put("data", data);
        return m;
    }

    public static Builder builder(EventType type, UUID uuid) {
        return new Builder(type, uuid);
    }

    public static class Builder {
        private final EventType type;
        private final UUID uuid;
        private long ts = System.currentTimeMillis();
        private double x, y, z;
        private float yaw, pitch;
        private String dim = "";
        private Map<String, Object> data;

        public Builder(EventType type, UUID uuid) {
            this.type = type;
            this.uuid = uuid;
        }
        public Builder at(double x, double y, double z) { this.x = x; this.y = y; this.z = z; return this; }
        public Builder looking(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; return this; }
        public Builder inDim(String d) { this.dim = d == null ? "" : d; return this; }
        public Builder data(String k, Object v) {
            if (data == null) data = new HashMap<>();
            data.put(k, v);
            return this;
        }
        public Builder ts(long t) { this.ts = t; return this; }
        public PlayerEvent build() {
            return new PlayerEvent(type, uuid, ts, x, y, z, yaw, pitch, dim, data);
        }
    }
}
