package br.com.murilo.liberthia.admin.engine.banneditem;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Gerenciador de itens banidos — funciona 100% pelo painel web.
 *
 * Fluxo:
 *   1. Admin adiciona itemId no painel (com autocomplete usando /api/items)
 *   2. Backend salva no banco
 *   3. Se mode=auto_clear ou silent → scheduler dispara /clear @a {itemId} a cada 30s
 *   4. Se mode=auto_clear ou broadcast → tellraw broadcast de banimento na criação
 *
 * Não precisa mexer no mod do MC — usa /clear vanilla via ModBridgeClient.
 */
@RestController
@RequestMapping("/api/banned-items")
public class BannedItemController {

    private static final Logger LOG = LoggerFactory.getLogger(BannedItemController.class);

    private final BannedItemRepository repo;
    private final ModBridgeClient mod;

    public BannedItemController(BannedItemRepository repo, ModBridgeClient mod) {
        this.repo = repo;
        this.mod = mod;
    }

    // =========================================================================
    // CRUD
    // =========================================================================

    @GetMapping
    public List<BannedItem> list() { return repo.findAllByOrderByBannedAtDesc(); }

    @GetMapping("/{id}")
    public ResponseEntity<BannedItem> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    public record BanRequest(String itemId, String displayName, String reason,
                             String mode, String bannedBy, Boolean active) {}

    @PostMapping
    public ResponseEntity<?> ban(@RequestBody BanRequest req) {
        if (req.itemId == null || req.itemId.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "itemId obrigatório"));
        // Normaliza pra minecraft:xxx se não veio com namespace
        String itemId = req.itemId.contains(":") ? req.itemId : "minecraft:" + req.itemId;

        // Já existe? Só atualiza
        BannedItem b = repo.findByItemId(itemId).orElseGet(BannedItem::new);
        b.setItemId(itemId);
        if (req.displayName != null) b.setDisplayName(req.displayName);
        if (req.reason != null) b.setReason(req.reason);
        if (req.mode != null) b.setMode(req.mode);
        else if (b.getMode() == null) b.setMode("auto_clear");
        if (req.bannedBy != null) b.setBannedBy(req.bannedBy);
        b.setActive(req.active == null ? true : req.active);
        BannedItem saved = repo.save(b);

        // Dispara o clear imediato pra todos os players (e broadcast se aplicável)
        triggerClear(saved, /*immediate=*/true);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BannedItem> update(@PathVariable Long id, @RequestBody BanRequest req) {
        return repo.findById(id).map(b -> {
            if (req.displayName != null) b.setDisplayName(req.displayName);
            if (req.reason != null) b.setReason(req.reason);
            if (req.mode != null) b.setMode(req.mode);
            if (req.bannedBy != null) b.setBannedBy(req.bannedBy);
            if (req.active != null) b.setActive(req.active);
            return ResponseEntity.ok(repo.save(b));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> unban(@PathVariable Long id) {
        repo.findById(id).ifPresent(b -> {
            // Broadcast de un-ban se estava ativo
            if (b.isActive() && !"silent".equals(b.getMode())) {
                String name = b.getDisplayName() != null ? b.getDisplayName() : b.getItemId();
                mod.runCommand("tellraw @a {\"text\":\"§a✓ Item §f" + escape(name) +
                        " §anão está mais banido.\"}");
            }
            repo.deleteById(id);
        });
        return Map.of("ok", true);
    }

    /** Liga/desliga ban sem deletar registro. */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<BannedItem> toggle(@PathVariable Long id) {
        return repo.findById(id).map(b -> {
            b.setActive(!b.isActive());
            return ResponseEntity.ok(repo.save(b));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Dispara /clear imediato — sem esperar o scheduler. */
    @PostMapping("/{id}/enforce")
    public Map<String, Object> enforceNow(@PathVariable Long id) {
        BannedItem b = repo.findById(id).orElse(null);
        if (b == null) return Map.of("ok", false, "error", "not found");
        triggerClear(b, /*immediate=*/true);
        return Map.of("ok", true);
    }

    /**
     * Autocomplete: busca items pelo nome no /api/items do mod.
     *
     * Formato real do mod: {"items":[{"id":"minecraft:diamond_sword","name":"Diamond Sword"}, ...]}
     * — objeto envelope com array, campo `name` (NÃO `displayName`).
     */
    @GetMapping("/search-items")
    public Object searchItems(@RequestParam(required = false) String q,
                              @RequestParam(required = false, defaultValue = "50") int limit) {
        try {
            JsonNode resp = mod.getItems();
            JsonNode arr = resp == null ? null : resp.path("items");
            String needle = q == null ? "" : q.toLowerCase();
            List<Map<String, Object>> out = new ArrayList<>();
            if (arr != null && arr.isArray()) {
                for (JsonNode item : arr) {
                    String id = item.path("id").asText("");
                    // Suporta os 2 nomes de campo: "name" (formato atual) e "displayName" (legado)
                    String name = item.path("name").asText(item.path("displayName").asText(id));
                    if (id.isEmpty()) continue;
                    if (needle.isEmpty()
                            || id.toLowerCase().contains(needle)
                            || name.toLowerCase().contains(needle)) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", id);
                        row.put("displayName", name);
                        out.add(row);
                        if (out.size() >= limit) break;
                    }
                }
            }
            return Map.of("items", out, "count", out.size());
        } catch (Exception e) {
            LOG.warn("[BannedItem] search-items falhou: {}", e.getMessage());
            return Map.of("items", List.of(), "count", 0, "error", e.getMessage());
        }
    }

    // =========================================================================
    // ENFORCEMENT
    // =========================================================================

    /** Scheduler — a cada 30s tira items banidos de todo player. */
    @Scheduled(fixedDelay = 30_000)
    public void enforcementTick() {
        List<BannedItem> actives = repo.findByActiveTrue();
        for (BannedItem b : actives) {
            if ("broadcast".equals(b.getMode())) continue; // só anuncia, não remove
            triggerClear(b, /*immediate=*/false);
        }
    }

    private void triggerClear(BannedItem b, boolean immediate) {
        if (!b.isActive()) return;
        try {
            String mode = b.getMode() == null ? "auto_clear" : b.getMode();
            // Remove dos inventários (exceto modo "broadcast")
            if (!"broadcast".equals(mode)) {
                mod.runCommand("clear @a " + b.getItemId());
                b.setClearCount(b.getClearCount() + 1);
                repo.save(b);
            }
            // Broadcast no chat (exceto modo "silent")
            if (immediate && !"silent".equals(mode)) {
                String name = b.getDisplayName() != null ? b.getDisplayName() : b.getItemId();
                String reason = b.getReason() == null || b.getReason().isBlank()
                        ? "" : " §7(motivo: " + escape(b.getReason()) + ")";
                mod.runCommand("tellraw @a {\"text\":\"§c⛔ Item §f" + escape(name)
                        + " §cfoi banido do servidor." + reason + "\"}");
            }
        } catch (Exception e) {
            LOG.warn("[BannedItem] enforce {} falhou: {}", b.getItemId(), e.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
