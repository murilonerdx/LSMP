package br.com.murilo.liberthia.telemetry.inference;

import br.com.murilo.liberthia.telemetry.features.PlayerFeatures;
import br.com.murilo.liberthia.telemetry.session.PlayerSession;

/**
 * Sistema de inferência baseado em REGRAS — sem ML ainda.
 *
 * Lê PlayerFeatures + métricas da sessão e decide:
 *   1. currentGoal     (mining, exploring, fighting, idle, building, socializing)
 *   2. currentMood     (focused, frustrated, confused, satisfied, neutral)
 *   3. suggestedAction (none, spawn_helper_npc, drop_hint_item, increase_loot,
 *                        spawn_challenge, send_lore_whisper, cooldown)
 *
 * Cada decisão produz um confidence score [0,1]. Decisões com confidence < 0.4
 * são rebaixadas pra "unknown" pra evitar reações falsas.
 *
 * Esse arquivo é INTENCIONALMENTE explícito (if/else) — quando tivermos
 * dados, plugamos um classificador XGBoost/HMM aqui sem mexer no resto.
 */
public class Inferencer {

    public PlayerInference infer(PlayerSession s) {
        PlayerFeatures f = s.getFeatures();
        PlayerInference out = new PlayerInference();
        out.computedAt = System.currentTimeMillis();

        if (f == null) {
            out.currentGoal = "unknown";
            return out;
        }

        // ===== GOAL ===== (mutuamente exclusivos por hipótese mais forte)
        double maxScore = 0;
        String pickedGoal = "unknown";

        // Mining: muitos blocos quebrados + baixa social + alguma exploração
        double miningScore = f.efficiencyScore * 0.7 + (1 - f.socialScore) * 0.3;
        if (miningScore > maxScore && s.getBlocksBroken() > 30) {
            maxScore = miningScore; pickedGoal = "mining";
        }

        // Exploring: alta exploration + pouca quebra de bloco
        double exploringScore = f.explorationScore * 0.7 + (1 - f.efficiencyScore) * 0.3;
        if (exploringScore > maxScore && f.explorationScore > 0.4) {
            maxScore = exploringScore; pickedGoal = "exploring";
        }

        // Fighting: aggression alta + dano dado/recebido
        double fightingScore = f.aggressionScore;
        if (fightingScore > maxScore && fightingScore > 0.3) {
            maxScore = fightingScore; pickedGoal = "fighting";
        }

        // Building: blocks placed > blocks broken
        double buildScore = clamp((double) s.getBlocksPlaced() / Math.max(1, s.getBlocksBroken() + s.getBlocksPlaced()));
        if (buildScore > maxScore && s.getBlocksPlaced() > 20) {
            maxScore = buildScore; pickedGoal = "building";
        }

        // Socializing: chat alto
        if (f.socialScore > maxScore && f.socialScore > 0.3) {
            maxScore = f.socialScore; pickedGoal = "socializing";
        }

        // Idle: idle time > 1min e baixo em tudo
        if (s.idleTimeMs() > 60_000 && f.explorationScore < 0.1 && f.efficiencyScore < 0.1) {
            maxScore = clamp(s.idleTimeMs() / 300_000.0); pickedGoal = "idle";
        }

        out.currentGoal = pickedGoal;
        out.goalConfidence = maxScore < 0.4 ? 0 : maxScore;

        // ===== MOOD =====
        if (f.frustrationScore > 0.6) {
            out.currentMood = "frustrated";
            out.moodConfidence = f.frustrationScore;
        } else if (f.confusionScore > 0.5) {
            out.currentMood = "confused";
            out.moodConfidence = f.confusionScore;
        } else if (f.efficiencyScore > 0.6 || f.explorationScore > 0.6) {
            out.currentMood = "focused";
            out.moodConfidence = Math.max(f.efficiencyScore, f.explorationScore);
        } else if (s.getKillCount() > 0 && f.aggressionScore > 0.5) {
            out.currentMood = "satisfied";
            out.moodConfidence = f.aggressionScore;
        } else {
            out.currentMood = "neutral";
            out.moodConfidence = 0.5;
        }

        // ===== SUGGESTED ACTION =====
        // Player frustrado → ajuda (hint, loot, NPC)
        // Player focado → não atrapalha (cooldown)
        // Player exploring → spawn algo interessante
        // Player fighting + risk alto → spawn challenge ou drop healing
        if ("frustrated".equals(out.currentMood) && out.moodConfidence > 0.7) {
            if (s.getDeathCount() >= 2) {
                out.suggestedAction = "drop_hint_item";
                out.actionConfidence = 0.8;
            } else {
                out.suggestedAction = "send_lore_whisper";
                out.actionConfidence = 0.6;
            }
        } else if ("focused".equals(out.currentMood)) {
            out.suggestedAction = "cooldown"; // não atrapalha
            out.actionConfidence = 0.9;
        } else if ("exploring".equals(out.currentGoal) && out.goalConfidence > 0.6) {
            out.suggestedAction = "spawn_structure_hint";
            out.actionConfidence = 0.5;
        } else if (f.riskBehavior > 0.7 && "fighting".equals(out.currentGoal)) {
            out.suggestedAction = "drop_healing";
            out.actionConfidence = 0.7;
        } else if ("idle".equals(out.currentGoal) && out.goalConfidence > 0.5) {
            out.suggestedAction = "send_lore_whisper";
            out.actionConfidence = 0.4;
        } else {
            out.suggestedAction = "none";
            out.actionConfidence = 0;
        }

        return out;
    }

    private static double clamp(double v) {
        if (Double.isNaN(v)) return 0;
        return Math.max(0, Math.min(1, v));
    }
}
