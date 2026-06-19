package br.com.murilo.liberthia.admin.engine.pilgrimage;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Repository
interface PilgrimageRepository extends JpaRepository<Pilgrimage, String> {}

@Repository
interface PilgrimageProgressRepository extends JpaRepository<PilgrimageProgress, Long> {
    @Query("SELECT p FROM PilgrimageProgress p WHERE p.playerUuid = :uuid AND p.completedAt IS NULL")
    List<PilgrimageProgress> findActiveByPlayer(@Param("uuid") String uuid);

    @Query("SELECT p FROM PilgrimageProgress p WHERE p.playerUuid = :uuid AND p.pilgrimageId = :pid")
    Optional<PilgrimageProgress> findByPlayerAndPilgrimage(@Param("uuid") String uuid, @Param("pid") String pid);
}

/**
 * PilgrimageEngine: detecta avanço de estações via polling de posição do player.
 *
 * Cada estação tem `type` (memorial, glyph, anchor, coords) e referência.
 * O engine resolve coords da estação (via DB de Memorial/Glyph/Anchor ou direto
 * do JSON pra type=coords) e verifica se algum player com progresso ativo está
 * dentro do raio.
 *
 * Roda a cada 2s. Cada player pode ter múltiplas pilgrimages ativas simultâneas.
 */
@Service
public class PilgrimageEngine {

    private static final Logger LOG = LoggerFactory.getLogger(PilgrimageEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double DEFAULT_RADIUS = 5.0;

    private final PilgrimageRepository repo;
    private final PilgrimageProgressRepository progressRepo;
    private final EngineActions actions;
    private final StationResolver resolver;

    public PilgrimageEngine(PilgrimageRepository repo, PilgrimageProgressRepository progressRepo,
                            EngineActions actions, StationResolver resolver) {
        this.repo = repo;
        this.progressRepo = progressRepo;
        this.actions = actions;
        this.resolver = resolver;
    }

    @Scheduled(fixedDelay = 2000L, initialDelay = 5000L)
    @Transactional
    public void tick() {
        var players = actions.getPlayers();
        if (players.isEmpty()) return;

        var enabledPilgrimages = repo.findAll().stream()
                .filter(Pilgrimage::isEnabled).toList();
        if (enabledPilgrimages.isEmpty()) return;

        for (var p : players) {
            // Para cada player, pega progresso ativo
            var activeProgresses = progressRepo.findActiveByPlayer(p.uuid());

            for (var pilgrimage : enabledPilgrimages) {
                // Player já tem progresso ativo nessa pilgrimage? Senão, AUTO-iniciar.
                var progress = activeProgresses.stream()
                        .filter(pr -> pr.getPilgrimageId().equals(pilgrimage.getId()))
                        .findFirst()
                        .orElse(null);

                if (progress == null) {
                    // Auto-start quando player chega na PRIMEIRA estação (sentinel)
                    if (isAtStep(p, pilgrimage, 0)) {
                        progress = new PilgrimageProgress(p.uuid(), p.name(), pilgrimage.getId());
                        progressRepo.save(progress);
                        onStart(p, pilgrimage);
                        // Já está na step 0 — avança pra 1
                        advance(p, pilgrimage, progress);
                    }
                    continue;
                }

                // Time limit excedido? Reseta.
                if (pilgrimage.getTimeLimitSec() > 0 && progress.getStartedAt() != null) {
                    Duration since = Duration.between(progress.getStartedAt(), Instant.now());
                    if (since.getSeconds() > pilgrimage.getTimeLimitSec()) {
                        LOG.info("[Pilgrimage] {} excedeu limite de tempo em '{}', resetando",
                                p.name(), pilgrimage.getName());
                        actions.title(p.uuid(), "§c§l⏳ TEMPO ESGOTADO", "§c§oPeregrinação reiniciada", 10, 60, 20);
                        progressRepo.delete(progress);
                        continue;
                    }
                }

                // Player chegou na próxima estação?
                if (isAtStep(p, pilgrimage, progress.getCurrentStep())) {
                    advance(p, pilgrimage, progress);
                }
            }
        }
    }

    private boolean isAtStep(EngineActions.PlayerInfo p, Pilgrimage pg, int stepIdx) {
        try {
            JsonNode steps = MAPPER.readTree(pg.getStepsJson() == null ? "[]" : pg.getStepsJson());
            if (!steps.isArray() || stepIdx < 0 || stepIdx >= steps.size()) return false;
            JsonNode step = steps.get(stepIdx);
            StationResolver.Station st = resolver.resolve(step);
            if (st == null) return false;
            // Mesmo plano de dimensão? Compara aceitando overworld vs minecraft:overworld
            String pDim = p.dimension().replace("minecraft:", "");
            String sDim = st.dim.replace("minecraft:", "");
            if (!pDim.equalsIgnoreCase(sDim)) return false;
            double dx = p.x() - st.x;
            double dy = p.y() - st.y;
            double dz = p.z() - st.z;
            return (dx * dx + dy * dy + dz * dz) <= (st.radius * st.radius);
        } catch (Exception e) {
            LOG.debug("isAtStep fail: {}", e.getMessage());
            return false;
        }
    }

    private void advance(EngineActions.PlayerInfo p, Pilgrimage pg, PilgrimageProgress progress) {
        int totalSteps = countSteps(pg);
        int next = progress.getCurrentStep() + 1;
        progress.setCurrentStep(next);

        if (next >= totalSteps) {
            // Completou!
            progress.setCompletedAt(Instant.now());
            progressRepo.save(progress);
            onComplete(p, pg);
        } else {
            progressRepo.save(progress);
            onStep(p, pg, next, totalSteps);
        }
    }

    private int countSteps(Pilgrimage pg) {
        try {
            JsonNode steps = MAPPER.readTree(pg.getStepsJson() == null ? "[]" : pg.getStepsJson());
            return steps.isArray() ? steps.size() : 0;
        } catch (Exception e) { return 0; }
    }

    private void onStart(EngineActions.PlayerInfo p, Pilgrimage pg) {
        String msg = pg.getStartMsg();
        if (msg != null && !msg.isBlank()) actions.tellraw(p.name(), msg.replace("{player}", p.name()));
        actions.title(p.uuid(), pg.getEmoji() + " " + pg.getName(), "§7§o— peregrinação iniciada —", 10, 70, 20);
        LOG.info("[Pilgrimage] {} iniciou '{}'", p.name(), pg.getName());
    }

    private void onStep(EngineActions.PlayerInfo p, Pilgrimage pg, int step, int total) {
        String msg = pg.getStepMsg();
        if (msg != null && !msg.isBlank()) actions.tellraw(p.name(), msg.replace("{player}", p.name())
                .replace("{step}", String.valueOf(step))
                .replace("{total}", String.valueOf(total)));
        actions.title(p.uuid(), pg.getEmoji() + " " + step + "/" + total, "§7§o— estação alcançada —", 5, 40, 15);
        if (pg.getStepSound() != null && !pg.getStepSound().isBlank()) actions.sound(p.uuid(), pg.getStepSound(), 1, 1.2);
        LOG.info("[Pilgrimage] {} avançou '{}': {}/{}", p.name(), pg.getName(), step, total);
    }

    private void onComplete(EngineActions.PlayerInfo p, Pilgrimage pg) {
        String msg = pg.getCompleteMsg();
        if (msg != null && !msg.isBlank()) actions.tellraw(p.name(), msg.replace("{player}", p.name()));
        actions.title(p.uuid(), pg.getEmoji() + " " + pg.getName(), "§a§l⛧ PEREGRINAÇÃO CONCLUÍDA ⛧", 10, 100, 30);
        if (pg.getCompleteSound() != null && !pg.getCompleteSound().isBlank()) actions.sound(p.uuid(), pg.getCompleteSound(), 1, 1);
        if (pg.getRewardCmd() != null && !pg.getRewardCmd().isBlank()) {
            actions.runCommand(pg.getRewardCmd().replace("{player}", p.name()));
        }
        LOG.info("[Pilgrimage] {} COMPLETOU '{}'", p.name(), pg.getName());
    }

    // --- API ---
    public List<Pilgrimage> list() { return repo.findAll(); }
    public Pilgrimage save(Pilgrimage p) { return repo.save(p); }
    public void delete(String id) { repo.deleteById(id); }
    public Pilgrimage toggle(String id) {
        Pilgrimage p = repo.findById(id).orElseThrow();
        p.setEnabled(!p.isEnabled());
        return repo.save(p);
    }
    public List<PilgrimageProgress> listProgress() { return progressRepo.findAll(); }
    public void resetProgress(String playerUuid, String pilgrimageId) {
        progressRepo.findByPlayerAndPilgrimage(playerUuid, pilgrimageId)
                .ifPresent(progressRepo::delete);
    }
}

@RestController
@RequestMapping("/api/pilgrimages")
class PilgrimageController {
    private final PilgrimageEngine engine;
    PilgrimageController(PilgrimageEngine engine) { this.engine = engine; }

    @GetMapping("")
    public Map<String, Object> all() {
        return Map.of("pilgrimages", engine.list(), "progress", engine.listProgress());
    }

    @PostMapping("")
    public Pilgrimage save(@RequestBody Pilgrimage p) {
        if (p.getId() == null || p.getId().isBlank())
            p.setId("pil_" + Long.toHexString(System.currentTimeMillis()));
        return engine.save(p);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        engine.delete(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/toggle")
    public Pilgrimage toggle(@PathVariable String id) { return engine.toggle(id); }

    @PostMapping("/{id}/reset/{playerUuid}")
    public Map<String, Object> resetProgress(@PathVariable String id, @PathVariable String playerUuid) {
        engine.resetProgress(playerUuid, id);
        return Map.of("ok", true);
    }
}
