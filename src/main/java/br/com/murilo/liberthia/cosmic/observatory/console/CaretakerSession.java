package br.com.murilo.liberthia.cosmic.observatory.console;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r50: Sessão ativa do Caretaker Console — admin definiu mensagens
 * pra injetar em um target player específico.
 *
 * <h2>Per-target</h2>
 * Cada player pode ter no máximo 1 sessão ativa por vez. Reinjetar substitui.
 *
 * <h2>Mensagens</h2>
 * Lista de strings (já formatadas com §-codes). O server escolhe random a
 * cada N segundos e manda APENAS pra esse target via FAKE_CHAT_MESSAGE.
 */
public class CaretakerSession {

    /** Target UUID. */
    public final UUID targetId;

    /** Lista de mensagens pra injetar (cada uma já formatada). */
    public final List<String> messages;

    /** Intervalo entre mensagens em ticks (default 600 = 30s). */
    public int intervalTicks;

    /** Lifetime restante em ticks (-1 = permanente). */
    public int remainingTicks;

    /** Quando próxima mensagem dispara (gameTime). */
    public long nextFireTick;

    /** Quem criou a sessão (admin/caretaker). */
    public final UUID createdBy;

    public CaretakerSession(UUID targetId, List<String> messages,
                            int intervalTicks, int remainingTicks, UUID createdBy) {
        this.targetId = targetId;
        this.messages = new ArrayList<>(messages);
        this.intervalTicks = Math.max(60, intervalTicks); // min 3s
        this.remainingTicks = remainingTicks;
        this.createdBy = createdBy;
        this.nextFireTick = 0;
    }

    public String pickRandomMessage() {
        if (messages.isEmpty()) return null;
        return messages.get((int)(Math.random() * messages.size()));
    }
}
