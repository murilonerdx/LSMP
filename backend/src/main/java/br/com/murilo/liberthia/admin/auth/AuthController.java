package br.com.murilo.liberthia.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthService auth, LoginRateLimiter rateLimiter) {
        this.auth = auth;
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

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> body,
                                                     HttpServletRequest httpReq) {
        // Rate limit: 5 tentativas/min por IP (mesma política do tester login)
        String ip = clientIp(httpReq);
        if (!rateLimiter.attempt(ip)) {
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(rateLimiter.retryAfterSeconds(ip)))
                    .body(Map.of(
                            "error", "rate_limited",
                            "message", "Muitas tentativas. Espera 1 minuto.",
                            "retryAfter", rateLimiter.retryAfterSeconds(ip)
                    ));
        }
        Object pwd = body == null ? null : body.get("password");
        if (!auth.checkPassword(pwd == null ? null : pwd.toString())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_password"));
        }
        rateLimiter.onSuccess(ip);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "token", auth.issueToken(),
                "ttlMs", 7L * 24 * 3600 * 1000
        ));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> check(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        String token = auth.substring(7);
        return ResponseEntity.ok(Map.of("valid", this.auth.validate(token)));
    }
}
