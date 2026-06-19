package br.com.murilo.liberthia.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "mod")
public class ModBridgeConfig {
    private String url = "http://localhost:25580";
    private String token = "";
    private long sseReconnectMs = 5000;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getSseReconnectMs() { return sseReconnectMs; }
    public void setSseReconnectMs(long sseReconnectMs) { this.sseReconnectMs = sseReconnectMs; }

    @Bean
    public WebClient modWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .defaultHeader("X-Liberthia-Token", token)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024)) // 8MB pra /api/items
                .build();
    }
}
