package br.com.murilo.liberthia.admin.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Distribui eventos do mod (SSE/WebSocket) para engines de background interessadas.
 *
 * Cada engine que precisa reagir a eventos (ex: ForbiddenWords reage a 'chat')
 * se registra via {@link #subscribe}. {@link ModSseListener} bombeia eventos
 * pra cá via {@link #publish}.
 *
 * Pattern thread-safe (CopyOnWriteArrayList) — engines podem ser registradas
 * em qualquer thread, mas publish é chamado da thread SSE.
 */
@Component
public class EngineEventBus {

    private static final Logger LOG = LoggerFactory.getLogger(EngineEventBus.class);
    private final List<Consumer<JsonNode>> subscribers = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void subscribe(Consumer<JsonNode> handler) {
        subscribers.add(handler);
        LOG.info("[EventBus] subscriber added (total={})", subscribers.size());
    }

    public void unsubscribe(Consumer<JsonNode> handler) {
        subscribers.remove(handler);
    }

    /** Chamado pelo ModSseListener com o JSON cru do evento. */
    public void publish(String json) {
        if (subscribers.isEmpty()) return;
        try {
            JsonNode event = mapper.readTree(json);
            for (Consumer<JsonNode> sub : subscribers) {
                try { sub.accept(event); }
                catch (Exception e) { LOG.warn("subscriber threw", e); }
            }
        } catch (Exception e) {
            LOG.warn("invalid event json: {}", e.getMessage());
        }
    }

    /** Útil pra dispatch sintético (ex: testes). */
    public void publishParsed(JsonNode event) {
        for (Consumer<JsonNode> sub : subscribers) {
            try { sub.accept(event); }
            catch (Exception e) { LOG.warn("subscriber threw", e); }
        }
    }
}
