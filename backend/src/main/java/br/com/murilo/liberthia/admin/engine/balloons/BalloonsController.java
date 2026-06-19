package br.com.murilo.liberthia.admin.engine.balloons;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Painel pro sistema de balões de fala (TalkBalloons + ComicsBubbles + Emojiful).
 *
 * Endpoints:
 *  GET  /api/balloons                    → lista balões capturados/criados
 *  POST /api/balloons/say                → admin faz player "falar" (broadcast no chat → balão renderiza)
 *  POST /api/balloons/capture            → mod posta balão capturado do chat
 *  GET  /api/balloons/stats              → top players + counters
 *  DELETE /api/balloons/{id}
 */
@RestController
@RequestMapping("/api/balloons")
public class BalloonsController {

    private final BalloonRepository repo;
    private final ModBridgeClient mod;

    public BalloonsController(BalloonRepository repo, ModBridgeClient mod) {
        this.repo = repo;
        this.mod = mod;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String playerUuid,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        var pg = PageRequest.of(page, Math.min(size, 300));
        var result = playerUuid != null && !playerUuid.isBlank()
                ? repo.findByPlayerUuidOrderByTsDesc(playerUuid, pg)
                : source != null && !source.isBlank()
                ? repo.findBySourceOrderByTsDesc(source, pg)
                : repo.findAllByOrderByTsDesc(pg);
        return Map.of("content", result.getContent(), "total", result.getTotalElements());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<Object[]> rows = repo.aggregateByPlayer();
        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] r : rows) {
            top.add(Map.of(
                    "playerUuid", r[0],
                    "playerName", r[1],
                    "count", ((Number) r[2]).longValue(),
                    "lastTs", r[3] == null ? 0L : ((Number) r[3]).longValue()
            ));
        }
        return Map.of(
                "topPlayers", top,
                "captured", repo.countBySource("captured"),
                "admin", repo.countBySource("admin"),
                "total", repo.count()
        );
    }

    public record SayRequest(String playerName, String text, String type,
                             String imageUrl, String createdBy) {}

    /**
     * Admin faz um player "dizer" algo. Backend dispara /tellraw com a estrutura
     * que o TalkBalloons / Comics Bubbles intercepta (chat-style message).
     *
     * Player vê parecendo que outro player falou.
     */
    @PostMapping("/say")
    public Map<String, Object> say(@RequestBody SayRequest req) {
        if (req.playerName == null || req.playerName.isBlank())
            return Map.of("ok", false, "error", "playerName missing");
        if (req.text == null || req.text.isBlank())
            return Map.of("ok", false, "error", "text vazio");

        // Mod TalkBalloons intercepta chat normal (formato vanilla "<Player> msg").
        // Pra forçar o balão visualmente, usamos /tellraw com prefixo de chat
        // formatado igual o vanilla — assim ambos os mods (TalkBalloons e Comics)
        // pegam e renderizam acima da cabeça.
        String prefix = switch (req.type == null ? "talk" : req.type) {
            case "shout"   -> "§l!!§r ";
            case "thought" -> "§o";
            case "comic"   -> "§n";
            default        -> "";
        };
        String text = escape(prefix + req.text);
        // /tellraw como se fosse mensagem de chat normal — o balão renderiza
        String cmd = "tellraw @a [\"\",{\"text\":\"<" + escape(req.playerName) +
                "> \",\"color\":\"white\"},{\"text\":\"" + text + "\"}]";
        mod.runCommand(cmd);

        // Salva no banco
        Balloon b = new Balloon();
        b.setPlayerName(req.playerName);
        b.setText(req.text);
        b.setType(req.type == null ? "talk" : req.type);
        b.setImageUrl(req.imageUrl);
        b.setSource("admin");
        b.setTs(System.currentTimeMillis());
        b.setCreatedBy(req.createdBy);
        repo.save(b);

        return Map.of("ok", true, "id", b.getId());
    }

    /**
     * Endpoint chamado pelo mod (ServerChatEvent hook) pra salvar balões
     * capturados automaticamente. Auth via X-Liberthia-Token igual /api/mod/*.
     */
    @PostMapping("/capture")
    public Map<String, Object> capture(@RequestBody Balloon in) {
        in.setSource("captured");
        if (in.getType() == null) in.setType("talk");
        if (in.getTs() == 0) in.setTs(System.currentTimeMillis());
        return Map.of("ok", true, "id", repo.save(in).getId());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("ok", true);
    }

    @PostMapping("/bulk-delete")
    public Map<String, Object> bulkDelete(@RequestBody Map<String, Object> body) {
        Object raw = body.get("ids");
        if (!(raw instanceof List<?> rawList)) return Map.of("ok", false, "deleted", 0);
        int n = 0;
        for (Object o : rawList) {
            if (o instanceof Number num) { repo.deleteById(num.longValue()); n++; }
        }
        return Map.of("ok", true, "deleted", n);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
