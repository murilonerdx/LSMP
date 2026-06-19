package br.com.murilo.liberthia.client.fog;

import br.com.murilo.liberthia.fog.FogZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado client-side das zonas de neblina (recebido do servidor via
 * {@code FogZonesSyncS2CPacket}). O {@code ClientFogEvents} lê isso pra renderizar.
 */
public final class ClientFogZones {

    private static volatile List<FogZone> zones = new ArrayList<>();

    private ClientFogZones() {}

    public static void set(List<FogZone> newZones) {
        zones = new ArrayList<>(newZones);
    }

    public static void clear() {
        zones = new ArrayList<>();
    }

    /** Lista atual de zonas (pro emissor de partículas iterar). */
    public static List<FogZone> all() {
        return zones;
    }

    /** Resultado do cálculo: cor da névoa + intensidade 0..1 (0 = sem névoa). */
    public static final class Result {
        public final int color;
        public final float strength;
        public Result(int color, float strength) { this.color = color; this.strength = strength; }
    }

    /**
     * Dada a posição da câmera e a dimensão atual, retorna a névoa mais forte
     * que afeta esse ponto. strength = densidade × fade-de-borda (1 = máximo).
     */
    public static Result compute(double camX, double camY, double camZ, String dimNow) {
        List<FogZone> local = zones;
        if (local.isEmpty()) return null;

        int bestColor = 0;
        float bestStrength = 0f;
        for (FogZone z : local) {
            if (!z.dim.equals(dimNow)) continue;
            double dist = Math.sqrt(z.distanceSqTo(camX, camY, camZ));
            if (dist >= z.radius) continue;
            // núcleo (70% interno) = cheio; borda externa faz fade linear
            double core = z.radius * 0.70;
            float fade;
            if (dist <= core) {
                fade = 1f;
            } else {
                fade = (float) ((z.radius - dist) / (z.radius - core));
            }
            float strength = z.density * Math.max(0f, Math.min(1f, fade));
            if (strength > bestStrength) {
                bestStrength = strength;
                bestColor = z.color;
            }
        }
        if (bestStrength <= 0f) return null;
        return new Result(bestColor, bestStrength);
    }
}
