package br.com.murilo.liberthia.admin.config;

import br.com.murilo.liberthia.admin.mod.LiveEventBroadcaster;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveEventBroadcaster broadcaster;

    public WebSocketConfig(LiveEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(broadcaster, "/ws")
                .setAllowedOriginPatterns("*");  // qualquer origem (LAN, VPS, qualquer dev)
    }
}
