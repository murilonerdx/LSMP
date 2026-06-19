package br.com.murilo.liberthia.admin.tester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bulk import endpoints — admin envia JSON gerado pela IA (via
 * {@code tools/generate-items-update.py}) com lista de items/rewards/changelog/roadmap,
 * e o backend faz <b>upsert</b> (cria novos, atualiza existentes por chave natural).
 *
 * <h3>Chaves naturais (pra detectar updates):</h3>
 * <ul>
 *   <li><b>Beta Items</b>: {@code itemId} (fallback: {@code name})</li>
 *   <li><b>Rewards</b>: {@code name}</li>
 *   <li><b>Changelog</b>: {@code title}</li>
 *   <li><b>Roadmap</b>: {@code title}</li>
 * </ul>
 *
 * <p>Items removidos do JSON <b>NÃO são deletados</b> do banco (preserva
 * histórico). Admin desativa manualmente via toggle se quiser esconder.
 *
 * <p>Endpoints retornam {@code {ok, created, updated, errors, errorMessages[]}}.
 */
@RestController
public class BulkImportController {

    private static final Logger LOG = LoggerFactory.getLogger(BulkImportController.class);

    private final TesterContentService testerSvc;
    private final PublicContentService publicSvc;
    private final TesterExtensionsService extSvc;

    public BulkImportController(TesterContentService testerSvc, PublicContentService publicSvc,
                                TesterExtensionsService extSvc) {
        this.testerSvc = testerSvc;
        this.publicSvc = publicSvc;
        this.extSvc = extSvc;
    }

    // ============================================================
    // BETA ITEMS
    // ============================================================
    @PostMapping("/api/admin/tester/beta-items/bulk-import")
    @Transactional
    public ResponseEntity<?> bulkImportBetaItems(@RequestBody Map<String, Object> payload) {
        return processBulk(payload, "items", (m, errors) -> {
            String itemId = strOrNull(m.get("itemId"));
            String name   = strOrNull(m.get("name"));
            if (name == null || name.isBlank()) {
                errors.add("entry sem 'name'"); return null;
            }
            List<BetaItem> existing = testerSvc.listBetaItems(false);
            BetaItem found = existing.stream()
                    .filter(it -> (itemId != null && itemId.equals(it.getItemId()))
                                || (itemId == null && name.equalsIgnoreCase(it.getName())))
                    .findFirst().orElse(null);
            BetaItem target = (found != null) ? found : new BetaItem();
            target.setName(name);
            if (itemId != null) target.setItemId(itemId);
            if (m.get("kind") != null) {
                try { target.setKind(BetaItem.Kind.valueOf(strOrNull(m.get("kind")).toUpperCase())); }
                catch (Exception ignored) { if (found == null) target.setKind(BetaItem.Kind.ITEM); }
            }
            if (m.containsKey("category"))       target.setCategory(strOrNull(m.get("category")));
            if (m.containsKey("description"))    target.setDescription(strOrNull(m.get("description")));
            if (m.containsKey("lore"))           target.setLore(strOrNull(m.get("lore")));
            if (m.containsKey("giveCommand"))    target.setGiveCommand(strOrNull(m.get("giveCommand")));
            if (m.containsKey("propertiesJson")) target.setPropertiesJson(strOrNull(m.get("propertiesJson")));
            if (m.containsKey("effectsJson"))    target.setEffectsJson(strOrNull(m.get("effectsJson")));
            if (m.containsKey("recipeJson"))     target.setRecipeJson(strOrNull(m.get("recipeJson")));
            if (m.containsKey("imageUrl"))       target.setImageUrl(strOrNull(m.get("imageUrl")));
            // Respeita enabled=false explícito do JSON, mas pra NOVO item
            // sempre default true (visibilidade pros testers). Antes podia
            // criar item desabilitado se o JSON tivesse "enabled":false sem
            // querer, ou se o gerador esquecesse a key.
            if (m.get("enabled") instanceof Boolean b) {
                target.setEnabled(b);
            } else if (found == null) {
                target.setEnabled(true);
            }
            if (found == null) {
                target.setCreatedBy("bulk-import");
                testerSvc.createBetaItem(target);
                return "created";
            }
            testerSvc.updateBetaItem(target.getId(), target);
            return "updated";
        });
    }

    // ============================================================
    // REWARDS
    // ============================================================
    @PostMapping("/api/admin/tester/rewards/bulk-import")
    @Transactional
    public ResponseEntity<?> bulkImportRewards(@RequestBody Map<String, Object> payload) {
        return processBulk(payload, "rewards", (m, errors) -> {
            String name = strOrNull(m.get("name"));
            if (name == null || name.isBlank()) { errors.add("entry sem 'name'"); return null; }
            List<Reward> existing = testerSvc.listRewards(false);
            Reward found = existing.stream()
                    .filter(r -> name.equalsIgnoreCase(r.getName()))
                    .findFirst().orElse(null);
            Reward target = (found != null) ? found : new Reward();
            target.setName(name);
            if (m.containsKey("description")) target.setDescription(strOrNull(m.get("description")));
            if (m.containsKey("imageUrl"))    target.setImageUrl(strOrNull(m.get("imageUrl")));
            if (m.get("costPoints") instanceof Number n) target.setCostPoints(n.intValue());
            if (m.containsKey("giveCommand")) target.setGiveCommand(strOrNull(m.get("giveCommand")));
            if (m.get("perTesterLimit") instanceof Number n) target.setPerTesterLimit(n.intValue());
            if (m.containsKey("category"))    target.setCategory(strOrNull(m.get("category")));
            // Mesma proteção do beta-items: NOVO reward default enabled=true.
            if (m.get("enabled") instanceof Boolean b) {
                target.setEnabled(b);
            } else if (found == null) {
                target.setEnabled(true);
            }
            if (found == null) {
                testerSvc.createReward(target);
                return "created";
            }
            testerSvc.updateReward(target.getId(), target);
            return "updated";
        });
    }

    // ============================================================
    // CHANGELOG
    // ============================================================
    @PostMapping("/api/admin/tester/changelog/bulk-import")
    @Transactional
    public ResponseEntity<?> bulkImportChangelog(@RequestBody Map<String, Object> payload) {
        return processBulk(payload, "entries", (m, errors) -> {
            String title = strOrNull(m.get("title"));
            if (title == null || title.isBlank()) { errors.add("entry sem 'title'"); return null; }
            // Procura existente — listChangelog() retorna List<Map<String,Object>> com 'id' e 'title'
            List<Map<String, Object>> all = publicSvc.listChangelog();
            Map<String, Object> existing = all.stream()
                    .filter(c -> title.equalsIgnoreCase((String) c.get("title")))
                    .findFirst().orElse(null);
            // PublicContentService.applyChangelog aceita o body Map direto
            Map<String, Object> body = new LinkedHashMap<>(m);
            if (existing == null) {
                publicSvc.createChangelog(body, "bulk-import");
                return "created";
            }
            Long id = ((Number) existing.get("id")).longValue();
            publicSvc.updateChangelog(id, body);
            return "updated";
        });
    }

    // ============================================================
    // ROADMAP
    // ============================================================
    @PostMapping("/api/admin/tester/roadmap/bulk-import")
    @Transactional
    public ResponseEntity<?> bulkImportRoadmap(@RequestBody Map<String, Object> payload) {
        return processBulk(payload, "items", (m, errors) -> {
            String title = strOrNull(m.get("title"));
            if (title == null || title.isBlank()) { errors.add("entry sem 'title'"); return null; }
            List<Map<String, Object>> all = publicSvc.listRoadmap();
            Map<String, Object> existing = all.stream()
                    .filter(r -> title.equalsIgnoreCase((String) r.get("title")))
                    .findFirst().orElse(null);
            Map<String, Object> body = new LinkedHashMap<>(m);
            if (existing == null) {
                publicSvc.createRoadmap(body, "bulk-import");
                return "created";
            }
            Long id = ((Number) existing.get("id")).longValue();
            publicSvc.updateRoadmap(id, body);
            return "updated";
        });
    }

    // ============================================================
    // FEATURE WIKI
    // ============================================================
    @PostMapping("/api/admin/feature-wiki/bulk-import")
    @Transactional
    public ResponseEntity<?> bulkImportWiki(@RequestBody Map<String, Object> payload) {
        return processBulk(payload, "entries", (m, errors) -> {
            String slug  = strOrNull(m.get("slug"));
            String title = strOrNull(m.get("title"));
            if (slug == null || slug.isBlank())   { errors.add("entry sem 'slug'");  return null; }
            if (title == null || title.isBlank()) { errors.add("entry '" + slug + "' sem 'title'"); return null; }

            // Slugify pra bater com o lookup de findBySlug (que normaliza igual no save)
            String normalizedSlug = slug.trim().toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-+|-+$", "");
            var found = extSvc.getWikiBySlug(normalizedSlug).orElse(null);
            FeatureWikiEntry target = (found != null) ? found : new FeatureWikiEntry();
            target.setSlug(normalizedSlug);
            target.setTitle(title);
            if (m.containsKey("itemId"))         target.setItemId(strOrNull(m.get("itemId")));
            if (m.containsKey("imageUrl"))       target.setImageUrl(strOrNull(m.get("imageUrl")));
            if (m.containsKey("summary"))        target.setSummary(strOrNull(m.get("summary")));
            if (m.containsKey("contentMd")) {
                String md = strOrNull(m.get("contentMd"));
                if (md != null) target.setContentMd(md);
            } else if (found == null) {
                // Novo entry sem contentMd — saveWiki vai falhar; aborta pra logar erro claro
                errors.add("entry '" + slug + "' sem 'contentMd'");
                return null;
            }
            if (m.containsKey("creditsJson"))    target.setCreditsJson(jsonStrOrNull(m.get("creditsJson")));
            if (m.containsKey("recipeJson"))     target.setRecipeJson(jsonStrOrNull(m.get("recipeJson")));
            if (m.containsKey("addedInVersion")) target.setAddedInVersion(strOrNull(m.get("addedInVersion")));
            if (m.containsKey("tags"))           target.setTags(strOrNull(m.get("tags")));
            if (m.containsKey("category")) {
                try { target.setCategory(FeatureWikiEntry.Category.valueOf(strOrNull(m.get("category")).toUpperCase())); }
                catch (Exception ignored) { if (found == null) target.setCategory(FeatureWikiEntry.Category.ITEM); }
            } else if (found == null) {
                target.setCategory(FeatureWikiEntry.Category.ITEM);
            }
            if (m.get("published") instanceof Boolean b) {
                target.setPublished(b);
            } else if (found == null) {
                target.setPublished(true);
            }
            extSvc.saveWiki(target, "bulk-import");
            return (found != null) ? "updated" : "created";
        });
    }

    // ============================================================
    // Helpers
    // ============================================================

    @FunctionalInterface
    private interface EntryProcessor {
        /** Retorna "created", "updated" ou null se falhou. */
        String process(Map<String, Object> entry, List<String> errors);
    }

    private ResponseEntity<?> processBulk(Map<String, Object> payload, String arrayKey, EntryProcessor proc) {
        Object arrObj = payload.get(arrayKey);
        if (!(arrObj instanceof List<?> raw)) {
            return ResponseEntity.badRequest().body(Map.of("error", "field '" + arrayKey + "' must be array"));
        }
        int created = 0, updated = 0, errors = 0;
        List<String> errorMessages = new ArrayList<>();

        for (Object item : raw) {
            if (!(item instanceof Map<?, ?> m)) {
                errors++;
                errorMessages.add("entry não é object");
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> mTyped = (Map<String, Object>) m;
            try {
                String result = proc.process(mTyped, errorMessages);
                if ("created".equals(result)) created++;
                else if ("updated".equals(result)) updated++;
                else errors++;
            } catch (Exception e) {
                errors++;
                errorMessages.add(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }

        LOG.info("[BulkImport] {}: {} created, {} updated, {} errors", arrayKey, created, updated, errors);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("created", created);
        resp.put("updated", updated);
        resp.put("errors", errors);
        resp.put("errorMessages", errorMessages);
        return ResponseEntity.ok(resp);
    }

    private static String strOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof String s) return s.isEmpty() ? null : s;
        return o.toString();
    }

    /**
     * Aceita campo JSON como String (já serializado) OU como objeto/array
     * (e serializa pra String). Útil pra creditsJson/recipeJson onde o autor
     * do JSON quer escrever ["Bob", "Alice"] em vez de "[\"Bob\",\"Alice\"]".
     */
    private static String jsonStrOrNull(Object o) {
        if (o == null) return null;
        if (o instanceof String s) return s.isEmpty() ? null : s;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return o.toString();
        }
    }
}
