package br.com.murilo.liberthia.admin.tester;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

/**
 * Serviço unificado das extensões de tester:
 *   - SplashSuggestion (mensagens de loading screen)
 *   - BalanceRequest (buff/nerf)
 *   - FeatureWikiEntry (docs por feature)
 *   - TesterNotification (inbox in-app)
 *
 * Mantém os repos como interfaces internas (mesmo padrão de
 * {@link TesterContentService} pra evitar arquivos minúsculos espalhados).
 */
@Service
public class TesterExtensionsService {

    private static final ObjectMapper JSON = new ObjectMapper();

    public interface SplashRepo extends JpaRepository<SplashSuggestion, Long> {
        List<SplashSuggestion> findByStatusOrderByUpvotesDescCreatedAtDesc(SplashSuggestion.Status status);
        List<SplashSuggestion> findAllByOrderByCreatedAtDesc();
        List<SplashSuggestion> findByAuthorMcNameIgnoreCaseOrderByCreatedAtDesc(String mc);
    }

    public interface BalanceRepo extends JpaRepository<BalanceRequest, Long> {
        List<BalanceRequest> findByStatusOrderByCreatedAtDesc(BalanceRequest.Status status);
        List<BalanceRequest> findAllByOrderByCreatedAtDesc();
        List<BalanceRequest> findByTesterMcNameIgnoreCaseOrderByCreatedAtDesc(String mc);
        List<BalanceRequest> findByItemIdOrderByCreatedAtDesc(String itemId);
    }

    public interface WikiRepo extends JpaRepository<FeatureWikiEntry, Long> {
        List<FeatureWikiEntry> findByPublishedTrueOrderByUpdatedAtDescCreatedAtDesc();
        List<FeatureWikiEntry> findAllByOrderByUpdatedAtDescCreatedAtDesc();
        Optional<FeatureWikiEntry> findBySlug(String slug);
        List<FeatureWikiEntry> findByCategoryAndPublishedTrueOrderByTitleAsc(FeatureWikiEntry.Category cat);
    }

    public interface NotificationRepo extends JpaRepository<TesterNotification, Long> {
        List<TesterNotification> findTop100ByRecipientMcNameIgnoreCaseOrderByCreatedAtDesc(String mc);
        long countByRecipientMcNameIgnoreCaseAndIsReadFalse(String mc);
        List<TesterNotification> findByRecipientMcNameIgnoreCaseAndIsReadFalseOrderByCreatedAtDesc(String mc);
    }

    private final SplashRepo splashRepo;
    private final BalanceRepo balanceRepo;
    private final WikiRepo wikiRepo;
    private final NotificationRepo notifRepo;

    public TesterExtensionsService(SplashRepo splashRepo, BalanceRepo balanceRepo,
                                   WikiRepo wikiRepo, NotificationRepo notifRepo) {
        this.splashRepo = splashRepo;
        this.balanceRepo = balanceRepo;
        this.wikiRepo = wikiRepo;
        this.notifRepo = notifRepo;
    }

    // ============================================================
    // SPLASH SUGGESTIONS
    // ============================================================

    @Transactional
    public SplashSuggestion createSplash(String mc, String text, String colorHex,
                                          SplashSuggestion.Category cat) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("texto vazio");
        if (text.length() > 200) throw new IllegalArgumentException("máximo 200 caracteres");
        SplashSuggestion s = new SplashSuggestion();
        s.setAuthorMcName(mc);
        s.setText(text.trim());
        s.setColorHex(colorHex);
        s.setCategory(cat != null ? cat : SplashSuggestion.Category.FUNNY);
        return splashRepo.save(s);
    }

    public List<SplashSuggestion> listSplashes(SplashSuggestion.Status filter) {
        if (filter != null) return splashRepo.findByStatusOrderByUpvotesDescCreatedAtDesc(filter);
        return splashRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public SplashSuggestion voteSplash(Long id, String mcName, int vote) {
        SplashSuggestion s = splashRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("splash não existe"));
        Map<String, Integer> votes = parseVotes(s.getVotesJson());
        Integer prev = votes.getOrDefault(mcName, 0);
        if (vote == 0) votes.remove(mcName); else votes.put(mcName, vote > 0 ? 1 : -1);
        s.setVotesJson(stringifyVotes(votes));
        int up = 0, down = 0;
        for (int v : votes.values()) { if (v > 0) up++; else if (v < 0) down++; }
        s.setUpvotes(up); s.setDownvotes(down);
        return splashRepo.save(s);
    }

    @Transactional
    public SplashSuggestion triageSplash(Long id, SplashSuggestion.Status newStatus,
                                          String adminNote, String triagedBy) {
        SplashSuggestion s = splashRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("splash não existe"));
        SplashSuggestion.Status old = s.getStatus();
        s.setStatus(newStatus);
        s.setAdminNote(adminNote);
        s.setTriagedBy(triagedBy);
        s.setTriagedAt(Instant.now());
        SplashSuggestion saved = splashRepo.save(s);

        // Notifica autor se mudou status pra algo definitivo
        if (old != newStatus && s.getAuthorMcName() != null) {
            TesterNotification.Type t = switch (newStatus) {
                case APPROVED -> TesterNotification.Type.SPLASH_APPROVED;
                case REJECTED -> TesterNotification.Type.SPLASH_REJECTED;
                default -> null;
            };
            if (t != null) {
                notify(s.getAuthorMcName(), t,
                        newStatus == SplashSuggestion.Status.APPROVED
                                ? "✨ Seu splash foi aprovado!"
                                : "❌ Splash não aprovado",
                        s.getText(),
                        "/tester/dashboard?tab=splashes&id=" + s.getId());
            }
        }
        return saved;
    }

    @Transactional
    public void deleteSplash(Long id) { splashRepo.deleteById(id); }

    /** Lista approved splashes pesados por weight — usado pelo mod (endpoint público). */
    public List<SplashSuggestion> activeSplashes() {
        return splashRepo.findByStatusOrderByUpvotesDescCreatedAtDesc(SplashSuggestion.Status.APPROVED);
    }

    // ============================================================
    // BALANCE REQUESTS
    // ============================================================

    @Transactional
    public BalanceRequest createBalance(String mc, BalanceRequest req) {
        if (req.getItemId() == null || req.getItemId().isBlank())
            throw new IllegalArgumentException("itemId obrigatório");
        if (req.getTitle() == null || req.getTitle().isBlank())
            throw new IllegalArgumentException("título obrigatório");
        if (req.getDescription() == null || req.getDescription().isBlank())
            throw new IllegalArgumentException("descrição obrigatória");
        req.setTesterMcName(mc);
        return balanceRepo.save(req);
    }

    public List<BalanceRequest> listBalances(BalanceRequest.Status filter, String mcName) {
        if (mcName != null && !mcName.isBlank())
            return balanceRepo.findByTesterMcNameIgnoreCaseOrderByCreatedAtDesc(mcName);
        if (filter != null) return balanceRepo.findByStatusOrderByCreatedAtDesc(filter);
        return balanceRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public BalanceRequest triageBalance(Long id, BalanceRequest.Status newStatus,
                                         String adminNote, String triagedBy,
                                         Integer awardPoints) {
        BalanceRequest b = balanceRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("request não existe"));
        BalanceRequest.Status old = b.getStatus();
        b.setStatus(newStatus);
        b.setAdminNote(adminNote);
        b.setTriagedBy(triagedBy);
        b.setTriagedAt(Instant.now());
        if (awardPoints != null && awardPoints > 0) {
            b.setPointsAwarded(b.getPointsAwarded() + awardPoints);
        }
        BalanceRequest saved = balanceRepo.save(b);

        if (old != newStatus && b.getTesterMcName() != null) {
            TesterNotification.Type t = switch (newStatus) {
                case APPROVED -> TesterNotification.Type.BALANCE_APPROVED;
                case REJECTED -> TesterNotification.Type.BALANCE_REJECTED;
                case IMPLEMENTED -> TesterNotification.Type.BALANCE_IMPLEMENTED;
                default -> null;
            };
            if (t != null) {
                String title = switch (newStatus) {
                    case APPROVED -> "⚖ Pedido de ajuste APROVADO";
                    case REJECTED -> "❌ Pedido de ajuste recusado";
                    case IMPLEMENTED -> "🎉 Ajuste IMPLEMENTADO";
                    default -> "Status do pedido alterado";
                };
                notify(b.getTesterMcName(), t, title, b.getTitle(),
                        "/tester/dashboard?tab=balance&id=" + b.getId());
            }
        }
        return saved;
    }

    @Transactional
    public void deleteBalance(Long id) { balanceRepo.deleteById(id); }

    // ============================================================
    // WIKI ENTRIES
    // ============================================================

    @Transactional
    public FeatureWikiEntry saveWiki(FeatureWikiEntry entry, String authorAdmin) {
        if (entry.getSlug() == null || entry.getSlug().isBlank())
            throw new IllegalArgumentException("slug obrigatório");
        if (entry.getTitle() == null || entry.getTitle().isBlank())
            throw new IllegalArgumentException("título obrigatório");
        if (entry.getContentMd() == null || entry.getContentMd().isBlank())
            throw new IllegalArgumentException("conteúdo obrigatório");
        entry.setSlug(slugify(entry.getSlug()));
        if (entry.getAuthorAdmin() == null) entry.setAuthorAdmin(authorAdmin);
        FeatureWikiEntry saved = wikiRepo.save(entry);

        // Notifica os créditos (1ª vez que entrada aparece)
        if (saved.isPublished() && saved.getCreditsJson() != null) {
            try {
                List<String> credits = JSON.readValue(saved.getCreditsJson(), new TypeReference<>() {});
                for (String mc : credits) {
                    if (mc != null && !mc.isBlank()) {
                        notify(mc, TesterNotification.Type.WIKI_CREDIT,
                                "📚 Você foi creditado numa wiki!",
                                saved.getTitle(),
                                "/wiki/" + saved.getSlug());
                    }
                }
            } catch (Exception ignored) {}
        }
        return saved;
    }

    public List<FeatureWikiEntry> listWiki(boolean publishedOnly, FeatureWikiEntry.Category cat) {
        if (cat != null && publishedOnly) return wikiRepo.findByCategoryAndPublishedTrueOrderByTitleAsc(cat);
        if (publishedOnly) return wikiRepo.findByPublishedTrueOrderByUpdatedAtDescCreatedAtDesc();
        return wikiRepo.findAllByOrderByUpdatedAtDescCreatedAtDesc();
    }

    public Optional<FeatureWikiEntry> getWikiBySlug(String slug) {
        return wikiRepo.findBySlug(slug);
    }

    @Transactional
    public void deleteWiki(Long id) { wikiRepo.deleteById(id); }

    private static String slugify(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    // ============================================================
    // NOTIFICATIONS
    // ============================================================

    @Transactional
    public TesterNotification notify(String recipient, TesterNotification.Type type,
                                      String title, String body, String link) {
        TesterNotification n = new TesterNotification();
        n.setRecipientMcName(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        return notifRepo.save(n);
    }

    public List<TesterNotification> listNotifications(String mcName) {
        return notifRepo.findTop100ByRecipientMcNameIgnoreCaseOrderByCreatedAtDesc(mcName);
    }

    public long unreadCount(String mcName) {
        return notifRepo.countByRecipientMcNameIgnoreCaseAndIsReadFalse(mcName);
    }

    @Transactional
    public void markRead(Long id, String mcName) {
        notifRepo.findById(id).ifPresent(n -> {
            if (!n.getRecipientMcName().equalsIgnoreCase(mcName)) return; // só dono lê
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(Instant.now());
                notifRepo.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(String mcName) {
        List<TesterNotification> unread = notifRepo
                .findByRecipientMcNameIgnoreCaseAndIsReadFalseOrderByCreatedAtDesc(mcName);
        Instant now = Instant.now();
        for (TesterNotification n : unread) {
            n.setRead(true);
            n.setReadAt(now);
        }
        notifRepo.saveAll(unread);
    }

    @Transactional
    public void deleteNotification(Long id, String mcName) {
        notifRepo.findById(id).ifPresent(n -> {
            if (n.getRecipientMcName().equalsIgnoreCase(mcName)) notifRepo.deleteById(id);
        });
    }

    // ============================================================
    // HELPERS DE VOTOS (compartilhado entre splash/suggestion)
    // ============================================================

    public static Map<String, Integer> parseVotes(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try { return JSON.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return new HashMap<>(); }
    }

    public static String stringifyVotes(Map<String, Integer> map) {
        try { return JSON.writeValueAsString(map); }
        catch (Exception e) { return "{}"; }
    }

    private static String randomToken(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
}
