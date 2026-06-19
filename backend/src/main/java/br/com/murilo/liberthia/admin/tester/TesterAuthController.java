package br.com.murilo.liberthia.admin.tester;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Endpoints públicos do tester (não usa AuthFilter do admin).
 *  - POST /api/tester/auth/register  → { mcName, code, password }
 *  - POST /api/tester/auth/login     → { mcName, password }
 *  - GET  /api/tester/auth/me        → header Authorization: Bearer <tester-token>
 *
 * Endpoints admin:
 *  - POST /api/admin/tester/invites  → cria código (precisa de admin JWT)
 *  - GET  /api/admin/tester/invites  → lista códigos
 *  - GET  /api/admin/tester/ranking  → top 20 testers
 */
@RestController
public class TesterAuthController {

    private final ModTesterService svc;
    private final br.com.murilo.liberthia.admin.auth.LoginRateLimiter rateLimiter;

    public TesterAuthController(ModTesterService svc,
                                br.com.murilo.liberthia.admin.auth.LoginRateLimiter rateLimiter) {
        this.svc = svc;
        this.rateLimiter = rateLimiter;
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma >= 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }

    public record RegisterRequest(String mcName, String code, String password) {}
    public record LoginRequest(String mcName, String password) {}

    @PostMapping("/api/tester/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        try {
            ModTester t = svc.register(body.mcName(), body.code(), body.password());
            String token = svc.issueToken(t.getMcName());
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "token", token,
                    "tester", toDto(t)
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false, "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "ok", false, "error", "interno: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/api/tester/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body,
                                   jakarta.servlet.http.HttpServletRequest httpReq) {
        // Rate limit: 5 tentativas/min por IP
        String ip = clientIp(httpReq);
        if (!rateLimiter.attempt(ip)) {
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(rateLimiter.retryAfterSeconds(ip)))
                    .body(Map.of(
                            "ok", false,
                            "error", "Muitas tentativas de login. Espera 1 minuto e tenta de novo.",
                            "reason", "rate_limited"
                    ));
        }
        var r = svc.loginWithReason(body.mcName(), body.password());
        if (r.result().isEmpty()) {
            // Mensagens específicas pra UX
            String reason = r.failReason() == null ? "wrong_password" : r.failReason();
            String userMessage = switch (reason.startsWith("account_banned") ? "account_banned" : reason) {
                case "account_not_found" -> "Nick MC não encontrado. Verifica se digitou certo, ou se você nunca se cadastrou ainda.";
                case "account_banned" -> {
                    String banReason = reason.length() > "account_banned".length() + 2
                            ? reason.substring("account_banned: ".length()) : "";
                    yield "Sua conta foi banida" + (banReason.isBlank() ? "." : ": " + banReason);
                }
                case "pending_claim" -> "Sua conta foi criada mas você ainda não definiu a senha. Use o link de claim que o admin te mandou (página /tester/claim).";
                case "wrong_password" -> "Senha incorreta. Se esqueceu, peça pro admin resetar.";
                case "invalid_input" -> "Preencha nick MC e senha.";
                default -> "Credenciais inválidas (" + reason + ")";
            };
            return ResponseEntity.status(401).body(Map.of(
                    "ok", false,
                    "error", userMessage,
                    "reason", reason
            ));
        }
        ModTester t = r.result().get();
        rateLimiter.onSuccess(ip);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "token", svc.issueToken(t.getMcName()),
                "tester", toDto(t)
        ));
    }

    @GetMapping("/api/tester/auth/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        String mcName = extractMcName(req);
        if (mcName == null) {
            return ResponseEntity.status(401).body(Map.of("error", "token inválido"));
        }
        return svc.findByMcName(mcName)
                .map(t -> ResponseEntity.ok(toDto(t)))
                .orElse(ResponseEntity.status(404).body(null));
    }

    // ============ ADMIN ============

    @PostMapping("/api/admin/tester/invites")
    public Map<String, Object> createInvite(@RequestBody Map<String, String> body) {
        String note = body.getOrDefault("note", "");
        InviteCode inv = svc.createInvite("admin", note);
        return Map.of(
                "ok", true,
                "code", inv.getCode(),
                "id", inv.getId(),
                "note", inv.getNote() == null ? "" : inv.getNote()
        );
    }

    @GetMapping("/api/admin/tester/invites")
    public Map<String, Object> listInvites(@RequestParam(defaultValue = "false") boolean onlyUnused) {
        List<InviteCode> list = svc.listInvites(onlyUnused);
        List<Map<String, Object>> out = new ArrayList<>();
        for (InviteCode i : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("code", i.getCode());
            m.put("createdBy", i.getCreatedBy());
            m.put("createdAt", i.getCreatedAt() == null ? null : i.getCreatedAt().toString());
            m.put("usedBy", i.getUsedBy());
            m.put("usedAt", i.getUsedAt() == null ? null : i.getUsedAt().toString());
            m.put("note", i.getNote());
            out.add(m);
        }
        return Map.of("invites", out, "count", out.size());
    }

    /** Deleta um código de invite (qualquer estado: usado ou não). */
    @DeleteMapping("/api/admin/tester/invites/{id}")
    public Map<String, Object> deleteInvite(@PathVariable Long id) {
        try {
            svc.deleteInvite(id);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    /** Lista TODOS os testers (não só top 20). */
    @GetMapping("/api/admin/tester/testers")
    public Map<String, Object> listAllTesters() {
        List<ModTester> list = svc.listAllTesters();
        List<Map<String, Object>> out = new ArrayList<>();
        for (ModTester t : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("mcName", t.getMcName());
            m.put("points", t.getPoints());
            m.put("enabled", t.isEnabled());
            m.put("inviteCodeUsed", t.getInviteCodeUsed());
            m.put("createdAt", t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
            m.put("lastLoginAt", t.getLastLoginAt() == null ? null : t.getLastLoginAt().toString());
            out.add(m);
        }
        return Map.of("testers", out, "count", out.size());
    }

    public record BanRequest(String reason) {}

    @PostMapping("/api/admin/tester/testers/{mcName}/ban")
    public Map<String, Object> banTester(@PathVariable String mcName, @RequestBody(required = false) BanRequest body) {
        try {
            ModTester t = svc.banTester(mcName, body == null ? null : body.reason());
            return Map.of("ok", true, "mcName", t.getMcName(), "enabled", t.isEnabled());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @PostMapping("/api/admin/tester/testers/{mcName}/unban")
    public Map<String, Object> unbanTester(@PathVariable String mcName) {
        try {
            ModTester t = svc.unbanTester(mcName);
            return Map.of("ok", true, "mcName", t.getMcName(), "enabled", t.isEnabled());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @DeleteMapping("/api/admin/tester/testers/{mcName}")
    public Map<String, Object> deleteTester(@PathVariable String mcName) {
        try {
            svc.deleteTester(mcName);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @GetMapping("/api/admin/tester/ranking")
    public Map<String, Object> ranking() {
        List<ModTester> list = svc.ranking();
        List<Map<String, Object>> out = new ArrayList<>();
        int rank = 1;
        for (ModTester t : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("mcName", t.getMcName());
            m.put("points", t.getPoints());
            m.put("createdAt", t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
            m.put("lastLoginAt", t.getLastLoginAt() == null ? null : t.getLastLoginAt().toString());
            m.put("enabled", t.isEnabled());
            out.add(m);
        }
        return Map.of("ranking", out, "count", out.size());
    }

    // ============ HELPERS ============

    private String extractMcName(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        return svc.validateToken(h.substring(7));
    }

    private Map<String, Object> toDto(ModTester t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("mcName", t.getMcName());
        m.put("points", t.getPoints());
        m.put("createdAt", t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
        m.put("lastLoginAt", t.getLastLoginAt() == null ? null : t.getLastLoginAt().toString());
        return m;
    }
}
