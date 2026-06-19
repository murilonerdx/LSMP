package br.com.murilo.liberthia.admin.engine.kv;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * REST genérico pra ler/gravar configs JSON por chave.
 *
 *   GET    /api/kv/{key}        → { key, dataJson, updatedAt }
 *   PUT    /api/kv/{key}        body: {dataJson} → salva
 *   DELETE /api/kv/{key}        → remove
 *   GET    /api/kv?prefix=xxx   → lista todas com prefixo
 *
 * Validação: keys são restritas a [a-z0-9_.\\-] pra evitar abuso.
 */
@RestController
@RequestMapping("/api/kv")
public class KvController {

    private static final Pattern KEY_OK = Pattern.compile("^[a-zA-Z0-9_.\\-]{1,128}$");

    private final KvRepository repo;

    public KvController(KvRepository repo) { this.repo = repo; }

    @GetMapping("/{key}")
    public Map<String, Object> get(@PathVariable String key) {
        validateKey(key);
        KvConfig kv = repo.findById(key).orElse(null);
        Map<String, Object> out = new HashMap<>();
        out.put("key", key);
        out.put("dataJson", kv == null ? null : kv.getDataJson());
        out.put("updatedAt", kv == null ? null : kv.getUpdatedAt());
        return out;
    }

    @PutMapping("/{key}")
    public Map<String, Object> put(@PathVariable String key, @RequestBody Map<String, String> body) {
        validateKey(key);
        KvConfig kv = repo.findById(key).orElse(new KvConfig(key, ""));
        kv.setDataJson(body.getOrDefault("dataJson", ""));
        repo.save(kv);
        return Map.of("ok", true, "key", key);
    }

    @DeleteMapping("/{key}")
    public Map<String, Object> del(@PathVariable String key) {
        validateKey(key);
        repo.deleteById(key);
        return Map.of("ok", true);
    }

    @GetMapping("")
    public Map<String, Object> list(@RequestParam(required = false) String prefix) {
        List<KvConfig> all = repo.findAll();
        if (prefix != null && !prefix.isBlank()) {
            all = all.stream().filter(k -> k.getKey().startsWith(prefix)).toList();
        }
        return Map.of("configs", all);
    }

    private void validateKey(String key) {
        if (key == null || !KEY_OK.matcher(key).matches())
            throw new IllegalArgumentException("invalid key: " + key);
    }
}
