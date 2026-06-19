package br.com.murilo.liberthia.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS aberto pra qualquer origem (allowedOriginPatterns="*"). Decisão consciente —
 * o backend é o ponto público da arquitetura, e a auth fim-a-fim acontece via
 * X-Liberthia-Token nas requests sensíveis. Frontend pode rodar em qualquer
 * host (dev local, GitHub Pages, Vercel, IP da LAN, etc.).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                // PATCH incluído — sem ele o preflight CORS retorna 403
                // "Invalid CORS request" no PATCH /api/voice/clips/{id} e
                // qualquer outro endpoint que use o método.
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
