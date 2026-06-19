package br.com.murilo.liberthia.admin.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter simples in-memory pra endpoints de login. Mitigação contra
 * brute-force de senha de admin/tester.
 *
 * Política:
 *  - Permite até 5 tentativas POR IP em 1 minuto
 *  - Sucesso reseta o contador
 *  - Bloqueio: retorna 429 com Retry-After de 60s
 *
 * Por IP (não user) — assim attacker não consegue lockar account de outro user
 * só martelando o nick. Se você tá atrás do Cloudflare/Traefik, configurar
 * forwardHeaders.enabled=true pra Tomcat pegar o IP real do X-Forwarded-For.
 *
 * Reset automático: AtomicInteger.get() é checado contra wallclock — se a
 * janela de 60s passou, count zera no próximo hit.
 */
@Component
public class LoginRateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(LoginRateLimiter.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000L; // 1 minuto

    private static final class Counter {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    private final ConcurrentHashMap<String, Counter> byIp = new ConcurrentHashMap<>();

    /**
     * Retorna true se a tentativa pode prosseguir. False = bloqueado.
     */
    public boolean attempt(String ip) {
        if (ip == null || ip.isBlank()) ip = "unknown";
        Counter c = byIp.computeIfAbsent(ip, k -> new Counter());
        long now = System.currentTimeMillis();
        synchronized (c) {
            if (now - c.windowStart >= WINDOW_MS) {
                c.windowStart = now;
                c.count.set(0);
            }
            int n = c.count.incrementAndGet();
            if (n > MAX_ATTEMPTS) {
                LOG.warn("[RateLimit] IP {} bloqueado — {} tentativas em {}s", ip, n, WINDOW_MS / 1000);
                return false;
            }
            return true;
        }
    }

    /** Chamar quando login deu sucesso — reseta contador desse IP. */
    public void onSuccess(String ip) {
        if (ip == null) return;
        Counter c = byIp.get(ip);
        if (c != null) c.count.set(0);
    }

    /** Quantos segundos sobram do bloqueio (pra header Retry-After). */
    public int retryAfterSeconds(String ip) {
        if (ip == null) return (int) (WINDOW_MS / 1000);
        Counter c = byIp.get(ip);
        if (c == null) return 0;
        long remaining = c.windowStart + WINDOW_MS - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }
}
