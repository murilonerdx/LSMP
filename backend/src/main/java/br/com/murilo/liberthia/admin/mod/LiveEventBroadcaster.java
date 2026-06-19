package br.com.murilo.liberthia.admin.mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantém WebSocket sessions ativas. {@link ModSseListener} chama
 * {@link #broadcast} pra cada evento recebido do mod.
 */
@Component
public class LiveEventBroadcaster extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LiveEventBroadcaster.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        LOG.info("WS connected: {} ({} total)", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        LOG.info("WS disconnected: {} ({} remaining)", session.getId(), sessions.size());
    }

    public void broadcast(String json) {
        TextMessage msg = new TextMessage(json);
        for (WebSocketSession s : sessions) {
            if (!s.isOpen()) {
                sessions.remove(s);
                continue;
            }
            try {
                synchronized (s) {
                    s.sendMessage(msg);
                }
            } catch (IOException e) {
                LOG.warn("WS send failed to {}: {}", s.getId(), e.getMessage());
                sessions.remove(s);
            }
        }
    }

    public int sessionCount() {
        return sessions.size();
    }
}
