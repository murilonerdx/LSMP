package br.com.murilo.liberthia.admin.tester;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Lógica de inscrições (TesterApplication), configuração do servidor de teste
 * (TestServerInfo singleton) e sugestões de items (TesterSuggestion + votos).
 */
@Service
public class TesterApplyService {

    private static final Logger LOG = LoggerFactory.getLogger(TesterApplyService.class);

    public interface AppRepo extends JpaRepository<TesterApplication, Long> {
        List<TesterApplication> findByStatusOrderByCreatedAtAsc(TesterApplication.Status status);
        List<TesterApplication> findAllByOrderByCreatedAtDesc();
        Optional<TesterApplication> findByMcNameIgnoreCase(String mcName);
    }

    public interface ServerInfoRepo extends JpaRepository<TestServerInfo, Long> {}

    public interface SuggestionRepo extends JpaRepository<TesterSuggestion, Long> {
        List<TesterSuggestion> findAllByOrderByUpvotesDescCreatedAtDesc();
        List<TesterSuggestion> findByStatusOrderByUpvotesDesc(TesterSuggestion.Status status);
        List<TesterSuggestion> findByAuthorMcNameIgnoreCaseOrderByCreatedAtDesc(String mcName);
    }

    private final AppRepo appRepo;
    private final ServerInfoRepo serverRepo;
    private final SuggestionRepo sugRepo;
    private final ModTesterService testerSvc;
    private final org.springframework.beans.factory.ObjectProvider<TesterExtensionsService> extProvider;
    private final ObjectMapper mapper = new ObjectMapper();

    public TesterApplyService(AppRepo appRepo, ServerInfoRepo serverRepo,
                              SuggestionRepo sugRepo, ModTesterService testerSvc,
                              org.springframework.beans.factory.ObjectProvider<TesterExtensionsService> extProvider) {
        this.appRepo = appRepo;
        this.serverRepo = serverRepo;
        this.sugRepo = sugRepo;
        this.testerSvc = testerSvc;
        this.extProvider = extProvider;
    }

    // ============================================================ //
    // APPLICATIONS
    // ============================================================ //

    @Transactional
    public TesterApplication apply(TesterApplication in) {
        if (in.getMcName() == null || in.getMcName().isBlank())
            throw new IllegalArgumentException("Nick MC obrigatório");
        if (in.getRealName() == null || in.getRealName().isBlank())
            throw new IllegalArgumentException("Nome obrigatório");
        if (in.getMotivation() == null || in.getMotivation().isBlank())
            throw new IllegalArgumentException("Motivação obrigatória");

        // Não permite reaplicar se já tem uma pendente ou aprovada
        Optional<TesterApplication> existing = appRepo.findByMcNameIgnoreCase(in.getMcName());
        if (existing.isPresent()) {
            TesterApplication ex = existing.get();
            if (ex.getStatus() == TesterApplication.Status.PENDING)
                throw new IllegalStateException("Você já tem uma inscrição pendente — aguarde aprovação");
            if (ex.getStatus() == TesterApplication.Status.APPROVED)
                throw new IllegalStateException("Você já foi aprovado — use seu código pra criar a conta");
        }

        in.setStatus(TesterApplication.Status.PENDING);
        return appRepo.save(in);
    }

    public List<TesterApplication> listApplications(TesterApplication.Status status) {
        if (status == null) return appRepo.findAllByOrderByCreatedAtDesc();
        return appRepo.findByStatusOrderByCreatedAtAsc(status);
    }

    public Optional<TesterApplication> getApplication(Long id) { return appRepo.findById(id); }

    /**
     * Aprova a inscrição: gera InviteCode auto-vinculado, retorna app atualizado.
     * Admin copia o código gerado e manda pro candidato.
     */
    @Transactional
    public TesterApplication approve(Long id, String adminNote, String reviewedBy) {
        TesterApplication a = appRepo.findById(id).orElseThrow();
        if (a.getStatus() == TesterApplication.Status.APPROVED) return a;
        a.setStatus(TesterApplication.Status.APPROVED);
        a.setAdminNote(adminNote);
        a.setReviewedBy(reviewedBy);
        a.setReviewedAt(Instant.now());

        // Gera invite code + vincula
        InviteCode inv = testerSvc.createInvite(reviewedBy != null ? reviewedBy : "admin",
                "Auto-gerado pra inscrição #" + id + " (" + a.getMcName() + ")");
        a.setGeneratedInviteCode(inv.getCode());
        return appRepo.save(a);
    }

    @Transactional
    public TesterApplication reject(Long id, String adminNote, String reviewedBy) {
        TesterApplication a = appRepo.findById(id).orElseThrow();
        a.setStatus(TesterApplication.Status.REJECTED);
        a.setAdminNote(adminNote);
        a.setReviewedBy(reviewedBy);
        a.setReviewedAt(Instant.now());
        return appRepo.save(a);
    }

    // ============================================================ //
    // SERVER INFO
    // ============================================================ //

    public TestServerInfo getServerInfo() {
        return serverRepo.findById(1L).orElseGet(() -> {
            TestServerInfo s = new TestServerInfo();
            s.setId(1L);
            s.setNdaText("AVISO: este servidor é exclusivo para mod testers aprovados. " +
                    "É TERMINANTEMENTE PROIBIDO compartilhar o endereço, gravações, " +
                    "screenshots de items beta ou qualquer conteúdo do servidor com " +
                    "terceiros sem autorização expressa do admin. Violação = banimento " +
                    "permanente da conta de tester + perda de pontos acumulados.");
            return serverRepo.save(s);
        });
    }

    @Transactional
    public TestServerInfo updateServerInfo(TestServerInfo in, String by) {
        TestServerInfo s = getServerInfo();
        if (in.getServerAddress() != null) s.setServerAddress(in.getServerAddress());
        if (in.getMcVersion() != null) s.setMcVersion(in.getMcVersion());
        if (in.getModVersion() != null) s.setModVersion(in.getModVersion());
        if (in.getConnectionNotes() != null) s.setConnectionNotes(in.getConnectionNotes());
        if (in.getNdaText() != null) s.setNdaText(in.getNdaText());
        if (in.getDiscordLink() != null) s.setDiscordLink(in.getDiscordLink());
        if (in.getVoiceChatInfo() != null) s.setVoiceChatInfo(in.getVoiceChatInfo());
        s.setActive(in.isActive());
        s.setUpdatedBy(by != null ? by : "admin");
        s.setUpdatedAt(Instant.now());
        return serverRepo.save(s);
    }

    // ============================================================ //
    // SUGGESTIONS + VOTES
    // ============================================================ //

    @Transactional
    public TesterSuggestion createSuggestion(TesterSuggestion in) {
        if (in.getTitle() == null || in.getTitle().isBlank())
            throw new IllegalArgumentException("Título obrigatório");
        if (in.getDescription() == null || in.getDescription().isBlank())
            throw new IllegalArgumentException("Descrição obrigatória");
        in.setStatus(TesterSuggestion.Status.PENDING);
        in.setUpvotes(0); in.setDownvotes(0);
        in.setVotesJson("{}");
        return sugRepo.save(in);
    }

    public List<TesterSuggestion> listSuggestions(TesterSuggestion.Status status) {
        if (status == null) return sugRepo.findAllByOrderByUpvotesDescCreatedAtDesc();
        return sugRepo.findByStatusOrderByUpvotesDesc(status);
    }

    public List<TesterSuggestion> listSuggestionsByAuthor(String mcName) {
        return sugRepo.findByAuthorMcNameIgnoreCaseOrderByCreatedAtDesc(mcName);
    }

    public Optional<TesterSuggestion> getSuggestion(Long id) { return sugRepo.findById(id); }

    /**
     * Aplica voto do tester: vote=+1 (up), -1 (down), 0 (remove).
     * Re-conta upvotes/downvotes ao final.
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public TesterSuggestion vote(Long suggestionId, String voterMcName, int vote) {
        TesterSuggestion s = sugRepo.findById(suggestionId).orElseThrow();
        Map<String, Integer> votes;
        try {
            String j = s.getVotesJson() == null || s.getVotesJson().isBlank() ? "{}" : s.getVotesJson();
            votes = (Map<String, Integer>) mapper.readValue(j, Map.class);
        } catch (Exception e) {
            votes = new HashMap<>();
        }
        if (vote == 0) {
            votes.remove(voterMcName);
        } else if (vote > 0) {
            votes.put(voterMcName, 1);
        } else {
            votes.put(voterMcName, -1);
        }
        int up = 0, down = 0;
        for (Integer v : votes.values()) {
            if (v != null && v > 0) up++;
            else if (v != null && v < 0) down++;
        }
        s.setUpvotes(up);
        s.setDownvotes(down);
        try { s.setVotesJson(mapper.writeValueAsString(votes)); }
        catch (Exception ignored) { s.setVotesJson("{}"); }
        return sugRepo.save(s);
    }

    /** Quanto o tester votou nessa sugestão (-1, 0 ou +1). */
    @SuppressWarnings("unchecked")
    public int getUserVote(TesterSuggestion s, String mcName) {
        try {
            if (s.getVotesJson() == null) return 0;
            Map<String, Integer> votes = (Map<String, Integer>) mapper.readValue(s.getVotesJson(), Map.class);
            Integer v = votes.get(mcName);
            return v == null ? 0 : v;
        } catch (Exception e) { return 0; }
    }

    @Transactional
    public TesterSuggestion adminUpdateStatus(Long id, TesterSuggestion.Status status,
                                              String adminNote, String triagedBy) {
        return adminUpdateStatus(id, status, adminNote, triagedBy, null);
    }

    /**
     * Overload com developmentNote pra workflow expandido (IN_DEVELOPMENT,
     * PAUSED). Notifica autor da sugestão se o status mudou.
     */
    @Transactional
    public TesterSuggestion adminUpdateStatus(Long id, TesterSuggestion.Status status,
                                              String adminNote, String triagedBy,
                                              String developmentNote) {
        TesterSuggestion s = sugRepo.findById(id).orElseThrow();
        TesterSuggestion.Status old = s.getStatus();
        s.setStatus(status);
        s.setAdminNote(adminNote);
        s.setTriagedBy(triagedBy);
        s.setTriagedAt(Instant.now());
        s.setStatusChangedAt(Instant.now());
        if (developmentNote != null) s.setDevelopmentNote(developmentNote);
        TesterSuggestion saved = sugRepo.save(s);

        // Notifica autor se status realmente mudou
        if (old != status && s.getAuthorMcName() != null && extProvider != null) {
            try {
                TesterExtensionsService ext = extProvider.getIfAvailable();
                if (ext != null) {
                    TesterNotification.Type t = switch (status) {
                        case APPROVED -> TesterNotification.Type.SUGGESTION_APPROVED;
                        case REJECTED -> TesterNotification.Type.SUGGESTION_REJECTED;
                        case IMPLEMENTED -> TesterNotification.Type.SUGGESTION_IMPLEMENTED;
                        default -> TesterNotification.Type.SUGGESTION_STATUS_CHANGED;
                    };
                    String emoji = switch (status) {
                        case APPROVED -> "✅";
                        case REJECTED -> "❌";
                        case IMPLEMENTED -> "🎉";
                        case IN_DEVELOPMENT -> "🛠️";
                        case PAUSED -> "⏸️";
                        case UNDER_REVIEW -> "👀";
                        default -> "📝";
                    };
                    ext.notify(s.getAuthorMcName(), t,
                            emoji + " Sugestão " + status.name(),
                            s.getTitle(),
                            "/tester/dashboard?tab=suggestions&id=" + s.getId());
                }
            } catch (Exception ignored) {}
        }
        return saved;
    }
}
