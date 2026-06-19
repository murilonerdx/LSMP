package br.com.murilo.liberthia.admin.tester;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Serviço que consolida 3 features públicas:
 *   1. Changelog público (release notes)
 *   2. Roadmap público (com votação)
 *   3. Bug Hunter Leaderboard
 *
 * Todas as queries SEM autenticação ficam aqui. CRUD admin tem auth.
 */
@Service
public class PublicContentService {

    public interface ChangelogRepo extends JpaRepository<ChangelogEntry, Long> {
        List<ChangelogEntry> findAllByOrderByReleaseDateDesc();
    }

    public interface RoadmapRepo extends JpaRepository<RoadmapItem, Long> {
        List<RoadmapItem> findByCategory(RoadmapItem.Category category, Sort sort);
        List<RoadmapItem> findAllByOrderByPriorityDescVotesDesc();
    }

    public interface TesterRepo extends JpaRepository<ModTester, Long> {}
    public interface BugRepo extends JpaRepository<BugReport, Long> {
        List<BugReport> findByTesterMcName(String mcName);
        List<BugReport> findByStatus(BugReport.Status status);
    }
    public interface ModPackageRepo extends JpaRepository<ModPackage, Long> {}
    public interface TesterSuggestionRepo extends JpaRepository<TesterSuggestion, Long> {}

    private final ChangelogRepo changelogRepo;
    private final RoadmapRepo roadmapRepo;
    private final TesterRepo testerRepo;
    private final BugRepo bugRepo;
    private final ModPackageRepo packageRepo;
    private final TesterSuggestionRepo suggestionRepo;

    public PublicContentService(ChangelogRepo cr, RoadmapRepo rr, TesterRepo tr,
                                 BugRepo br, ModPackageRepo pr, TesterSuggestionRepo sr) {
        this.changelogRepo = cr;
        this.roadmapRepo = rr;
        this.testerRepo = tr;
        this.bugRepo = br;
        this.packageRepo = pr;
        this.suggestionRepo = sr;
    }

    // ====================================================================
    // CHANGELOG
    // ====================================================================

    public List<Map<String, Object>> listChangelog() {
        return changelogRepo.findAllByOrderByReleaseDateDesc().stream()
                .map(this::changelogDto).toList();
    }

    public Map<String, Object> getChangelog(Long id) {
        return changelogRepo.findById(id).map(this::changelogDto).orElse(null);
    }

    @Transactional
    public ChangelogEntry createChangelog(Map<String, Object> body, String createdBy) {
        ChangelogEntry e = new ChangelogEntry();
        applyChangelog(e, body);
        e.setCreatedBy(createdBy);
        return changelogRepo.save(e);
    }

    @Transactional
    public ChangelogEntry updateChangelog(Long id, Map<String, Object> body) {
        ChangelogEntry e = changelogRepo.findById(id).orElseThrow();
        applyChangelog(e, body);
        return changelogRepo.save(e);
    }

    @Transactional
    public void deleteChangelog(Long id) {
        changelogRepo.deleteById(id);
    }

    private void applyChangelog(ChangelogEntry e, Map<String, Object> body) {
        if (body.get("version") != null) e.setVersion(str(body.get("version")));
        if (body.get("title") != null) e.setTitle(str(body.get("title")));
        if (body.containsKey("summary")) e.setSummary(str(body.get("summary")));
        if (body.containsKey("itemsAdded")) e.setItemsAdded(str(body.get("itemsAdded")));
        if (body.containsKey("bugsFixed")) e.setBugsFixed(str(body.get("bugsFixed")));
        if (body.containsKey("buffs")) e.setBuffs(str(body.get("buffs")));
        if (body.containsKey("debuffs")) e.setDebuffs(str(body.get("debuffs")));
        if (body.containsKey("integrations")) e.setIntegrations(str(body.get("integrations")));
        if (body.containsKey("credits")) e.setCredits(str(body.get("credits")));
        if (body.containsKey("notes")) e.setNotes(str(body.get("notes")));
        // bugsJson e suggestionsJson aceitam:
        //   • string já serializada (caller já fez JSON.stringify)
        //   • List/Map (caller mandou estruturado — re-serializa pra string)
        // Isso permite o frontend mandar payload natural (entries[i].bugs = [...])
        // OU já pré-serializado como string.
        if (body.containsKey("bugsJson")) e.setBugsJson(jsonStr(body.get("bugsJson")));
        if (body.containsKey("suggestionsJson")) e.setSuggestionsJson(jsonStr(body.get("suggestionsJson")));
        // Conveniência: se o caller mandar entry.bugs ou entry.suggestions
        // (não-string), também aceita — re-serializa.
        if (body.containsKey("bugs")) e.setBugsJson(jsonStr(body.get("bugs")));
        if (body.containsKey("suggestions")) e.setSuggestionsJson(jsonStr(body.get("suggestions")));
        if (body.get("highlighted") != null) e.setHighlighted(Boolean.TRUE.equals(body.get("highlighted")));
        if (body.get("releaseDate") != null) {
            try { e.setReleaseDate(Instant.parse(str(body.get("releaseDate")))); }
            catch (Exception ignored) {}
        }
    }

    /**
     * Aceita string já-serializada ou objeto/lista, retornando string JSON.
     * Pra strings, valida que é JSON parseable — se não for, retorna como-está
     * (compat). Null vira null (limpa o campo no DB).
     */
    private String jsonStr(Object value) {
        if (value == null) return null;
        if (value instanceof String s) {
            if (s.isBlank()) return null;
            return s;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Auto-gera um DRAFT da próxima changelog entry com:
     *   • bugs CONFIRMED com triagedAt >= corte
     *   • suggestions APPROVED/IMPLEMENTED com statusChangedAt >= corte
     *
     * O CORTE é o mais recente de:
     *   1. releaseDate da última changelog entry publicada
     *   2. uploadedAt do mod package mais recente
     *   3. Se nenhum dos 2 existir, retorna TUDO (sem cutoff)
     *
     * Isso impede que bugs já listados em changelog anterior reapareçam — só
     * pega o que aconteceu DESDE a última release.
     *
     * Retorna estrutura pronta pra POST /api/admin/changelog:
     *   { version: "?", title: "?", bugs: [...], suggestions: [...],
     *     bugsFixed: "...", credits: "...", releaseDate: now }
     */
    public Map<String, Object> autoGenerateNextChangelog() {
        // Determina cutoff: mais recente entre última changelog e último package
        Instant cutoff = null;
        var lastChangelog = changelogRepo.findAllByOrderByReleaseDateDesc().stream().findFirst();
        if (lastChangelog.isPresent()) cutoff = lastChangelog.get().getReleaseDate();
        var allPkgs = packageRepo.findAll();
        for (ModPackage p : allPkgs) {
            if (p.getUploadedAt() == null) continue;
            if (cutoff == null || p.getUploadedAt().isAfter(cutoff)) {
                cutoff = p.getUploadedAt();
            }
        }
        // Se nada existe, cutoff fica null e tudo entra (primeira release)

        final Instant cutoffFinal = cutoff;

        // Bugs CONFIRMED após cutoff (inclui ainda não-triados? não — só os
        // que foram triados em status final positivo).
        List<BugReport> confirmedBugs = bugRepo.findByStatus(BugReport.Status.CONFIRMED).stream()
                .filter(b -> b.getTriagedAt() != null)
                .filter(b -> cutoffFinal == null || b.getTriagedAt().isAfter(cutoffFinal))
                .sorted(Comparator.comparing(BugReport::getTriagedAt))
                .toList();

        // Suggestions APPROVED ou IMPLEMENTED após cutoff
        List<TesterSuggestion> sugs = suggestionRepo.findAll().stream()
                .filter(s -> s.getStatus() == TesterSuggestion.Status.APPROVED
                        || s.getStatus() == TesterSuggestion.Status.IMPLEMENTED)
                .filter(s -> {
                    Instant when = s.getStatusChangedAt();
                    if (when == null) when = s.getCreatedAt();
                    return when != null && (cutoffFinal == null || when.isAfter(cutoffFinal));
                })
                .sorted(Comparator.comparing((TesterSuggestion s) ->
                        s.getStatusChangedAt() != null ? s.getStatusChangedAt() : s.getCreatedAt()))
                .toList();

        // Constrói arrays estruturados
        List<Map<String, Object>> bugsArr = new ArrayList<>();
        Map<String, int[]> creditAgg = new LinkedHashMap<>();  // mcName → [count, totalPts]
        for (BugReport b : confirmedBugs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("title", b.getTitle());
            m.put("severity", b.getSeverity());
            m.put("priority", b.getPriority());
            m.put("reporterMcName", b.getTesterMcName());
            m.put("pointsAwarded", b.getPointsAwarded());
            m.put("itemId", b.getItemId());
            m.put("description", b.getDescription());
            m.put("screenshotUrl", b.getScreenshotUrl());
            m.put("status", "fixed");
            m.put("triagedAt", b.getTriagedAt() != null ? b.getTriagedAt().toString() : null);
            m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
            bugsArr.add(m);

            String name = b.getTesterMcName() != null ? b.getTesterMcName() : "anônimo";
            int[] agg = creditAgg.computeIfAbsent(name, k -> new int[2]);
            agg[0]++;
            agg[1] += b.getPointsAwarded();
        }

        List<Map<String, Object>> sugsArr = new ArrayList<>();
        for (TesterSuggestion s : sugs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("title", s.getTitle());
            m.put("type", s.getType() != null ? s.getType().name() : null);
            m.put("authorMcName", s.getAuthorMcName());
            // Score = upvotes - downvotes (TesterSuggestion não tem campo cached)
            m.put("score", s.getUpvotes() - s.getDownvotes());
            m.put("upvotes", s.getUpvotes());
            m.put("downvotes", s.getDownvotes());
            m.put("description", s.getDescription());
            m.put("suggestedItemId", s.getSuggestedItemId());
            m.put("iconUrl", s.getIconUrl());
            m.put("status", s.getStatus().name());
            m.put("adminNote", s.getAdminNote());
            m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            m.put("statusChangedAt", s.getStatusChangedAt() != null ? s.getStatusChangedAt().toString() : null);
            sugsArr.add(m);
        }

        // Strings legadas pra preencher os campos bugsFixed/credits
        StringBuilder bugsFixedSb = new StringBuilder();
        for (BugReport b : confirmedBugs) {
            if (bugsFixedSb.length() > 0) bugsFixedSb.append(" • ");
            bugsFixedSb.append("#").append(b.getId()).append(" ").append(b.getTitle());
            if (b.getTesterMcName() != null) bugsFixedSb.append(" (por ").append(b.getTesterMcName());
            if (b.getPointsAwarded() > 0) bugsFixedSb.append(" +").append(b.getPointsAwarded()).append("pts");
            if (b.getTesterMcName() != null) bugsFixedSb.append(")");
        }
        StringBuilder creditsSb = new StringBuilder();
        for (var entry : creditAgg.entrySet()) {
            if (creditsSb.length() > 0) creditsSb.append(" | ");
            creditsSb.append(entry.getKey())
                    .append(" (").append(entry.getValue()[0]).append(" bugs, ")
                    .append(entry.getValue()[1]).append("pts)");
        }

        // Estrutura final do draft
        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("version", "");  // admin preenche
        draft.put("title", "");
        draft.put("summary", "");
        draft.put("releaseDate", Instant.now().toString());
        draft.put("bugs", bugsArr);
        draft.put("suggestions", sugsArr);
        draft.put("bugsFixed", bugsFixedSb.toString());
        draft.put("credits", creditsSb.toString());
        draft.put("itemsAdded", "");
        draft.put("buffs", "");
        draft.put("debuffs", "");
        draft.put("integrations", "");
        draft.put("notes", "");
        draft.put("highlighted", false);
        // Meta-info pro UI mostrar "gerado desde X"
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("cutoffAt", cutoffFinal != null ? cutoffFinal.toString() : null);
        meta.put("cutoffSource", lastChangelog.isPresent() && (lastChangelog.get().getReleaseDate().equals(cutoffFinal))
                ? "last_changelog" : "last_package");
        meta.put("bugsCount", bugsArr.size());
        meta.put("suggestionsCount", sugsArr.size());
        draft.put("_meta", meta);
        return draft;
    }

    private Map<String, Object> changelogDto(ChangelogEntry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("version", e.getVersion());
        m.put("title", e.getTitle());
        m.put("summary", e.getSummary());
        m.put("itemsAdded", e.getItemsAdded());
        m.put("bugsFixed", e.getBugsFixed());
        m.put("buffs", e.getBuffs());
        m.put("debuffs", e.getDebuffs());
        m.put("integrations", e.getIntegrations());
        m.put("credits", e.getCredits());
        m.put("notes", e.getNotes());
        // bugsJson e suggestionsJson são retornados como STRINGS — frontend
        // faz JSON.parse(). Antes essa info vivia só dentro do `credits` como
        // texto puro; agora o modal de detalhe pode renderizar cards com
        // reporter/data/screenshot/severidade pra cada bug.
        m.put("bugsJson", e.getBugsJson());
        m.put("suggestionsJson", e.getSuggestionsJson());
        m.put("highlighted", e.isHighlighted());
        m.put("releaseDate", e.getReleaseDate() != null ? e.getReleaseDate().toString() : null);
        m.put("createdBy", e.getCreatedBy());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        m.put("updatedAt", e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        return m;
    }

    // ====================================================================
    // ROADMAP
    // ====================================================================

    public Map<String, List<Map<String, Object>>> listRoadmapGrouped() {
        // Agrupa por categoria, ordenado por priority desc + votes desc dentro de cada grupo.
        List<RoadmapItem> all = roadmapRepo.findAllByOrderByPriorityDescVotesDesc();
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (RoadmapItem.Category cat : RoadmapItem.Category.values()) {
            grouped.put(cat.name(), new ArrayList<>());
        }
        for (RoadmapItem it : all) {
            grouped.get(it.getCategory().name()).add(roadmapDto(it));
        }
        return grouped;
    }

    public List<Map<String, Object>> listRoadmap() {
        return roadmapRepo.findAllByOrderByPriorityDescVotesDesc().stream()
                .map(this::roadmapDto).toList();
    }

    @Transactional
    public RoadmapItem createRoadmap(Map<String, Object> body, String createdBy) {
        RoadmapItem it = new RoadmapItem();
        applyRoadmap(it, body);
        it.setCreatedBy(createdBy);
        return roadmapRepo.save(it);
    }

    @Transactional
    public RoadmapItem updateRoadmap(Long id, Map<String, Object> body) {
        RoadmapItem it = roadmapRepo.findById(id).orElseThrow();
        applyRoadmap(it, body);
        return roadmapRepo.save(it);
    }

    @Transactional
    public void deleteRoadmap(Long id) {
        roadmapRepo.deleteById(id);
    }

    /**
     * Voto público (1 click = +1). Anti-abuso via voterKey é client-side
     * (localStorage). Servidor só soma um contador.
     * Retorna o item atualizado.
     */
    @Transactional
    public RoadmapItem vote(Long id) {
        RoadmapItem it = roadmapRepo.findById(id).orElseThrow();
        it.setVotes(it.getVotes() + 1);
        return roadmapRepo.save(it);
    }

    /** Voto reverso (caso o tester queira desfazer). */
    @Transactional
    public RoadmapItem unvote(Long id) {
        RoadmapItem it = roadmapRepo.findById(id).orElseThrow();
        it.setVotes(Math.max(0, it.getVotes() - 1));
        return roadmapRepo.save(it);
    }

    private void applyRoadmap(RoadmapItem it, Map<String, Object> body) {
        if (body.get("title") != null) it.setTitle(str(body.get("title")));
        if (body.containsKey("description")) it.setDescription(str(body.get("description")));
        if (body.get("category") != null) {
            try { it.setCategory(RoadmapItem.Category.valueOf(str(body.get("category")).toUpperCase())); }
            catch (Exception ignored) {}
        }
        if (body.containsKey("emoji")) it.setEmoji(str(body.get("emoji")));
        if (body.containsKey("tag")) it.setTag(str(body.get("tag")));
        if (body.get("priority") != null) it.setPriority(intOrZero(body.get("priority")));
        if (body.containsKey("targetVersion")) it.setTargetVersion(str(body.get("targetVersion")));
        if (body.get("votes") != null) it.setVotes(intOrZero(body.get("votes")));
    }

    private Map<String, Object> roadmapDto(RoadmapItem it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", it.getId());
        m.put("title", it.getTitle());
        m.put("description", it.getDescription());
        m.put("category", it.getCategory().name());
        m.put("emoji", it.getEmoji());
        m.put("tag", it.getTag());
        m.put("votes", it.getVotes());
        m.put("priority", it.getPriority());
        m.put("targetVersion", it.getTargetVersion());
        m.put("createdBy", it.getCreatedBy());
        m.put("createdAt", it.getCreatedAt() != null ? it.getCreatedAt().toString() : null);
        return m;
    }

    // ====================================================================
    // BUG HUNTER LEADERBOARD
    // ====================================================================

    /**
     * Ranking público de caçadores de bugs.
     *
     * Tiers (baseados em bugs CONFIRMADOS):
     *   • Iniciante (1-2)
     *   • Bronze (3-5)
     *   • Silver (6-10)
     *   • Gold (11-20)
     *   • Diamond (21-49)
     *   • Lendário (50+)
     *
     * Pontos vêm do tester.points (admin define quanto por bug ao confirmar).
     */
    public Map<String, Object> bugLeaderboard() {
        // Agrega contagem de bugs confirmados por tester
        Map<String, Integer> confirmedByTester = new HashMap<>();
        for (BugReport b : bugRepo.findByStatus(BugReport.Status.CONFIRMED)) {
            confirmedByTester.merge(b.getTesterMcName(), 1, Integer::sum);
        }

        // Carrega todos os testers ativos e cruza com bug count
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (ModTester t : testerRepo.findAll()) {
            if (!t.isEnabled()) continue;
            int confirmed = confirmedByTester.getOrDefault(t.getMcName(), 0);
            if (confirmed == 0 && t.getPoints() == 0) continue; // só lista quem contribuiu

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mcName", t.getMcName());
            row.put("points", t.getPoints());
            row.put("bugsConfirmed", confirmed);
            row.put("tier", computeTier(confirmed));
            row.put("memberSince", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
            ranking.add(row);
        }

        // Ordena por pontos desc, bugs desc, mcName asc
        ranking.sort((a, b) -> {
            int pa = (int) a.get("points"); int pb = (int) b.get("points");
            if (pb != pa) return Integer.compare(pb, pa);
            int ba = (int) a.get("bugsConfirmed"); int bb = (int) b.get("bugsConfirmed");
            if (bb != ba) return Integer.compare(bb, ba);
            return ((String) a.get("mcName")).compareToIgnoreCase((String) b.get("mcName"));
        });

        // Adiciona posição (rank)
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ranking", ranking);
        out.put("count", ranking.size());
        out.put("totalBugsConfirmed", bugRepo.findByStatus(BugReport.Status.CONFIRMED).size());
        out.put("totalTesters", ranking.size());
        out.put("tiers", List.of(
                Map.of("name", "Lendário", "minBugs", 50, "emoji", "👑", "color", "#fbbf24"),
                Map.of("name", "Diamond", "minBugs", 21, "emoji", "💎", "color", "#60a5fa"),
                Map.of("name", "Gold", "minBugs", 11, "emoji", "🥇", "color", "#fcd34d"),
                Map.of("name", "Silver", "minBugs", 6, "emoji", "🥈", "color", "#cbd5e1"),
                Map.of("name", "Bronze", "minBugs", 3, "emoji", "🥉", "color", "#fb923c"),
                Map.of("name", "Iniciante", "minBugs", 1, "emoji", "🌱", "color", "#86efac")
        ));
        return out;
    }

    private String computeTier(int bugs) {
        if (bugs >= 50) return "Lendário";
        if (bugs >= 21) return "Diamond";
        if (bugs >= 11) return "Gold";
        if (bugs >= 6) return "Silver";
        if (bugs >= 3) return "Bronze";
        if (bugs >= 1) return "Iniciante";
        return "—";
    }

    // ====================================================================
    // helpers
    // ====================================================================

    private static String str(Object o) { return o == null ? null : o.toString(); }
    private static int intOrZero(Object o) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
}
