package br.com.murilo.liberthia.admin.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Auth simples baseado em senha do application.yml. Login retorna um token
 * HMAC-assinado com (issuedAt|expiresAt) — sem necessidade de banco.
 *
 * Token format: base64url(payload).base64url(hmacSHA256(payload, secret))
 * Payload: {"iat": longMillis, "exp": longMillis}
 */
@Service
public class AuthService {

    private final String password;
    private final byte[] secret;
    private static final long TTL_MS = 7L * 24 * 3600 * 1000; // 7 dias

    public AuthService(@Value("${admin.web.password:liberthia2026}") String password,
                       @Value("${admin.web.secret:}") String injectedSecret) {
        this.password = password;
        // Se não tiver secret no yml, deriva do password (estável entre restarts).
        // Pra rotacionar tokens em prod, defina admin.web.secret manualmente.
        String s = injectedSecret == null || injectedSecret.isBlank()
                ? "liberthia-default-secret-" + password : injectedSecret;
        this.secret = sha256(s.getBytes(StandardCharsets.UTF_8));
    }

    public boolean checkPassword(String pwd) {
        if (pwd == null) return false;
        // Comparação tempo-constante
        byte[] a = pwd.getBytes(StandardCharsets.UTF_8);
        byte[] b = password.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    /** Gera token assinado válido por TTL_MS. */
    public String issueToken() {
        long now = System.currentTimeMillis();
        String payload = "{\"iat\":" + now + ",\"exp\":" + (now + TTL_MS) + "}";
        String b64Payload = b64Url(payload.getBytes(StandardCharsets.UTF_8));
        String sig = b64Url(hmac(b64Payload.getBytes(StandardCharsets.UTF_8)));
        return b64Payload + "." + sig;
    }

    /** Valida token. Retorna true se assinatura OK e não expirado. */
    public boolean validate(String token) {
        if (token == null) return false;
        int dot = token.indexOf('.');
        if (dot < 0) return false;
        String payloadB64 = token.substring(0, dot);
        String sigB64 = token.substring(dot + 1);
        try {
            byte[] expected = hmac(payloadB64.getBytes(StandardCharsets.UTF_8));
            byte[] given = Base64.getUrlDecoder().decode(sigB64);
            if (!MessageDigest.isEqual(expected, given)) return false;
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            // Parse simples sem Jackson — só extrai exp
            int idx = payload.indexOf("\"exp\":");
            if (idx < 0) return false;
            int end = payload.indexOf('}', idx);
            long exp = Long.parseLong(payload.substring(idx + 6, end).trim().replaceAll("[^0-9]", ""));
            return System.currentTimeMillis() < exp;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] sha256(byte[] data) {
        try { return MessageDigest.getInstance("SHA-256").digest(data); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String b64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
