package br.com.murilo.liberthia.admin.engine.history;

import br.com.murilo.liberthia.admin.engine.EngineEventBus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Persiste chat e comandos no PostgreSQL. Subscreve EngineEventBus pra capturar
 * eventos do mod e gravar.
 */

@Repository
interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
    @Query("SELECT c FROM ChatLog c WHERE (:uuid IS NULL OR :uuid = '' OR c.uuid = :uuid) AND c.ts > :since ORDER BY c.ts DESC")
    List<ChatLog> findRecent(@Param("uuid") String uuid, @Param("since") Instant since, org.springframework.data.domain.Pageable p);
}

@Repository
interface CommandLogRepository extends JpaRepository<CommandLog, Long> {
    @Query("SELECT c FROM CommandLog c WHERE (:uuid IS NULL OR :uuid = '' OR c.uuid = :uuid) AND c.ts > :since ORDER BY c.ts DESC")
    List<CommandLog> findRecent(@Param("uuid") String uuid, @Param("since") Instant since, org.springframework.data.domain.Pageable p);
}

@Service
class HistoryService {
    private static final Logger LOG = LoggerFactory.getLogger(HistoryService.class);
    private final ChatLogRepository chatRepo;
    private final CommandLogRepository cmdRepo;
    private final EngineEventBus bus;

    HistoryService(ChatLogRepository chatRepo, CommandLogRepository cmdRepo, EngineEventBus bus) {
        this.chatRepo = chatRepo;
        this.cmdRepo = cmdRepo;
        this.bus = bus;
    }

    @PostConstruct
    void init() {
        bus.subscribe(this::onModEvent);
        LOG.info("HistoryService ✓ persistindo chat/command no DB");
    }

    private void onModEvent(JsonNode event) {
        String type = event.path("type").asText("");
        JsonNode data = event.get("data"); if (data == null) return;
        try {
            switch (type) {
                case "chat":
                    chatRepo.save(new ChatLog(
                            data.path("uuid").asText(""),
                            data.path("name").asText(""),
                            data.path("message").asText("")));
                    break;
                case "command":
                    cmdRepo.save(new CommandLog(
                            data.path("uuid").asText(""),
                            data.path("name").asText(""),
                            data.path("command").asText(""),
                            data.path("isPlayer").asBoolean(false)));
                    break;
            }
        } catch (Exception e) { LOG.debug("history save fail: {}", e.getMessage()); }
    }

    public List<ChatLog> chatRecent(String uuid, long since, int limit) {
        return chatRepo.findRecent(uuid, Instant.ofEpochMilli(since), PageRequest.of(0, Math.min(500, Math.max(1, limit))));
    }

    public List<CommandLog> cmdRecent(String uuid, long since, int limit) {
        return cmdRepo.findRecent(uuid, Instant.ofEpochMilli(since), PageRequest.of(0, Math.min(500, Math.max(1, limit))));
    }
}

@RestController
@RequestMapping("/api/history-db")
class HistoryController {
    private final HistoryService svc;
    HistoryController(HistoryService svc) { this.svc = svc; }

    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam(defaultValue = "") String uuid,
                                     @RequestParam(defaultValue = "0") long since,
                                     @RequestParam(defaultValue = "200") int limit) {
        return Map.of("entries", svc.chatRecent(uuid, since, limit));
    }

    @GetMapping("/commands")
    public Map<String, Object> commands(@RequestParam(defaultValue = "") String uuid,
                                         @RequestParam(defaultValue = "0") long since,
                                         @RequestParam(defaultValue = "200") int limit) {
        return Map.of("entries", svc.cmdRecent(uuid, since, limit));
    }
}
