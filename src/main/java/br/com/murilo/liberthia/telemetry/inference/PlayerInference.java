package br.com.murilo.liberthia.telemetry.inference;

import java.util.HashMap;
import java.util.Map;

/**
 * Resultado da inferência sobre o estado mental/comportamental do player.
 * Computado pelo Inferencer a partir das PlayerFeatures.
 *
 * Diferente de features (números) — aqui temos LABELS interpretáveis:
 *   currentGoal      → o que o player provavelmente está tentando fazer
 *   currentMood      → estado emocional inferido
 *   suggestedAction  → o que o servidor PODE fazer pra ajudar/desafiar
 *
 * Cada label vem com confidence [0, 1].
 */
public class PlayerInference {

    /** Hipótese principal do que o player tá fazendo. */
    public String currentGoal = "unknown";
    public double goalConfidence = 0;

    /** Estado emocional inferido. */
    public String currentMood = "neutral";
    public double moodConfidence = 0;

    /** Sugestão de reação do servidor (NÃO é uma decisão, é uma sugestão). */
    public String suggestedAction = "none";
    public double actionConfidence = 0;

    public long computedAt;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("goal", currentGoal);
        m.put("goalConfidence", round(goalConfidence));
        m.put("mood", currentMood);
        m.put("moodConfidence", round(moodConfidence));
        m.put("suggestedAction", suggestedAction);
        m.put("actionConfidence", round(actionConfidence));
        m.put("computedAt", computedAt);
        return m;
    }

    private static double round(double v) { return Math.round(v * 1000) / 1000.0; }
}
