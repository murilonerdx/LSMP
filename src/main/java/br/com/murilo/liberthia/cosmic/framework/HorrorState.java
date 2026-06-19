package br.com.murilo.liberthia.cosmic.framework;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * v0.1.24 r81: Estado persistente de horror de um único player.
 *
 * <p>Cada {@link HorrorType} tem um valor de <b>exposição</b> 0-100.
 * Exposição cresce conforme o player passa tempo expostro ao gatilho
 * (estar perto de entidades, usar items proibidos, ver eventos). Decai
 * lentamente em áreas seguras (overworld diurno, longe de horror).
 *
 * <p>O <b>estabilidade da realidade</b> (0.0 a 1.0) é uma média ponderada
 * das exposições — usado pra decidir intensidade de efeitos visuais (VHS,
 * cromática, partículas).
 *
 * <p>NÃO é serializado pra disk — vive em memória. Pra persistir entre
 * server restarts, salva via NBT no player.persistentData (futuro).
 */
public final class HorrorState {

    private final EnumMap<HorrorType, Float> exposures = new EnumMap<>(HorrorType.class);
    /** Memória de eventos recentes pra evitar repetição. */
    private final Map<String, Long> eventCooldowns = new HashMap<>();
    /** Tick em que entrou na atual dim/posição. */
    private long anchorTick = 0;
    /** Stability final — 1.0 = real estável, 0 = realidade caótica. */
    private float realityStability = 1.0F;
    /** Voltagem audível de whispers (multiplier de chance). */
    private float whisperPressure = 0.0F;
    /** Resistência inata — players podem ganhar/perder. */
    private float resistance = 1.0F;

    public HorrorState() {
        for (HorrorType t : HorrorType.values()) {
            exposures.put(t, 0.0F);
        }
    }

    public float getExposure(HorrorType t) {
        return exposures.getOrDefault(t, 0.0F);
    }

    public void addExposure(HorrorType t, float amount) {
        float current = exposures.getOrDefault(t, 0.0F);
        exposures.put(t, Math.min(100.0F, current + amount / Math.max(0.1F, resistance)));
        recomputeStability();
    }

    public void decayExposure(HorrorType t, float amount) {
        float current = exposures.getOrDefault(t, 0.0F);
        exposures.put(t, Math.max(0.0F, current - amount));
        recomputeStability();
    }

    private void recomputeStability() {
        float total = 0;
        int count = 0;
        for (float v : exposures.values()) {
            total += v;
            count++;
        }
        if (count == 0) {
            realityStability = 1.0F;
            return;
        }
        float avg = total / count / 100.0F;
        realityStability = Math.max(0.0F, 1.0F - avg);
    }

    public float realityStability() {
        return realityStability;
    }

    public float compositeIntensity() {
        return 1.0F - realityStability;
    }

    public float whisperPressure() {
        return whisperPressure;
    }

    public void setWhisperPressure(float v) {
        whisperPressure = Math.max(0.0F, Math.min(10.0F, v));
    }

    public float resistance() {
        return resistance;
    }

    public void setResistance(float v) {
        resistance = Math.max(0.1F, Math.min(10.0F, v));
    }

    /** True se já passou ticks suficiente desde a última vez do evento. */
    public boolean canFireEvent(String eventId, long now, long cooldownTicks) {
        Long last = eventCooldowns.get(eventId);
        if (last == null) return true;
        return (now - last) >= cooldownTicks;
    }

    /** Marca tempo do evento. */
    public void markEventFired(String eventId, long now) {
        eventCooldowns.put(eventId, now);
    }

    public long anchorTick() {
        return anchorTick;
    }

    public void resetAnchor(long now) {
        anchorTick = now;
    }

    /** Retorna o tipo mais agressivo no momento (maior exposição). */
    public HorrorType dominantType() {
        HorrorType best = HorrorType.COSMIC;
        float bestVal = -1;
        for (var e : exposures.entrySet()) {
            if (e.getValue() > bestVal) {
                bestVal = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }
}
