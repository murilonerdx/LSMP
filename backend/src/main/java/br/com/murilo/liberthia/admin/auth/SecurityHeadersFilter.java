package br.com.murilo.liberthia.admin.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adiciona headers de segurança em todas as respostas HTTP da API.
 *
 * Roda ANTES do {@link AuthFilter} (order=HIGHEST_PRECEDENCE) pra garantir que
 * mesmo respostas 401 e 404 levem os headers.
 *
 * Defesa em camadas — mitigação de XSS, clickjacking, MIME sniffing, e
 * exposição de info em headers padrão do Tomcat.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        // CSP — restringe origens de scripts/styles/imagens.
        // 'self' permite recursos do próprio domínio. unsafe-inline em style/script
        // é necessário pelo Vite/Tailwind (CSS-in-JS) — em ambiente full lockdown
        // poderia usar nonces. data: pra imagens base64 e blob: pra audio.
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob: https:; " +
                "media-src 'self' blob: data:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' wss: https:; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'");

        // Bloqueia o site dentro de iframes (clickjacking)
        resp.setHeader("X-Frame-Options", "DENY");

        // Bloqueia o browser de "adivinhar" MIME type
        resp.setHeader("X-Content-Type-Options", "nosniff");

        // Não vaza referer pra outros sites
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions policy — desabilita features sensíveis do browser
        resp.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=()");

        // HSTS — força HTTPS por 1 ano (só faz sentido em prod com HTTPS)
        // O Traefik termina TLS, então a app não sabe se é HTTPS ou não.
        // Verifica via X-Forwarded-Proto (header padrão do Traefik).
        String proto = req.getHeader("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(proto)) {
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        chain.doFilter(req, resp);
    }
}
