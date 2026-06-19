package br.com.murilo.liberthia.admin.engine.voice;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import br.com.murilo.liberthia.admin.mod.ModRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de voz divididos em dois grupos:
 *
 *  <p>1. <b>/api/mod/voice/*</b> — usado APENAS pelo mod. Autenticação via
 *  header <code>X-Liberthia-Token</code> (compartilhado entre mod e backend).
 *  Esse path é liberado no AuthFilter (não precisa Bearer).
 *
 *  <p>2. <b>/api/voice/*</b> — usado pelo painel (browser). Autenticação via
 *  Bearer normal (filtro padrão).
 */
@RestController
public class VoiceClipController {

    private final VoiceClipService svc;
    private final ModRegistry registry;
    private final ModBridgeClient mod;
    private final WhisperTranscriptionService whisperSvc;
    private final VoiceRetentionService retentionSvc;
    private final WhisperRuntimeConfig whisperConfig;

    public VoiceClipController(VoiceClipService svc, ModRegistry registry,
                               ModBridgeClient mod,
                               WhisperTranscriptionService whisperSvc,
                               VoiceRetentionService retentionSvc,
                               WhisperRuntimeConfig whisperConfig) {
        this.svc = svc;
        this.registry = registry;
        this.mod = mod;
        this.whisperSvc = whisperSvc;
        this.retentionSvc = retentionSvc;
        this.whisperConfig = whisperConfig;
    }

    /**
     * Status do scheduler do Whisper — útil pra debug. Mostra se está ativo,
     * quantos ticks rodou, qual foi o último resultado, modelo carregado, etc.
     * Endpoint público no painel: GET /api/voice/whisper/status
     */
    @GetMapping("/api/voice/whisper/status")
    public Map<String, Object> whisperStatus() {
        return whisperSvc.status();
    }

    /** Lê config runtime do Whisper (do banco). */
    @GetMapping("/api/voice/whisper/config")
    public Map<String, Object> whisperConfigGet() {
        return whisperConfig.get().toMap();
    }

    /**
     * Atualiza config runtime do Whisper. Body parcial — só os campos enviados
     * são alterados. Validação automática (clamp em ranges).
     *
     * <p>Body pode conter: enabled, scheduleEnabled, scheduleHourFrom/To,
     * threads (1..8), beamSize (1..10), bestOf (1..10), workers (1..3),
     * cpuPriority (0..19).
     *
     * <p>Mudança aplica no próximo tick (até 5s). Sem restart.
     */
    @PutMapping("/api/voice/whisper/config")
    public Map<String, Object> whisperConfigPut(@RequestBody Map<String, Object> body) {
        return whisperConfig.update(body).toMap();
    }

    /**
     * Pause rápido — atalho que faz PUT {enabled: false} + MATA workers em andamento.
     *
     * <p>v0.1.13: antes pausar só impedia NOVOS pickups; workers já rodando finalizavam
     * o clipe atual (~50s) antes de parar. Usuário via transcrições continuarem e ficava
     * confuso. Agora killAllActive() mata os processos whisper-cli imediatamente — os
     * clipes em andamento ficam FAILED ("killed via pause") e podem ser retranscrição
     * via /api/voice/clips/{id}/transcribe quando user der resume.
     */
    @PostMapping("/api/voice/whisper/pause")
    public Map<String, Object> whisperPause() {
        Map<String, Object> result = whisperConfig.update(java.util.Map.of("enabled", false)).toMap();
        int killed = whisperSvc.killAllActive();
        result.put("killedActive", killed);
        return result;
    }

    /** Resume rápido — atalho que faz PUT {enabled: true}. */
    @PostMapping("/api/voice/whisper/resume")
    public Map<String, Object> whisperResume() {
        return whisperConfig.update(java.util.Map.of("enabled", true)).toMap();
    }

    /**
     * Voice Map — clipes com posição em uma janela de tempo + dimensão.
     * Pra UI plotar pins num mapa 2D.
     * Filtros opcionais: fromTs, toTs, dimension, playerUuid.
     */
    @GetMapping("/api/voice/map")
    public Map<String, Object> voiceMap(
            @RequestParam(defaultValue = "0") long fromTs,
            @RequestParam(defaultValue = "0") long toTs,
            @RequestParam(required = false) String dimension,
            @RequestParam(required = false) String playerUuid,
            @RequestParam(defaultValue = "1000") int limit) {
        return svc.voiceMap(fromTs, toTs, dimension, playerUuid, limit);
    }

    /**
     * Espacial+Temporal — pega clipes PRÓXIMOS (radius blocos + Y minutos)
     * de um clipe-âncora. Retorna a "conversa" em volta dele.
     * Útil pra encontrar players que estavam juntos quando alguém falou algo.
     */
    @GetMapping("/api/voice/nearby")
    public Map<String, Object> nearby(
            @RequestParam long clipId,
            @RequestParam(defaultValue = "30") int radius,
            @RequestParam(defaultValue = "5") int timeMinutes) {
        return svc.findNearby(clipId, radius, timeMinutes);
    }

    /**
     * Clipes próximos da posição ATUAL de um player online.
     * Útil pra Voice Map "escutar quem fala perto" sem precisar clicar num
     * clipe-âncora. Refresca conforme player se move.
     */
    @GetMapping("/api/voice/around-player/{uuid}")
    public Map<String, Object> aroundPlayer(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "50") int radius,
            @RequestParam(defaultValue = "30") int minutes) {
        return svc.findAroundPlayer(uuid, radius, minutes, mod);
    }

    /**
     * Busca por TEXTO em transcrições. Encontra clipes que contêm a palavra
     * + retorna players agrupados pra UI mostrar "Steve disse 'XYZ' em 3 clipes".
     * Endpoint: GET /api/voice/search?q=texto&limit=200
     */
    @GetMapping("/api/voice/search")
    public Map<String, Object> searchTranscription(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "200") int limit) {
        return svc.searchTranscription(query, limit);
    }

    // =========================================================================
    // Retention settings — admin pode ligar/desligar cleanup + mudar dias
    // =========================================================================

    @GetMapping("/api/voice/retention")
    public Map<String, Object> getRetention() {
        var s = retentionSvc.get();
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("enabled", s.isEnabled());
        out.put("retentionDays", s.getRetentionDays());
        out.put("maxClipsPerPlayer", s.getMaxClipsPerPlayer());
        out.put("updatedBy", s.getUpdatedBy() == null ? "" : s.getUpdatedBy());
        out.put("updatedAt", s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString());
        out.put("nextRunCron", "0 0 5 * * * (todo dia às 5h)");
        return out;
    }

    public record RetentionUpdate(Boolean enabled, Integer retentionDays,
                                  Integer maxClipsPerPlayer, String updatedBy) {}

    @PutMapping("/api/voice/retention")
    public Map<String, Object> putRetention(@RequestBody RetentionUpdate body) {
        var s = retentionSvc.update(body.enabled(), body.retentionDays(),
                body.maxClipsPerPlayer(),
                body.updatedBy() == null ? "admin" : body.updatedBy());
        return Map.of(
                "ok", true,
                "enabled", s.isEnabled(),
                "retentionDays", s.getRetentionDays(),
                "maxClipsPerPlayer", s.getMaxClipsPerPlayer()
        );
    }

    /**
     * Aplica o limite por player AGORA (sem esperar o cleanup das 5h).
     * Aceita override do max via query param `max`; senão usa o do settings.
     */
    @PostMapping("/api/voice/retention/enforce-per-player")
    public Map<String, Object> enforcePerPlayer(@RequestParam(required = false) Integer max) {
        if (max != null && max > 0) {
            // Override pontual — atualiza temporariamente o setting pra aplicar
            // e retorna após. Mais simples: aplicar direto sem persistir.
            int removed = svc.enforceAllPerPlayerLimitsWithOverride(max);
            return Map.of("ok", true, "removed", removed, "maxUsed", max);
        }
        int removed = svc.enforceAllPerPlayerLimits();
        var s = retentionSvc.get();
        return Map.of("ok", true, "removed", removed, "maxUsed", s.getMaxClipsPerPlayer());
    }

    /**
     * Preview de quantos clipes serão deletados se rodar cleanup com X dias.
     * Não apaga nada — só conta.
     */
    @GetMapping("/api/voice/retention/preview")
    public Map<String, Object> previewRetention(@RequestParam(defaultValue = "14") int days) {
        return svc.previewCleanup(days);
    }

    /**
     * Roda o cleanup MANUALMENTE agora. Ignora flag enabled — quem clicou tem
     * intenção. Aceita override de days (opcional) ou usa o do settings.
     */
    @PostMapping("/api/voice/retention/run")
    public Map<String, Object> runRetention(@RequestParam(required = false) Integer days) {
        int removed = svc.runManualCleanup(days);
        return Map.of("ok", true, "removed", removed, "daysUsed", days == null ? -1 : days);
    }

    /**
     * Limpa transcrições antigas (v85-v87) que ficaram contaminadas com a
     * mensagem de erro de IO do whisper-cli ("open: failed to open '...' for
     * writing"). Itera todos os clipes com transcrição e remove o fragmento.
     * Retorna nº de clipes corrigidos.
     */
    @PostMapping("/api/voice/transcriptions/clean-io-errors")
    public Map<String, Object> cleanIoErrorTranscriptions() {
        int fixed = svc.cleanIoErrorFragmentsInTranscriptions();
        return Map.of("ok", true, "fixed", fixed,
                "note", "transcrições com 'open: failed to open ... for writing' foram limpas");
    }

    /**
     * Limpa orphan DB rows: clipes que nunca tiveram WAV uploaded (reserve
     * sem PUT audio) OU cujo arquivo sumiu do disco. Causava 404 quando user
     * tentava dar play num clipe na UI.
     */
    @PostMapping("/api/voice/clips/cleanup-orphans")
    public Map<String, Object> cleanupOrphans() {
        int removed = svc.cleanupOrphanClips();
        return Map.of("ok", true, "removed", removed);
    }

    /**
     * Força resetar TODOS os clipes em PROCESSING pra PENDING. Útil quando o
     * worker do whisper trava e fica acumulando clipes presos em PROCESSING.
     */
    @PostMapping("/api/voice/transcriptions/reset-processing")
    public Map<String, Object> resetProcessing() {
        int reset = svc.resetStuckProcessing();
        return Map.of("ok", true, "reset", reset);
    }

    // =========================================================================
    // MOD ↔ BACKEND endpoints (public path — but token-checked)
    // =========================================================================

    @PostMapping("/api/mod/voice/reserve")
    public ResponseEntity<?> modReserve(@RequestBody VoiceClip in, HttpServletRequest req) {
        if (!validateModToken(req)) return ResponseEntity.status(401).body(Map.of("error", "bad token"));
        VoiceClip clip = svc.reserve(in);
        return ResponseEntity.ok(Map.of(
                "id", clip.getId(),
                "ok", true
        ));
    }

    @PutMapping(value = "/api/mod/voice/clips/{id}/audio", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<?> modUpload(@PathVariable Long id, @RequestBody byte[] wav,
                                       HttpServletRequest req) {
        if (!validateModToken(req)) return ResponseEntity.status(401).body(Map.of("error", "bad token"));
        try {
            svc.storeAudio(id, wav);
            return ResponseEntity.ok(Map.of("ok", true, "id", id, "sizeBytes", wav.length));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Mod usa pra fazer playback in-game (precisa baixar o WAV pra re-encodar Opus). */
    @GetMapping("/api/mod/voice/clips/{id}/audio")
    public ResponseEntity<byte[]> modDownload(@PathVariable Long id, HttpServletRequest req) {
        if (!validateModToken(req)) return ResponseEntity.status(401).build();
        try {
            byte[] data = svc.readAudio(id);
            if (data == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean validateModToken(HttpServletRequest req) {
        String expected = registry.getToken();
        if (expected == null || expected.isBlank()) return true; // token disabled
        String got = req.getHeader("X-Liberthia-Token");
        return expected.equals(got);
    }

    // =========================================================================
    // BROWSER endpoints (Bearer auth via AuthFilter)
    // =========================================================================

    @GetMapping("/api/voice/clips")
    public Map<String, Object> list(
            @RequestParam(required = false) String playerUuid,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        List<VoiceClip> clips = svc.recent(playerUuid, limit);
        return Map.of("clips", clips, "count", clips.size());
    }

    /**
     * Voice Library: lista de players (como "pastas") com contagem de clipes,
     * duração total e bytes totais. UI mostra cards/grid; click → entra na pasta.
     *
     * v92: SEMPRE roda cleanup de orfãos antes (rows do DB cujos arquivos foram
     * deletados do disco mas o row ficou). Sem isso, voice library / rankings
     * mostravam contadores defasados — usuário via "200 clipes" e abria a
     * pasta com 150 reais.
     *
     * Cleanup é rápido (file-stat por row), throttled internamente pra rodar
     * no máx a cada 30s (vide VoiceClipService.cleanupOrphanClips). Chamadas
     * frequentes pelo front são absorvidas pelo throttle.
     */
    @GetMapping("/api/voice/library/players")
    public Map<String, Object> libraryPlayers(@RequestParam(defaultValue = "true") boolean fresh) {
        if (fresh) {
            try { svc.cleanupOrphanClips(); } catch (Exception ignored) {}
        }
        var rows = svc.playersWithClipCounts();
        return Map.of("players", rows, "count", rows.size());
    }

    /**
     * Proteção contra cleanup automática (preserva clipes especiais que admin
     * marcou — não vão ser apagados depois de 14 dias).
     */
    // BUG HISTÓRICO: esses 4 endpoints estavam em "/clips/..." sem o prefix
    // "/api/voice/" — o frontend chamava "/api/voice/clips/{id}/protect" mas
    // o backend só tinha "/clips/{id}/protect" registrado, daí o "404
    // endpoint_not_found" no painel. A classe não tem @RequestMapping no
    // topo, então cada method precisa do path completo.

    /**
     * Conversation mode: pega clipes de N players num range de tempo,
     * cronologicamente, pra reproduzir como "conversa" no painel.
     *
     * Query:
     *   playerUuids  — CSV de UUIDs (obrigatório)
     *   lastMinutes  — atalho: últimos N min até agora (default 60, ignora se fromTs setado)
     *   fromTs       — timestamp ms inferior
     *   toTs         — timestamp ms superior (0 = agora)
     *   limit        — max clipes (default 500, max 2000)
     */
    @GetMapping("/api/voice/sessions/conversation")
    public Map<String, Object> conversation(
            @RequestParam String playerUuids,
            @RequestParam(required = false, defaultValue = "60") int lastMinutes,
            @RequestParam(required = false, defaultValue = "0") long fromTs,
            @RequestParam(required = false, defaultValue = "0") long toTs,
            @RequestParam(required = false, defaultValue = "500") int limit) {
        var uuids = java.util.Arrays.stream(playerUuids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .toList();
        if (uuids.isEmpty()) return Map.of("clips", List.of(), "count", 0);

        long fromEff = fromTs > 0 ? fromTs
                : System.currentTimeMillis() - (long) Math.max(1, lastMinutes) * 60_000L;
        long toEff = toTs > 0 ? toTs : System.currentTimeMillis() + 60_000L;

        var clips = svc.conversation(uuids, fromEff, toEff, limit);
        long totalDuration = clips.stream().mapToLong(VoiceClip::getDurationMs).sum();
        long totalBytes = clips.stream().mapToLong(VoiceClip::getSizeBytes).sum();
        return Map.of(
                "clips", clips,
                "count", clips.size(),
                "fromTs", fromEff, "toTs", toEff,
                "totalDurationMs", totalDuration,
                "totalBytes", totalBytes,
                "playerUuids", uuids
        );
    }

    /**
     * Forçar (re)transcrição de um clipe específico. Marca como PENDING e o
     * scheduler do Whisper pega no próximo tick.
     */
    @PostMapping("/api/voice/clips/{id}/transcribe")
    public Map<String, Object> retranscribe(@PathVariable Long id) {
        svc.setTranscriptionStatus(id, "PENDING");
        return Map.of("ok", true, "status", "PENDING (vai ser processado no próximo tick do scheduler)");
    }

    /** Status agregado: quantos clipes em cada estado de transcrição. */
    @GetMapping("/api/voice/transcription/status")
    public Map<String, Object> transcriptionStatus() {
        return svc.transcriptionStats();
    }

    @PostMapping("/api/voice/clips/{id}/protect")
    public VoiceClip protectClip(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? "" : body.getOrDefault("reason", "");
        String by = body == null ? "" : body.getOrDefault("by", "");
        return svc.setProtection(id, true, reason, by);
    }

    @PostMapping("/api/voice/clips/{id}/unprotect")
    public VoiceClip unprotectClip(@PathVariable Long id) {
        return svc.setProtection(id, false, null, null);
    }

    /** Marca múltiplos clipes como protegidos/desprotegidos em uma chamada. */
    @PostMapping("/api/voice/clips/bulk-protect")
    public Map<String, Object> bulkProtect(@RequestBody Map<String, Object> body) {
        Object raw = body.get("ids");
        if (!(raw instanceof List<?> list)) return Map.of("ok", false, "updated", 0);
        boolean protect = body.get("protect") instanceof Boolean p ? p : true;
        String reason = body.get("reason") == null ? "" : body.get("reason").toString();
        String by = body.get("by") == null ? "" : body.get("by").toString();
        List<Long> ids = new java.util.ArrayList<>();
        for (Object o : list) if (o instanceof Number n) ids.add(n.longValue());
        int n = svc.setProtectionBulk(ids, protect, reason, by);
        return Map.of("ok", true, "updated", n);
    }

    /** Download em ZIP de todos os clipes de 1 player. */
    @GetMapping("/api/voice/clips/by-player/{playerUuid}/zip")
    public ResponseEntity<byte[]> zipPlayer(@PathVariable String playerUuid) {
        try {
            byte[] zip = svc.zipForPlayer(playerUuid);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"voice-clips-" + playerUuid.substring(0, 8) + ".zip\"")
                    .body(zip);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(("ERROR: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Voice Library: busca com filtros. Todos os filtros são opcionais
     * (0 = sem limite). Sort: recent|oldest|longest|shortest|biggest|smallest.
     */
    @GetMapping("/api/voice/library/search")
    public Map<String, Object> librarySearch(
            @RequestParam(required = false) String playerUuid,
            @RequestParam(required = false, defaultValue = "0") long minDurationMs,
            @RequestParam(required = false, defaultValue = "0") long maxDurationMs,
            @RequestParam(required = false, defaultValue = "0") long minBytes,
            @RequestParam(required = false, defaultValue = "0") long maxBytes,
            @RequestParam(required = false, defaultValue = "0") long fromTs,
            @RequestParam(required = false, defaultValue = "0") long toTs,
            @RequestParam(required = false, defaultValue = "recent") String sort,
            @RequestParam(required = false, defaultValue = "200") int limit) {
        var clips = svc.searchWithFilters(playerUuid, minDurationMs, maxDurationMs,
                minBytes, maxBytes, fromTs, toTs, sort, limit);
        return Map.of("clips", clips, "count", clips.size());
    }

    @GetMapping("/api/voice/clips/{id}")
    public ResponseEntity<VoiceClip> get(@PathVariable Long id) {
        VoiceClip c = svc.get(id);
        return c == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(c);
    }

    @GetMapping("/api/voice/clips/{id}/audio")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @RequestHeader(value = "Range", required = false) String range) {
        try {
            byte[] data = svc.readAudio(id);
            if (data == null) return ResponseEntity.notFound().build();

            // BUG FIX: <audio> em Chrome/Firefox manda Range header e SPERA
            // 206 Partial Content. Antes retornávamos 200 com body inteiro o
            // que fazia o player travar/não tocar em alguns browsers/versões.
            // Agora suporta Range: bytes=N-M corretamente.
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "audio/wav");
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CACHE_CONTROL, "max-age=3600");

            if (range == null || range.isBlank()) {
                headers.setContentLength(data.length);
                return new ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
            }
            // Parse "bytes=START-END" (END opcional)
            String spec = range.replace("bytes=", "").trim();
            String[] parts = spec.split("-", -1);
            long start = 0;
            long end = data.length - 1L;
            try {
                if (!parts[0].isBlank()) start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isBlank()) end = Long.parseLong(parts[1]);
            } catch (NumberFormatException nfe) {
                // Range malformado → devolve full 200
                headers.setContentLength(data.length);
                return new ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
            }
            if (start < 0) start = 0;
            if (end >= data.length) end = data.length - 1L;
            if (start > end) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + data.length).build();
            }
            int length = (int) (end - start + 1);
            byte[] slice = new byte[length];
            System.arraycopy(data, (int) start, slice, 0, length);
            headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + data.length);
            headers.setContentLength(length);
            return new ResponseEntity<>(slice, headers, org.springframework.http.HttpStatus.PARTIAL_CONTENT);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/api/voice/clips/{id}")
    public VoiceClip patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String trans = body.get("transcription") == null ? null : body.get("transcription").toString();
        String tags = body.get("tagsJson") == null ? null : body.get("tagsJson").toString();
        String lang = body.get("language") == null ? null : body.get("language").toString();
        return svc.updateMeta(id, trans, tags, lang);
    }

    @DeleteMapping("/api/voice/clips/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        svc.delete(id);
        return Map.of("ok", true);
    }

    /** Deleta múltiplos clipes em uma chamada. Body: {ids: [1,2,3]}. */
    @PostMapping("/api/voice/clips/bulk-delete")
    public Map<String, Object> bulkDelete(@RequestBody Map<String, Object> body) {
        Object raw = body.get("ids");
        if (!(raw instanceof List<?> rawList)) return Map.of("ok", false, "deleted", 0);
        List<Long> ids = new java.util.ArrayList<>();
        for (Object o : rawList) {
            if (o instanceof Number n) ids.add(n.longValue());
        }
        int deleted = svc.deleteBulk(ids);
        return Map.of("ok", true, "deleted", deleted);
    }

    /** Deleta TODOS os clipes de um player específico. */
    @DeleteMapping("/api/voice/clips/by-player/{playerUuid}")
    public Map<String, Object> deleteByPlayer(@PathVariable String playerUuid) {
        int deleted = svc.deleteByPlayer(playerUuid);
        return Map.of("ok", true, "deleted", deleted);
    }

    /**
     * Deleta TODOS os clipes do arquivo (purge global).
     * Exige header "X-Confirm-Purge: yes" pra evitar trigger acidental.
     */
    @DeleteMapping("/api/voice/clips/all")
    public Map<String, Object> deleteAll(
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Confirm-Purge", required = false) String confirm) {
        if (!"yes".equalsIgnoreCase(confirm)) {
            return Map.of("ok", false, "error", "Missing X-Confirm-Purge: yes header", "deleted", 0);
        }
        int deleted = svc.deleteAll();
        return Map.of("ok", true, "deleted", deleted);
    }

    /**
     * Varre o disco de voice clips e apaga arquivos órfãos (sem DB row).
     *
     * <p>Útil quando admin observa que o volume tá consumindo mais espaço do
     * que a UI mostra de clipes — sinal de que alguns deletes falharam parcial
     * (DB row apagado, mas WAV sobreviveu). Roda também automático em
     * {@link VoiceClipService#runCleanup} e {@link VoiceClipService#deleteAll}.
     *
     * <p>Retorna {@code {ok, filesDeleted}}.
     */
    @PostMapping("/api/voice/clips/cleanup-orphan-files")
    public Map<String, Object> cleanupOrphanFiles() {
        int filesDeleted = svc.cleanupOrphanFiles();
        return Map.of("ok", true, "filesDeleted", filesDeleted);
    }

    /** Toca o clipe no mundo, em coords + dim definidas. */
    @PostMapping("/api/voice/clips/{id}/play")
    public Map<String, Object> play(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        double x = ((Number) body.getOrDefault("x", 0)).doubleValue();
        double y = ((Number) body.getOrDefault("y", 64)).doubleValue();
        double z = ((Number) body.getOrDefault("z", 0)).doubleValue();
        String dim = body.getOrDefault("dimension", "minecraft:overworld").toString();
        float volume = ((Number) body.getOrDefault("volume", 1.0)).floatValue();
        String category = body.getOrDefault("category", "liberthia_voice").toString();
        boolean ok = svc.playInGame(id, x, y, z, dim, volume, category);
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", ok);
        return resp;
    }

    /**
     * Voice Modulator → tocar pra um player específico no jogo.
     */
    @PostMapping(value = "/api/voice/modulate-play", consumes = "multipart/form-data")
    public Map<String, Object> modulatePlay(
            @RequestParam("audio") org.springframework.web.multipart.MultipartFile audio,
            @RequestParam("playerUuid") String playerUuid,
            @RequestParam(required = false, defaultValue = "1.5") float volume,
            @RequestParam(required = false, defaultValue = "true") boolean deleteAfter) throws java.io.IOException {

        if (audio == null || audio.isEmpty()) {
            return Map.of("ok", false, "error", "audio file missing/empty");
        }
        if (playerUuid == null || playerUuid.isBlank()) {
            return Map.of("ok", false, "error", "playerUuid missing");
        }

        var modPlayers = mod.getPlayers();
        var playersNode = modPlayers == null ? null : modPlayers.path("players");
        com.fasterxml.jackson.databind.JsonNode target = null;
        if (playersNode != null && playersNode.isArray()) {
            for (var p : playersNode) {
                if (playerUuid.equalsIgnoreCase(p.path("uuid").asText(""))) {
                    target = p;
                    break;
                }
            }
        }
        if (target == null) {
            return Map.of("ok", false, "error", "player offline ou não encontrado: " + playerUuid);
        }
        double x = target.path("position").path("x").asDouble(0);
        double y = target.path("position").path("y").asDouble(64);
        double z = target.path("position").path("z").asDouble(0);
        String dim = target.path("dimension").asText("minecraft:overworld");
        String playerName = target.path("name").asText("?");

        Long clipId = saveModulatedClip(audio, playerUuid, "modulated:" + playerName, x, y, z, dim);
        if (clipId == null) return Map.of("ok", false, "error", "falha ao salvar audio");

        boolean played = svc.playInGame(clipId, x, y, z, dim, volume, "liberthia_voice");
        scheduleDeleteIfRequested(clipId, deleteAfter && played);

        return Map.of(
                "ok", played,
                "clipId", clipId,
                "playerName", playerName,
                "position", Map.of("x", x, "y", y, "z", z, "dim", dim),
                "volume", volume,
                "willDelete", deleteAfter
        );
    }

    /**
     * Toca áudio modulado em uma COORDENADA específica do mundo, independente
     * de player. Útil pra "speakers" em locais fixos (eventos, lore).
     */
    @PostMapping(value = "/api/voice/modulate-play-at", consumes = "multipart/form-data")
    public Map<String, Object> modulatePlayAt(
            @RequestParam("audio") org.springframework.web.multipart.MultipartFile audio,
            @RequestParam("x") double x,
            @RequestParam("y") double y,
            @RequestParam("z") double z,
            @RequestParam(required = false, defaultValue = "minecraft:overworld") String dimension,
            @RequestParam(required = false, defaultValue = "1.5") float volume,
            @RequestParam(required = false, defaultValue = "true") boolean deleteAfter) throws java.io.IOException {

        if (audio == null || audio.isEmpty()) {
            return Map.of("ok", false, "error", "audio file missing/empty");
        }

        Long clipId = saveModulatedClip(audio, null, "modulated:coord", x, y, z, dimension);
        if (clipId == null) return Map.of("ok", false, "error", "falha ao salvar audio");

        boolean played = svc.playInGame(clipId, x, y, z, dimension, volume, "liberthia_voice");
        scheduleDeleteIfRequested(clipId, deleteAfter && played);

        return Map.of(
                "ok", played,
                "clipId", clipId,
                "position", Map.of("x", x, "y", y, "z", z, "dim", dimension),
                "volume", volume
        );
    }

    /**
     * Toca o áudio pra TODOS os players online — uma reprodução por player,
     * cada uma na posição do respectivo player. Cada um escuta "do próprio
     * ouvido". Útil pra announcements lore-friendly.
     */
    @PostMapping(value = "/api/voice/modulate-play-all", consumes = "multipart/form-data")
    public Map<String, Object> modulatePlayAll(
            @RequestParam("audio") org.springframework.web.multipart.MultipartFile audio,
            @RequestParam(required = false, defaultValue = "1.5") float volume,
            @RequestParam(required = false, defaultValue = "true") boolean deleteAfter) throws java.io.IOException {

        if (audio == null || audio.isEmpty()) {
            return Map.of("ok", false, "error", "audio file missing/empty");
        }

        var modPlayers = mod.getPlayers();
        var playersNode = modPlayers == null ? null : modPlayers.path("players");
        if (playersNode == null || !playersNode.isArray() || playersNode.size() == 0) {
            return Map.of("ok", false, "error", "nenhum player online");
        }

        // Salva 1 vez só, depois toca N vezes (mesmo clip pra economizar disk)
        var firstPlayer = playersNode.get(0);
        double x = firstPlayer.path("position").path("x").asDouble(0);
        double y = firstPlayer.path("position").path("y").asDouble(64);
        double z = firstPlayer.path("position").path("z").asDouble(0);
        String dim = firstPlayer.path("dimension").asText("minecraft:overworld");
        Long clipId = saveModulatedClip(audio, null, "modulated:global", x, y, z, dim);
        if (clipId == null) return Map.of("ok", false, "error", "falha ao salvar audio");

        int played = 0;
        var perPlayer = new java.util.ArrayList<Map<String, Object>>();
        for (var p : playersNode) {
            double px = p.path("position").path("x").asDouble(0);
            double py = p.path("position").path("y").asDouble(64);
            double pz = p.path("position").path("z").asDouble(0);
            String pdim = p.path("dimension").asText("minecraft:overworld");
            String name = p.path("name").asText("?");
            boolean ok = svc.playInGame(clipId, px, py, pz, pdim, volume, "liberthia_voice");
            if (ok) played++;
            perPlayer.add(Map.of("name", name, "played", ok));
        }
        scheduleDeleteIfRequested(clipId, deleteAfter && played > 0);

        return Map.of(
                "ok", played > 0,
                "clipId", clipId,
                "playedCount", played,
                "totalPlayers", playersNode.size(),
                "perPlayer", perPlayer
        );
    }

    // ===== Helpers =====

    private Long saveModulatedClip(org.springframework.web.multipart.MultipartFile audio,
                                   String playerUuid, String playerName,
                                   double x, double y, double z, String dim) {
        try {
            VoiceClip in = new VoiceClip();
            in.setPlayerUuid(playerUuid == null ? java.util.UUID.randomUUID().toString() : playerUuid);
            in.setPlayerName(playerName);
            in.setTs(System.currentTimeMillis());
            in.setDurationMs(0);
            in.setSizeBytes(audio.getSize());
            in.setPosX(x); in.setPosY(y); in.setPosZ(z);
            in.setDimension(dim);
            VoiceClip created = svc.reserve(in);
            svc.storeAudio(created.getId(), audio.getBytes());
            return created.getId();
        } catch (Exception e) {
            return null;
        }
    }

    private void scheduleDeleteIfRequested(Long clipId, boolean shouldDelete) {
        if (!shouldDelete) return;
        new Thread(() -> {
            try {
                Thread.sleep(60_000);
                svc.delete(clipId);
            } catch (Exception ignored) {}
        }, "modulator-cleanup-" + clipId).start();
    }
}
