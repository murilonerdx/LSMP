package br.com.murilo.liberthia.telemetry.features;

import java.util.HashMap;
import java.util.Map;

/**
 * Features extraídas — números normalizados (geralmente 0.0 → 1.0) que
 * resumem o comportamento do player em janelas curtas (~5min).
 *
 * Cada feature é uma hipótese sobre comportamento humano:
 *  - explorationScore   → cobertura espacial nova
 *  - aggressionScore    → razão combate iniciado / dano recebido
 *  - efficiencyScore    → recursos/tempo (blocos quebrados ÷ tempo)
 *  - confusionScore     → mudanças bruscas de direção + inv abre/fecha
 *  - frustrationScore   → mortes + idle + inv abre/fecha
 *  - socialScore        → chat + proximidade de outros
 *
 * NÃO armazenamos features cruas — só o resultado. Recalculado a cada
 * tick do worker (1-5s).
 */
public class PlayerFeatures {

    public double explorationScore;
    public double aggressionScore;
    public double efficiencyScore;
    public double confusionScore;
    public double frustrationScore;
    public double socialScore;
    public double riskBehavior;       // jogador correndo risco (vida baixa, mob denso)

    /** Window: quanto tempo a feature olha pra trás (ms). */
    public long windowMs;
    /** Timestamp da computação. */
    public long computedAt;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("exploration", round(explorationScore));
        m.put("aggression", round(aggressionScore));
        m.put("efficiency", round(efficiencyScore));
        m.put("confusion", round(confusionScore));
        m.put("frustration", round(frustrationScore));
        m.put("social", round(socialScore));
        m.put("risk", round(riskBehavior));
        m.put("windowMs", windowMs);
        m.put("computedAt", computedAt);
        return m;
    }

    private static double round(double v) { return Math.round(v * 1000) / 1000.0; }
}
