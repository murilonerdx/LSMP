package br.com.murilo.liberthia.admin.auth;

import br.com.murilo.liberthia.admin.mod.ModRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bloqueia requests sem token válido em /api/*. Liberados:
 *  - /api/auth/* (login + check)
 *  - /api/health
 *  - /api/mod/register, /api/mod/status (chamados pelo mod, sem auth)
 *  - OPTIONS (CORS preflight)
 *  - WebSocket upgrade (/ws) — separado do filtro web
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthFilter.class);
    private final AuthService auth;

    /**
     * Token compartilhado mod↔backend (env MOD_TOKEN). O ExposurePhotoWatcher do
     * mod manda esse token no header X-Liberthia-Token quando faz upload de fotos.
     * Usamos pra autenticar uploads do mod sem precisar JWT do admin.
     *
     * <p>NOTA: este é só o valor de bootstrap do {@code application.yml}. Quando
     * o operador atualiza o token via painel ({@code /api/admin/mod-config/token})
     * ou o mod re-registra, o token "real" passa a viver no {@link ModRegistry}
     * (persistido em backend_config no DB). Por isso preferimos
     * {@code registry.getToken()} quando ele tá inicializado.
     */
    @Value("${mod.token:}")
    private String modToken;

    /**
     * Lazy pra evitar ciclo no boot (ModRegistry depende de coisas que dependem
     * de Spring MVC, e o AuthFilter é registrado bem cedo).
     */
    private final ModRegistry registry;

    public AuthFilter(AuthService auth, @Lazy ModRegistry registry) {
        this.auth = auth;
        this.registry = registry;
    }

    /** Token corrente — prefere o do registry (DB override), fallback no env. */
    private String currentModToken() {
        try {
            String t = registry.getToken();
            if (t != null && !t.isBlank()) return t;
        } catch (Exception ignored) {
            // registry ainda não pronto (boot muito cedo) — cai no env
        }
        return modToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String rawPath = req.getRequestURI();
        // Normaliza barras duplicadas (//api/... → /api/...) pra evitar bypass
        // acidental do isPublic quando o cliente concatena URL com trailing slash.
        String path = rawPath == null ? "" : rawPath.replaceAll("/{2,}", "/");
        String method = req.getMethod();

        // Log /ws hits — útil pra diagnosticar problemas de WebSocket
        if (path != null && path.startsWith("/ws")) {
            LOG.info("[AuthFilter] /ws request method={} upgrade={} origin={}",
                    method, req.getHeader("Upgrade"), req.getHeader("Origin"));
        }

        if ("OPTIONS".equalsIgnoreCase(method) || isPublic(path)) {
            chain.doFilter(req, resp);
            return;
        }

        // Upload de fotos do mod Liberthia (ExposurePhotoWatcher) — usa
        // X-Liberthia-Token em vez de Bearer JWT. Aceita SOMENTE POST /api/photos.
        if ("POST".equalsIgnoreCase(method) && "/api/photos".equals(path)) {
            String modTok = req.getHeader("X-Liberthia-Token");
            String expected = currentModToken();
            if (modTok != null && !modTok.isBlank()
                    && (expected == null || expected.isBlank() || expected.equals(modTok))) {
                chain.doFilter(req, resp);
                return;
            }
        }

        String header = req.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        // Fallback: ?token= na query
        if (token == null) token = req.getParameter("token");

        if (token == null || !auth.validate(token)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"login required\"}");
            return;
        }
        chain.doFilter(req, resp);
    }

    private boolean isPublic(String path) {
        return path.startsWith("/api/auth/")
                || path.equals("/api/health")
                // Status público do mod — frontend faz polling pra mostrar banner
                // "Mod offline" sem precisar de auth (usado antes do login também).
                || path.equals("/api/mod-status")
                // TODOS os endpoints /api/tester/* (não /api/admin/tester/*) são
                // públicos do ponto de vista do AuthFilter — eles usam JWT
                // diferente do admin (HMAC com secret separado). Cada controller
                // de tester valida o token via ModTesterService.validateToken
                // internamente.
                //
                // Antes era só /api/tester/auth/* e /api/tester/apply, então
                // tester chamando /api/tester/server-info caía na validação ADMIN
                // → 401 mesmo com token válido → frontend redirecionava pra
                // login com "sessão expirou" (BUG da v96).
                //
                // CUIDADO: /api/admin/tester/* NÃO bate aqui pq não começa com
                // /api/tester/ — admin endpoints continuam exigindo token admin.
                || path.startsWith("/api/tester/")
                // Feature wiki é pública (somente leitura por slug/lista)
                || path.startsWith("/api/feature-wiki/") || path.equals("/api/feature-wiki")
                || path.startsWith("/api/wiki/") || path.equals("/api/wiki")
                || path.startsWith("/api/mod/register")
                || path.startsWith("/api/mod/status")
                // Voz: mod faz upload de clipes via /api/mod/voice/*
                //   autenticando com X-Liberthia-Token. Filtro Bearer ignora.
                || path.startsWith("/api/mod/voice/")
                // Telemetria: mod manda snapshots em batch a cada ~10s.
                //   autenticando com X-Liberthia-Token. Controller valida token.
                || path.startsWith("/api/mod/telemetry/")
                // Resource pack precisa ser baixado pelo cliente MC, que NÃO manda Authorization
                || path.equals("/api/sounds-pack.zip")
                || path.equals("/api/particles-pack.zip")
                // Áudio do Etched: client do mod baixa diretamente, sem header de auth.
                // Path tem extensão .mp3 — só serve arquivos extraídos via yt-dlp.
                || path.startsWith("/api/etched/audio/")
                // Vídeos PÚBLICOS — quem tem o link de /watch/{id} pode ver sem login.
                // Listing, detalhe, e stream do MP4 ficam liberados aqui;
                // operações admin (POST, PUT, DELETE) ficam em /api/videos (com auth).
                || path.startsWith("/api/public/")
                // WebSocket: browser não consegue mandar header Authorization no upgrade.
                // O handshake do WS passa por aqui antes do Spring detectar a upgrade.
                // CORS já protege via setAllowedOriginPatterns no WebSocketConfig.
                || path.equals("/ws")
                || path.startsWith("/ws/");
    }
}
