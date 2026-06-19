package br.com.murilo.liberthia.admin.tester;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * Lógica de auth + ranking dos mod testers.
 *
 * Token: HMAC-SHA256 sobre payload "{mcName}|{iat}|{exp}", retorna
 * base64url(payload) + "." + base64url(hmac). Validação confere assinatura
 * e expiração, e devolve o mcName extraído.
 *
 * Senha: SHA-256 com salt único por usuário. Formato persistido:
 * "{base64salt}${base64hash}".
 */
@Service
public class ModTesterService {

    public interface TesterRepo extends JpaRepository<ModTester, Long> {
        Optional<ModTester> findByMcNameIgnoreCase(String mcName);
        List<ModTester> findTop20ByOrderByPointsDesc();
    }

    public interface InviteRepo extends JpaRepository<InviteCode, Long> {
        Optional<InviteCode> findByCode(String code);
        List<InviteCode> findByUsedByIsNullOrderByCreatedAtDesc();
    }

    private final TesterRepo testers;
    private final InviteRepo invites;
    private final byte[] secret;
    private static final long TTL_MS = 30L * 24 * 3600 * 1000; // 30 dias

    public ModTesterService(TesterRepo testers, InviteRepo invites,
                            @Value("${admin.web.password:liberthia2026}") String adminPwd,
                            @Value("${admin.web.secret:}") String injectedSecret) {
        this.testers = testers;
        this.invites = invites;
        String s = injectedSecret == null || injectedSecret.isBlank()
                ? "liberthia-tester-secret-" + adminPwd : injectedSecret + "-tester";
        this.secret = sha256(s.getBytes(StandardCharsets.UTF_8));
    }

    // ============ AUTH ============

    @Transactional
    public ModTester register(String mcName, String inviteCode, String password) {
        if (mcName == null || mcName.isBlank()) throw new IllegalArgumentException("nome MC obrigatório");
        if (inviteCode == null || inviteCode.isBlank()) throw new IllegalArgumentException("código obrigatório");
        if (password == null || password.length() < 4) throw new IllegalArgumentException("senha mínimo 4 chars");

        // Verifica se o nome já está em uso
        if (testers.findByMcNameIgnoreCase(mcName).isPresent()) {
            throw new IllegalStateException("nome já registrado");
        }

        // Caso especial: PRE_PROVISIONED — código gerado pelo admin pra um
        // tester específico via /api/admin/tester/auto-create. Esse código
        // não tem mcName atrelado ainda; o tester escolhe o nome dele aqui.
        InviteCode inv = invites.findByCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("código inválido"));
        if (inv.isUsed()) {
            throw new IllegalStateException("código já usado por " + inv.getUsedBy());
        }

        // Marca o código como usado
        inv.setUsedBy(mcName);
        inv.setUsedAt(Instant.now());
        invites.save(inv);

        // Cria o tester
        ModTester t = new ModTester();
        t.setMcName(mcName.trim());
        t.setPasswordHash(hashPassword(password));
        t.setInviteCodeUsed(inv.getCode());
        return testers.save(t);
    }

    /**
     * Admin pré-provisiona uma conta de tester completa (mcName escolhido +
     * código de claim). O tester recebe um link e só precisa colocar a senha
     * pra ativar — não precisa de invite code separado nem escolher nome.
     *
     * Fluxo:
     *  1) Admin chama autoCreate("PlayerX") → cria ModTester com placeholder
     *     hash (disabled=false mas inacessível) + invite code linkado.
     *  2) Sistema retorna { mcName, claimCode, claimUrl }.
     *  3) Admin envia o link pro tester via Discord/DM.
     *  4) Tester abre o link → vê só campo de senha → POST claim → JWT issued.
     *
     * Retorna { tester, claimCode } pra o controller montar o link.
     */
    @Transactional
    public AutoCreateResult autoCreate(String mcName, String createdBy, String note) {
        if (mcName == null || mcName.isBlank()) throw new IllegalArgumentException("nome MC obrigatório");
        if (testers.findByMcNameIgnoreCase(mcName).isPresent()) {
            throw new IllegalStateException("nome já registrado");
        }

        // Cria o ModTester com flag de claim pendente (passwordHash placeholder).
        ModTester t = new ModTester();
        t.setMcName(mcName.trim());
        t.setPasswordHash("PENDING_CLAIM"); // sentinela inválida — login falha
        t.setEnabled(true);
        t = testers.save(t);

        // Cria invite code reservado pra essa conta. Note inclui mcName pra
        // o front mostrar "Conta de fulano" na tela de claim.
        InviteCode inv = new InviteCode();
        inv.setCode(generateCode());
        inv.setCreatedBy(createdBy != null ? createdBy : "admin");
        inv.setNote("CLAIM:" + t.getMcName() + (note != null && !note.isBlank() ? " | " + note : ""));
        invites.save(inv);

        return new AutoCreateResult(t, inv.getCode());
    }

    public record AutoCreateResult(ModTester tester, String claimCode) {}

    /**
     * Tester completa o claim de uma conta pré-provisionada — só define a
     * senha. Não precisa escolher mcName (já foi escolhido pelo admin).
     *
     * Valida: code existe, code tá no formato CLAIM:mcName, mcName confere
     * com a conta, conta tá em PENDING_CLAIM.
     */
    @Transactional
    public ModTester claimAccount(String claimCode, String password) {
        if (claimCode == null || claimCode.isBlank()) throw new IllegalArgumentException("código obrigatório");
        if (password == null || password.length() < 4) throw new IllegalArgumentException("senha mínimo 4 chars");

        InviteCode inv = invites.findByCode(claimCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("código inválido"));
        if (inv.isUsed()) throw new IllegalStateException("código já usado");
        if (inv.getNote() == null || !inv.getNote().startsWith("CLAIM:")) {
            throw new IllegalArgumentException("código não é de claim");
        }

        // Extrai mcName do note (formato "CLAIM:mcName | note")
        String mcRaw = inv.getNote().substring(6).trim();
        int sep = mcRaw.indexOf('|');
        String mcName = (sep > 0 ? mcRaw.substring(0, sep) : mcRaw).trim();

        ModTester t = testers.findByMcNameIgnoreCase(mcName)
                .orElseThrow(() -> new IllegalStateException("conta " + mcName + " não existe"));
        if (!"PENDING_CLAIM".equals(t.getPasswordHash())) {
            throw new IllegalStateException("conta já tem senha definida; use /login");
        }

        t.setPasswordHash(hashPassword(password));
        t.setLastLoginAt(Instant.now());

        inv.setUsedBy(t.getMcName());
        inv.setUsedAt(Instant.now());
        invites.save(inv);
        return testers.save(t);
    }

    public Optional<ModTester> login(String mcName, String password) {
        return loginWithReason(mcName, password).result;
    }

    /** Resultado do login com motivo da falha pra UX (Backend não-leak info sensível). */
    public LoginResult loginWithReason(String mcName, String password) {
        if (mcName == null || mcName.isBlank()) return LoginResult.empty();
        Optional<ModTester> opt = testers.findByMcNameIgnoreCase(mcName);
        if (opt.isEmpty()) return new LoginResult(Optional.empty(), "account_not_found");
        ModTester t = opt.get();
        if ("PENDING_CLAIM".equals(t.getPasswordHash())) {
            return new LoginResult(Optional.empty(), "pending_claim");
        }
        if (!t.isEnabled()) {
            // Extrai razão do ban (se foi salva via banTester)
            String reason = t.getInviteCodeUsed() != null && t.getInviteCodeUsed().startsWith("BANNED:")
                    ? t.getInviteCodeUsed().substring(7).trim() : null;
            return new LoginResult(Optional.empty(), "account_banned" + (reason != null ? ": " + reason : ""));
        }
        if (!verifyPassword(password, t.getPasswordHash())) {
            return new LoginResult(Optional.empty(), "wrong_password");
        }
        t.setLastLoginAt(Instant.now());
        testers.save(t);
        return new LoginResult(Optional.of(t), null);
    }

    public record LoginResult(Optional<ModTester> result, String failReason) {
        public static LoginResult empty() { return new LoginResult(Optional.empty(), "invalid_input"); }
    }

    public Optional<ModTester> findByMcName(String mcName) {
        return testers.findByMcNameIgnoreCase(mcName);
    }

    public List<ModTester> ranking() {
        return testers.findTop20ByOrderByPointsDesc();
    }

    @Transactional
    public ModTester addPoints(String mcName, int delta) {
        ModTester t = testers.findByMcNameIgnoreCase(mcName)
                .orElseThrow(() -> new IllegalArgumentException("tester não encontrado"));
        t.setPoints(t.getPoints() + delta);
        return testers.save(t);
    }

    // ============ INVITE CODES ============

    @Transactional
    public InviteCode createInvite(String createdBy, String note) {
        InviteCode inv = new InviteCode();
        inv.setCode(generateCode());
        inv.setCreatedBy(createdBy != null ? createdBy : "admin");
        inv.setNote(note);
        return invites.save(inv);
    }

    public List<InviteCode> listInvites(boolean onlyUnused) {
        if (onlyUnused) return invites.findByUsedByIsNullOrderByCreatedAtDesc();
        return invites.findAll();
    }

    /** Deleta um código de invite. Se o código já foi usado, marca a conta como
     *  inválida também (o tester linkado perde a ref mas continua existindo). */
    @Transactional
    public void deleteInvite(Long id) {
        invites.deleteById(id);
    }

    /** Ban: desabilita conta (não pode logar) sem deletar dados. Mantém histórico
     *  de bugs/sugestões/recompensas pra auditoria. */
    @Transactional
    public ModTester banTester(String mcName, String reason) {
        ModTester t = testers.findByMcNameIgnoreCase(mcName)
                .orElseThrow(() -> new IllegalArgumentException("tester não encontrado"));
        t.setEnabled(false);
        // Anota o motivo no campo `inviteCodeUsed` que é livre (não temos campo
        // dedicado pra ban reason). Prefixa pra distinguir.
        if (reason != null && !reason.isBlank()) {
            t.setInviteCodeUsed("BANNED: " + reason);
        }
        return testers.save(t);
    }

    /** Desbanir: reativa conta. */
    @Transactional
    public ModTester unbanTester(String mcName) {
        ModTester t = testers.findByMcNameIgnoreCase(mcName)
                .orElseThrow(() -> new IllegalArgumentException("tester não encontrado"));
        t.setEnabled(true);
        return testers.save(t);
    }

    /** Delete HARD: apaga a row do mod_testers (cascade: bug reports, sugestões
     *  e redemptions NÃO são deletados — ficam como histórico órfão pra auditoria).
     *  Use com cuidado — preferível banir. */
    @Transactional
    public void deleteTester(String mcName) {
        ModTester t = testers.findByMcNameIgnoreCase(mcName)
                .orElseThrow(() -> new IllegalArgumentException("tester não encontrado"));
        testers.deleteById(t.getId());
    }

    public List<ModTester> listAllTesters() {
        return testers.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.desc("lastLoginAt").nullsLast(),
                org.springframework.data.domain.Sort.Order.desc("createdAt")));
    }

    private String generateCode() {
        // 8 chars A-Z + 0-9 (sem 0/O/I/1 confusos)
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    // ============ TOKEN ============

    public String issueToken(String mcName) {
        long now = System.currentTimeMillis();
        String payload = mcName + "|" + now + "|" + (now + TTL_MS);
        String b64Payload = b64Url(payload.getBytes(StandardCharsets.UTF_8));
        String sig = b64Url(hmac(b64Payload.getBytes(StandardCharsets.UTF_8)));
        return b64Payload + "." + sig;
    }

    /** Valida o token. Retorna o mcName se OK, null se inválido/expirado. */
    public String validateToken(String token) {
        if (token == null) return null;
        int dot = token.indexOf('.');
        if (dot < 0) return null;
        String payloadB64 = token.substring(0, dot);
        String sigB64 = token.substring(dot + 1);
        try {
            byte[] expected = hmac(payloadB64.getBytes(StandardCharsets.UTF_8));
            byte[] given = Base64.getUrlDecoder().decode(sigB64);
            if (!MessageDigest.isEqual(expected, given)) return null;
            String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");
            if (parts.length != 3) return null;
            long exp = Long.parseLong(parts[2]);
            if (System.currentTimeMillis() >= exp) return null;
            return parts[0];
        } catch (Exception e) {
            return null;
        }
    }

    // ============ PASSWORD HASH ============

    private static String hashPassword(String pwd) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(pwd.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean verifyPassword(String pwd, String stored) {
        if (pwd == null || stored == null) return false;
        try {
            int sep = stored.indexOf('$');
            if (sep < 0) return false;
            byte[] salt = Base64.getDecoder().decode(stored.substring(0, sep));
            byte[] expected = Base64.getDecoder().decode(stored.substring(sep + 1));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] given = md.digest(pwd.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(expected, given);
        } catch (Exception e) {
            return false;
        }
    }

    // ============ HELPERS ============

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static byte[] sha256(byte[] data) {
        try { return MessageDigest.getInstance("SHA-256").digest(data); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String b64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
