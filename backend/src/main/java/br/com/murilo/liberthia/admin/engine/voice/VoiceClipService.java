package br.com.murilo.liberthia.admin.engine.voice;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
interface VoiceClipRepository extends JpaRepository<VoiceClip, Long> {
    /**
     * Lista recente de clipes. Filtra uploadStatus='DONE' pra não mostrar
     * orphan reserves (DB row sem WAV uploaded), que causaria 404 quando user
     * tenta dar play.
     *
     * Antes não tinha esse filtro — usuário via clips na lista mas o áudio
     * voltava 404 porque o mod nunca terminou o upload.
     */
    @Query("SELECT c FROM VoiceClip c WHERE (:uuid IS NULL OR c.playerUuid = :uuid) " +
           "AND c.uploadStatus = 'DONE' AND c.filePath IS NOT NULL " +
           "ORDER BY c.ts DESC")
    List<VoiceClip> findRecent(@Param("uuid") String uuid, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT c FROM VoiceClip c WHERE c.uploadStatus = 'PENDING' AND c.createdAt < :cutoff")
    List<VoiceClip> findOrphans(@Param("cutoff") Instant cutoff);

    /** Cleanup respeita protection — só apaga não-protegidos. */
    @Query("SELECT c FROM VoiceClip c WHERE c.ts < :cutoff AND c.protectedFromCleanup = false")
    List<VoiceClip> findExpiredUnprotected(@Param("cutoff") long cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM VoiceClip c WHERE c.ts < :cutoff AND c.protectedFromCleanup = false")
    int deleteOlderThanUnprotected(@Param("cutoff") long cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM VoiceClip c WHERE c.ts < :cutoff")
    int deleteOlderThan(@Param("cutoff") long cutoff);

    @Query("SELECT c FROM VoiceClip c WHERE c.playerUuid = :uuid AND c.uploadStatus = 'DONE' ORDER BY c.ts DESC")
    List<VoiceClip> findAllByPlayer(@Param("uuid") String uuid);

    /**
     * Clipes prontos pra transcrição: uploadStatus=DONE + transcriptionStatus=PENDING.
     *
     * ORDEM: ts DESC explícito = mais RECENTE primeiro (LIFO).
     * Razão: admin quer ouvir conversas do "agora" transcritas primeiro.
     * Backlog antigo vai sendo processado conforme tempo livre.
     *
     * BUG FIX v91: garantir filePath IS NOT NULL pra não tentar processar
     * orphan reserves (clip sem WAV — whisper-cli falha com "file not found"
     * e marca FAILED, poluindo a fila).
     */
    @Query("SELECT c FROM VoiceClip c WHERE c.uploadStatus = 'DONE' " +
           "AND c.filePath IS NOT NULL " +
           "AND (c.transcriptionStatus IS NULL OR c.transcriptionStatus = 'PENDING') " +
           "ORDER BY c.ts DESC")
    List<VoiceClip> findPendingTranscription(org.springframework.data.domain.Pageable pageable);

    long countByTranscriptionStatus(String status);
    long countByTranscriptionStatusIsNull();

    /**
     * Conversation mode: pega clipes de múltiplos players num range de tempo
     * ordenado cronologicamente. Usado pra "escutar a conversa" entre N players
     * dos últimos N minutos.
     */
    @Query("SELECT c FROM VoiceClip c WHERE c.uploadStatus = 'DONE' " +
           "AND c.playerUuid IN :uuids " +
           "AND c.ts >= :fromTs " +
           "AND c.ts <= :toTs " +
           "ORDER BY c.ts ASC")
    List<VoiceClip> findConversation(@Param("uuids") List<String> uuids,
                                     @Param("fromTs") long fromTs,
                                     @Param("toTs") long toTs,
                                     org.springframework.data.domain.Pageable pageable);

    /**
     * Agregação por player — usado pela Voice Library na grid inicial.
     * Retorna Object[] em ordem: uuid, name, count, totalDuration, totalBytes, firstTs, lastTs.
     * Só conta clipes com upload concluído.
     */
    @Query("SELECT c.playerUuid, MAX(c.playerName), COUNT(c), " +
           "       COALESCE(SUM(c.durationMs), 0), COALESCE(SUM(c.sizeBytes), 0), " +
           "       MIN(c.ts), MAX(c.ts) " +
           "FROM VoiceClip c " +
           "WHERE c.uploadStatus = 'DONE' AND c.playerUuid IS NOT NULL " +
           "GROUP BY c.playerUuid " +
           "ORDER BY MAX(c.ts) DESC")
    List<Object[]> aggregateByPlayer();

    /**
     * Busca com filtros e ordenação — Voice Library quando entra na pasta do player.
     * Parâmetros null/0 são ignorados nas comparações.
     */
    @Query("SELECT c FROM VoiceClip c WHERE " +
           "  (:uuid IS NULL OR c.playerUuid = :uuid) AND " +
           "  c.uploadStatus = 'DONE' AND " +
           "  (:minDur = 0 OR c.durationMs >= :minDur) AND " +
           "  (:maxDur = 0 OR c.durationMs <= :maxDur) AND " +
           "  (:minSize = 0 OR c.sizeBytes >= :minSize) AND " +
           "  (:maxSize = 0 OR c.sizeBytes <= :maxSize) AND " +
           "  (:fromTs = 0 OR c.ts >= :fromTs) AND " +
           "  (:toTs = 0 OR c.ts <= :toTs)")
    List<VoiceClip> search(@Param("uuid") String uuid,
                           @Param("minDur") long minDur, @Param("maxDur") long maxDur,
                           @Param("minSize") long minSize, @Param("maxSize") long maxSize,
                           @Param("fromTs") long fromTs, @Param("toTs") long toTs,
                           org.springframework.data.domain.Pageable pageable);

    /**
     * Busca por TEXTO na transcrição. Case-insensitive substring match.
     * Retorna clipes que contêm o texto + dados do player (uuid+name).
     * Usada pela Voice Library na busca "encontrar conversa com tal palavra".
     */
    @Query("SELECT c FROM VoiceClip c WHERE " +
           "  c.uploadStatus = 'DONE' AND " +
           "  c.transcriptionStatus = 'DONE' AND " +
           "  c.transcription IS NOT NULL AND " +
           "  LOWER(c.transcription) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY c.ts DESC")
    List<VoiceClip> searchByTranscription(@Param("query") String query,
                                          org.springframework.data.domain.Pageable pageable);
}

/**
 * Service de clipes de voz: orquestra storage em disco + DB + playback.
 *
 * Arquivos são salvos em data/voice-clips/yyyy-MM/<id>.wav — fora do DB pra
 * não bloatar o Postgres com binários. Cleanup automático após N dias.
 */
@Service
public class VoiceClipService {

    private static final Logger LOG = LoggerFactory.getLogger(VoiceClipService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final VoiceClipRepository repo;
    private final ModBridgeClient mod;

    @Value("${voice.storage-dir:data/voice-clips}")
    private String storageDir;

    /**
     * Retenção padrão reduzida pra 14 dias. Antes era 90 — ficou inviável
     * em volume alto. Clipes marcados como protectedFromCleanup ignoram isso.
     * Ajustável via env VOICE_RETENTION_DAYS.
     */
    @Value("${voice.retention-days:14}")
    private int retentionDays;

    private final VoiceRetentionService retention;

    public VoiceClipService(VoiceClipRepository repo, ModBridgeClient mod,
                            VoiceRetentionService retention) {
        this.repo = repo;
        this.mod = mod;
        this.retention = retention;
    }

    public VoiceClip reserve(VoiceClip in) {
        VoiceClip clip = new VoiceClip();
        clip.setPlayerUuid(in.getPlayerUuid());
        clip.setPlayerName(in.getPlayerName());
        clip.setTs(in.getTs());
        clip.setDurationMs(in.getDurationMs());
        clip.setPosX(in.getPosX());
        clip.setPosY(in.getPosY());
        clip.setPosZ(in.getPosZ());
        clip.setDimension(in.getDimension());
        clip.setSizeBytes(in.getSizeBytes()); // sized esperado, será sobrescrito no PUT
        clip.setUploadStatus("PENDING");
        return repo.save(clip);
    }

    @Transactional
    public VoiceClip storeAudio(Long clipId, byte[] wavBytes) throws IOException {
        VoiceClip clip = repo.findById(clipId).orElse(null);
        if (clip == null) throw new IllegalArgumentException("clip not found: " + clipId);

        // data/voice-clips/2026-05/123.wav
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        String monthDir = date.format(MONTH_FMT);
        Path dir = Paths.get(storageDir, monthDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.error("[Voice] mkdir falhou em {} — checa permissão do volume Docker (owner liberthia). Err: {}",
                    dir.toAbsolutePath(), e.toString());
            throw new IOException("mkdir failed at " + dir.toAbsolutePath() + " (permission denied? volume owner?)", e);
        }
        Path file = dir.resolve(clipId + ".wav");
        try {
            Files.write(file, wavBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOG.error("[Voice] write falhou em {}: {}", file.toAbsolutePath(), e.toString());
            throw new IOException("write failed at " + file.toAbsolutePath(), e);
        }

        String relPath = monthDir + "/" + clipId + ".wav";
        clip.setFilePath(relPath);
        clip.setSizeBytes(wavBytes.length);
        clip.setUploadStatus("DONE");
        VoiceClip saved = repo.save(clip);

        // Aplica limite por player (FIFO drop dos mais antigos não-protegidos)
        // de forma assíncrona — não bloqueia o upload se enforcement falhar.
        try {
            VoiceRetentionSettings s = retention.get();
            if (s.getMaxClipsPerPlayer() > 0 && saved.getPlayerUuid() != null) {
                int removed = enforcePerPlayerLimit(saved.getPlayerUuid(), s.getMaxClipsPerPlayer());
                if (removed > 0) {
                    LOG.info("[Voice] enforcePerPlayerLimit({}): removeu {} clipe(s) antigo(s) pra respeitar limite de {}",
                            saved.getPlayerUuid(), removed, s.getMaxClipsPerPlayer());
                }
            }
        } catch (Exception e) {
            LOG.warn("[Voice] enforce limit pós-upload falhou: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Mantém apenas os `max` clipes MAIS RECENTES de um player. Clipes
     * protegidos (protectedFromCleanup=true) sempre são preservados — não
     * contam pro limite. Retorna nº de clipes deletados.
     *
     * Estratégia: ordena por ts DESC, salta os primeiros `max` não-protegidos,
     * deleta o resto. Se o player tem 600 clipes e max=500 → deleta os 100
     * mais antigos não-protegidos.
     */
    /**
     * Mantém apenas os `max` clipes MAIS RECENTES de um player.
     *
     * IMPORTANTE — DIREÇÃO DO FIFO:
     *   - findAllByPlayer ordena por ts DESC (mais novo PRIMEIRO).
     *   - Iteramos do mais NOVO pro mais ANTIGO.
     *   - Mantemos os primeiros `max` (= os mais NOVOS).
     *   - Deletamos o resto (= os mais ANTIGOS).
     *
     * Protegidos (⭐) NUNCA contam pro limite NEM são deletados.
     *
     * Exemplo: player tem 1200 clipes (5 protegidos), max=1000:
     *   - Pulamos os 5 protegidos (continuam)
     *   - Mantemos os 1000 mais novos não-protegidos
     *   - Deletamos os 195 mais antigos não-protegidos
     *   → final: 1005 clipes (1000 não-protegidos + 5 protegidos)
     *
     * Retorna nº de clipes deletados.
     */
    @Transactional
    public int enforcePerPlayerLimit(String playerUuid, int max) {
        if (playerUuid == null || playerUuid.isBlank() || max <= 0) return 0;
        // findAllByPlayer = ORDER BY ts DESC → mais novo (índice 0) ... mais antigo (último)
        List<VoiceClip> all = repo.findAllByPlayer(playerUuid);
        if (all.isEmpty()) return 0;

        int kept = 0;
        int removed = 0;
        long oldestKeptTs = Long.MAX_VALUE;
        long newestDeletedTs = 0;

        for (VoiceClip c : all) {
            if (c.isProtectedFromCleanup()) continue; // ⭐ — sempre fica
            if (kept < max) {
                kept++;
                if (c.getTs() < oldestKeptTs) oldestKeptTs = c.getTs();
                continue;
            }
            // Aqui chegamos só nos clipes ALÉM do max — necessariamente mais
            // antigos que tudo que foi mantido (pq lista tá DESC).
            try {
                // SANITY CHECK: se algum dia a ordem da query mudar e isso
                // tentar deletar um clipe MAIS NOVO que algum mantido, abort.
                if (c.getTs() > oldestKeptTs) {
                    LOG.error("[Voice] FIFO INVERTIDO! clip {} ts={} mais novo que oldestKept={}. ABORTANDO pra não perder dados novos.",
                            c.getId(), c.getTs(), oldestKeptTs);
                    return removed;
                }
                if (c.getTs() > newestDeletedTs) newestDeletedTs = c.getTs();
                delete(c.getId());
                removed++;
            } catch (Exception ignored) {}
        }
        if (removed > 0) {
            LOG.info("[Voice] enforcePerPlayerLimit({}, max={}): mantidos={} (oldestKept ts={}), removidos={} (newestDeleted ts={})",
                    playerUuid, max, kept, oldestKeptTs, removed, newestDeletedTs);
        }
        return removed;
    }

    /**
     * Roda enforcePerPlayerLimit pra TODOS os players com clipes no DB.
     * Útil pra rodada manual quando o admin acabou de ajustar o limite.
     * Retorna total de clipes removidos somando todos os players.
     */
    @Transactional
    public int enforceAllPerPlayerLimits() {
        VoiceRetentionSettings s = retention.get();
        int max = s.getMaxClipsPerPlayer();
        if (max <= 0) {
            LOG.info("[Voice] enforceAllPerPlayerLimits: limite=0 (ilimitado), nada a fazer");
            return 0;
        }
        return enforceAllPerPlayerLimitsWithOverride(max);
    }

    /**
     * Como enforceAllPerPlayerLimits, mas usa o `max` passado ao invés de
     * ler do settings. Não persiste a mudança — útil pra preview ou pra
     * o painel rodar com um valor explícito sem alterar o salvo.
     */
    @Transactional
    public int enforceAllPerPlayerLimitsWithOverride(int max) {
        if (max <= 0) return 0;
        List<Object[]> agg = repo.aggregateByPlayer();
        int total = 0;
        for (Object[] row : agg) {
            String uuid = (String) row[0];
            if (uuid == null) continue;
            total += enforcePerPlayerLimit(uuid, max);
        }
        LOG.info("[Voice] enforceAllPerPlayerLimits(max={}): removeu {} clipes totais", max, total);
        return total;
    }

    public byte[] readAudio(Long clipId) throws IOException {
        VoiceClip clip = repo.findById(clipId).orElse(null);
        if (clip == null) {
            LOG.debug("[Voice] readAudio({}): clipe não existe no DB", clipId);
            return null;
        }
        if (clip.getFilePath() == null) {
            // Orphan: row existe mas nunca recebeu o WAV. O mod faz "reserve"
            // pra criar o ID, mas se o upload PUT do WAV falhar (rede, container
            // reiniciou, mod travou) fica esse DB row órfão. Auto-limpa.
            LOG.warn("[Voice] readAudio({}): clip sem filePath (upload incompleto) — deletando row órfão", clipId);
            try { repo.deleteById(clipId); } catch (Exception ignored) {}
            return null;
        }
        Path file = Paths.get(storageDir, clip.getFilePath());
        if (!Files.exists(file)) {
            // Sumiu do disco mas DB ainda tem ref. Pode ser:
            //  - container redeployed com volume diferente
            //  - cleanup deletou o arquivo mas row sobreviveu (bug raro)
            //  - permissão errada do volume
            // Em qualquer caso, é dado fantasma — limpa o row pra não polluir UI.
            LOG.warn("[Voice] readAudio({}): file path={} não existe no disco — deletando row fantasma",
                    clipId, file.toAbsolutePath());
            try { repo.deleteById(clipId); } catch (Exception ignored) {}
            return null;
        }
        return Files.readAllBytes(file);
    }

    public VoiceClip get(Long id) { return repo.findById(id).orElse(null); }

    public List<VoiceClip> recent(String playerUuid, int limit) {
        return repo.findRecent(
                playerUuid == null || playerUuid.isBlank() ? null : playerUuid,
                org.springframework.data.domain.PageRequest.of(0, Math.min(500, Math.max(1, limit))));
    }

    /** Resumo agregado por player — usado pela Voice Library na tela inicial. */
    public List<java.util.Map<String, Object>> playersWithClipCounts() {
        List<Object[]> rows = repo.aggregateByPlayer();
        List<java.util.Map<String, Object>> out = new java.util.ArrayList<>(rows.size());
        for (Object[] r : rows) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("playerUuid", r[0]);
            m.put("playerName", r[1]);
            m.put("clipCount", ((Number) r[2]).longValue());
            m.put("totalDurationMs", r[3] == null ? 0L : ((Number) r[3]).longValue());
            m.put("totalBytes", r[4] == null ? 0L : ((Number) r[4]).longValue());
            m.put("firstClipTs", r[5] == null ? 0L : ((Number) r[5]).longValue());
            m.put("lastClipTs", r[6] == null ? 0L : ((Number) r[6]).longValue());
            out.add(m);
        }
        return out;
    }

    /**
     * Busca com filtros e ordenação. Sort: recent|oldest|longest|shortest|biggest|smallest.
     * Filtros zerados = sem filtro.
     */
    public List<VoiceClip> searchWithFilters(String playerUuid, long minDur, long maxDur,
                                             long minSize, long maxSize, long fromTs, long toTs,
                                             String sort, int limit) {
        org.springframework.data.domain.Sort s = switch (sort == null ? "recent" : sort) {
            case "oldest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("ts"));
            case "longest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("durationMs"));
            case "shortest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("durationMs"));
            case "biggest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("sizeBytes"));
            case "smallest" -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("sizeBytes"));
            default -> org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("ts"));
        };
        var pg = org.springframework.data.domain.PageRequest.of(
                0, Math.min(1000, Math.max(1, limit)), s);
        return repo.search(
                playerUuid == null || playerUuid.isBlank() ? null : playerUuid,
                Math.max(0, minDur), Math.max(0, maxDur),
                Math.max(0, minSize), Math.max(0, maxSize),
                Math.max(0, fromTs), Math.max(0, toTs),
                pg);
    }

    @Transactional
    public VoiceClip updateMeta(Long id, String transcription, String tagsJson, String language) {
        VoiceClip c = repo.findById(id).orElseThrow();
        if (transcription != null) {
            c.setTranscription(transcription);
            c.setTranscriptionStatus("DONE");
        }
        if (tagsJson != null) c.setTagsJson(tagsJson);
        if (language != null) c.setLanguage(language);
        return repo.save(c);
    }

    /**
     * Conversation mode — clipes de N players num range de tempo, em ordem
     * cronológica. Pra "ouvir a conversa" entre eles sequencialmente.
     *
     * Args:
     *   uuids  — lista de player UUIDs (pelo menos 1)
     *   fromTs — timestamp ms (epoch). 0 = sem limite inferior.
     *   toTs   — timestamp ms (epoch). 0 = agora.
     *   limit  — max retornado (default 500, max 2000)
     */
    public List<VoiceClip> conversation(List<String> uuids, long fromTs, long toTs, int limit) {
        if (uuids == null || uuids.isEmpty()) return List.of();
        long fromEff = fromTs <= 0 ? 0 : fromTs;
        long toEff = toTs <= 0 ? System.currentTimeMillis() + 60_000 : toTs;
        return repo.findConversation(uuids, fromEff, toEff,
                org.springframework.data.domain.PageRequest.of(0,
                        Math.max(1, Math.min(2000, limit))));
    }

    /**
     * Busca clipes por texto na transcrição. Retorna lista de clipes + agrupa
     * por player (uuid → lista de clipes com hit). Pra UI mostrar "tal jogador
     * disse XYZ em N clipes".
     */
    public java.util.Map<String, Object> searchTranscription(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            return java.util.Map.of("clips", List.of(), "players", List.of(), "count", 0);
        }
        var clips = repo.searchByTranscription(query.trim(),
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(500, limit))));

        // Agrupa por player UUID
        java.util.Map<String, java.util.List<VoiceClip>> byPlayer = new java.util.LinkedHashMap<>();
        for (VoiceClip c : clips) {
            byPlayer.computeIfAbsent(c.getPlayerUuid(), k -> new java.util.ArrayList<>()).add(c);
        }
        java.util.List<java.util.Map<String, Object>> players = new java.util.ArrayList<>();
        for (var entry : byPlayer.entrySet()) {
            var list = entry.getValue();
            var first = list.get(0);
            var m = new java.util.LinkedHashMap<String, Object>();
            m.put("playerUuid", entry.getKey());
            m.put("playerName", first.getPlayerName());
            m.put("matchCount", list.size());
            m.put("clipIds", list.stream().map(VoiceClip::getId).toList());
            m.put("latestTs", first.getTs());
            players.add(m);
        }
        // Ordena por mais matches primeiro
        players.sort((a, b) -> Integer.compare(
                ((Number) b.get("matchCount")).intValue(),
                ((Number) a.get("matchCount")).intValue()));

        return java.util.Map.of(
                "clips", clips,
                "players", players,
                "count", clips.size(),
                "query", query
        );
    }

    /**
     * Voice Map — clipes com posição num range de tempo + dimensão +
     * PLAYERS ONLINE no servidor (via mod bridge, com posição em tempo real).
     */
    public java.util.Map<String, Object> voiceMap(long fromTs, long toTs, String dimension,
                                                  String playerUuid, int limit) {
        long fromEff = fromTs > 0 ? fromTs : 0;
        long toEff = toTs > 0 ? toTs : System.currentTimeMillis() + 60_000;
        var pageable = org.springframework.data.domain.PageRequest.of(
                0, Math.max(1, Math.min(2000, limit)));
        var allClips = repo.search(
                playerUuid != null && !playerUuid.isBlank() ? playerUuid : null,
                0, 0, 0, 0, fromEff, toEff, pageable);

        // Filtra clipes por dimension + tem posição
        var withPos = new java.util.ArrayList<VoiceClip>();
        var withoutPos = new java.util.ArrayList<VoiceClip>();
        java.util.Set<String> dims = new java.util.HashSet<>();
        java.util.Set<String> players = new java.util.HashSet<>();
        for (VoiceClip c : allClips) {
            if (c.getDimension() != null) dims.add(c.getDimension());
            if (c.getPlayerUuid() != null) players.add(c.getPlayerUuid());
            if (c.getPosX() == null || c.getPosZ() == null) {
                withoutPos.add(c);
                continue;
            }
            if (dimension != null && !dimension.isBlank() && !dimension.equals(c.getDimension())) continue;
            withPos.add(c);
        }

        // === ONLINE PLAYERS via mod bridge ===
        var onlinePlayers = new java.util.ArrayList<java.util.Map<String, Object>>();
        try {
            var resp = mod.getPlayers();
            var arr = resp == null ? null : resp.path("players");
            if (arr != null && arr.isArray()) {
                for (var p : arr) {
                    String dim = p.path("dimension").asText("");
                    if (dimension != null && !dimension.isBlank() && !dimension.equals(dim)) continue;
                    double x = p.path("position").path("x").asDouble(0);
                    double z = p.path("position").path("z").asDouble(0);
                    var m = new java.util.LinkedHashMap<String, Object>();
                    m.put("uuid", p.path("uuid").asText(""));
                    m.put("name", p.path("name").asText("?"));
                    m.put("posX", x);
                    m.put("posY", p.path("position").path("y").asDouble(64));
                    m.put("posZ", z);
                    m.put("dimension", dim);
                    m.put("health", p.path("health").asDouble(20));
                    onlinePlayers.add(m);
                }
            }
        } catch (Exception e) {
            LOG.warn("[VoiceMap] erro lendo online players: {}", e.getMessage());
        }

        // === NEARBY COUNT — pra cada player online, quantos outros estão num raio ===
        // Default 50 blocos. Útil pra mostrar agrupamentos de players (eventos,
        // hangouts, bases compartilhadas).
        final double NEARBY_RADIUS = 50;
        for (var p : onlinePlayers) {
            double px = ((Number) p.get("posX")).doubleValue();
            double pz = ((Number) p.get("posZ")).doubleValue();
            String pdim = (String) p.get("dimension");
            int near = 0;
            java.util.List<String> nearNames = new java.util.ArrayList<>();
            for (var other : onlinePlayers) {
                if (other == p) continue;
                if (!pdim.equals(other.get("dimension"))) continue;
                double dx = ((Number) other.get("posX")).doubleValue() - px;
                double dz = ((Number) other.get("posZ")).doubleValue() - pz;
                if (dx * dx + dz * dz <= NEARBY_RADIUS * NEARBY_RADIUS) {
                    near++;
                    nearNames.add((String) other.get("name"));
                }
            }
            p.put("nearbyCount", near);
            p.put("nearbyNames", nearNames);
        }

        // Bounds — inclui clipes COM pos + players online (assim viewport
        // acomoda jogadores ao vivo se nenhum clipe tem posição no range).
        Double minX = null, maxX = null, minZ = null, maxZ = null;
        for (VoiceClip c : withPos) {
            if (minX == null || c.getPosX() < minX) minX = c.getPosX();
            if (maxX == null || c.getPosX() > maxX) maxX = c.getPosX();
            if (minZ == null || c.getPosZ() < minZ) minZ = c.getPosZ();
            if (maxZ == null || c.getPosZ() > maxZ) maxZ = c.getPosZ();
        }
        for (var p : onlinePlayers) {
            double x = ((Number) p.get("posX")).doubleValue();
            double z = ((Number) p.get("posZ")).doubleValue();
            if (minX == null || x < minX) minX = x;
            if (maxX == null || x > maxX) maxX = x;
            if (minZ == null || z < minZ) minZ = z;
            if (maxZ == null || z > maxZ) maxZ = z;
        }
        // Fallback se ainda null: spawn ±200
        if (minX == null) { minX = -200.0; maxX = 200.0; minZ = -200.0; maxZ = 200.0; }
        // Padding mínimo de 100 blocos pra mapa não ficar absurdamente zoomed
        if (maxX - minX < 100) { double mid = (maxX + minX) / 2; minX = mid - 50; maxX = mid + 50; }
        if (maxZ - minZ < 100) { double mid = (maxZ + minZ) / 2; minZ = mid - 50; maxZ = mid + 50; }

        return java.util.Map.ofEntries(
                java.util.Map.entry("clips", withPos),
                java.util.Map.entry("clipsWithoutPos", withoutPos),
                java.util.Map.entry("onlinePlayers", onlinePlayers),
                java.util.Map.entry("count", withPos.size()),
                java.util.Map.entry("fromTs", fromEff),
                java.util.Map.entry("toTs", toEff),
                java.util.Map.entry("distinctPlayers", players.size()),
                java.util.Map.entry("dimensions", dims),
                java.util.Map.entry("bounds", java.util.Map.of(
                        "minX", minX, "maxX", maxX,
                        "minZ", minZ, "maxZ", maxZ
                ))
        );
    }

    /**
     * Clipes próximos da posição ATUAL de um player online. Usa mod bridge
     * pra pegar posição em tempo real e busca clipes em raio + janela de
     * tempo. Diferente de findNearby (que usa clipe como anchor), esse usa
     * a posição AO VIVO do player — atualiza conforme ele se move.
     */
    public java.util.Map<String, Object> findAroundPlayer(String uuid, int radius,
                                                          int minutes, ModBridgeClient modBridge) {
        try {
            var resp = modBridge.getPlayers();
            var players = resp == null ? null : resp.path("players");
            com.fasterxml.jackson.databind.JsonNode target = null;
            if (players != null && players.isArray()) {
                for (var p : players) {
                    if (uuid.equalsIgnoreCase(p.path("uuid").asText(""))) {
                        target = p;
                        break;
                    }
                }
            }
            if (target == null) {
                return java.util.Map.of("error", "player offline ou não encontrado", "clips", java.util.List.of());
            }
            double px = target.path("position").path("x").asDouble(0);
            double pz = target.path("position").path("z").asDouble(0);
            String pdim = target.path("dimension").asText("minecraft:overworld");

            // Busca clipes no range de tempo
            long fromTs = System.currentTimeMillis() - (long) Math.max(1, minutes) * 60_000L;
            var pageable = org.springframework.data.domain.PageRequest.of(0, 500);
            var candidates = repo.search(null, 0, 0, 0, 0, fromTs, System.currentTimeMillis() + 60_000L, pageable);

            // Filtra por distância + dimensão
            var result = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (VoiceClip c : candidates) {
                if (c.getPosX() == null || c.getPosZ() == null) continue;
                if (!pdim.equals(c.getDimension())) continue;
                double dx = c.getPosX() - px, dz = c.getPosZ() - pz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;
                var m = new java.util.LinkedHashMap<String, Object>();
                m.put("clip", c);
                m.put("distance", Math.round(dist * 10.0) / 10.0);
                result.add(m);
            }
            // Sort por mais recente primeiro
            result.sort((a, b) -> Long.compare(
                    ((VoiceClip) b.get("clip")).getTs(),
                    ((VoiceClip) a.get("clip")).getTs()));

            return java.util.Map.of(
                    "playerName", target.path("name").asText("?"),
                    "playerUuid", uuid,
                    "playerPos", java.util.Map.of("x", px, "z", pz, "dim", pdim),
                    "radius", radius,
                    "minutes", minutes,
                    "clips", result,
                    "count", result.size()
            );
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage(), "clips", java.util.List.of());
        }
    }

    /**
     * Acha clipes próximos no tempo+espaço de um clipe-âncora.
     * Retorna players que estavam por perto na hora da fala = potenciais
     * participantes da conversa.
     */
    public java.util.Map<String, Object> findNearby(long clipId, int radiusBlocks, int timeMinutes) {
        VoiceClip anchor = repo.findById(clipId).orElse(null);
        if (anchor == null) return java.util.Map.of("error", "anchor clip not found");
        if (anchor.getPosX() == null || anchor.getPosZ() == null) {
            return java.util.Map.of("error", "anchor clip has no position");
        }

        long ts = anchor.getTs();
        long fromTs = ts - (long) timeMinutes * 60_000L;
        long toTs = ts + (long) timeMinutes * 60_000L;

        // Busca todos no range de tempo + dimensão
        var pageable = org.springframework.data.domain.PageRequest.of(0, 500);
        var candidates = repo.search(null, 0, 0, 0, 0, fromTs, toTs, pageable);

        // Filtra por distância euclidiana 2D (X,Z) + mesma dimensão
        double ax = anchor.getPosX(), az = anchor.getPosZ();
        String dim = anchor.getDimension();
        var nearby = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (VoiceClip c : candidates) {
            if (c.getId().equals(clipId)) continue;
            if (c.getPosX() == null || c.getPosZ() == null) continue;
            if (dim != null && !dim.equals(c.getDimension())) continue;
            double dx = c.getPosX() - ax, dz = c.getPosZ() - az;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radiusBlocks) continue;
            var m = new java.util.LinkedHashMap<String, Object>();
            m.put("clip", c);
            m.put("distance", Math.round(dist * 10.0) / 10.0);
            m.put("timeDeltaMs", c.getTs() - ts);
            nearby.add(m);
        }
        nearby.sort((a, b) -> Long.compare(
                ((VoiceClip) a.get("clip")).getTs(),
                ((VoiceClip) b.get("clip")).getTs()));

        return java.util.Map.of(
                "anchor", anchor,
                "nearby", nearby,
                "count", nearby.size(),
                "radius", radiusBlocks,
                "timeMinutes", timeMinutes
        );
    }

    // ========== Whisper transcription helpers ==========

    /** Lista clipes PENDING de transcrição (limit pra evitar batch grandes). */
    public List<VoiceClip> findPendingTranscription(int limit) {
        return repo.findPendingTranscription(
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(100, limit))));
    }

    /**
     * Throttle pra cleanup — evita rodar varredura completa em chamadas
     * sucessivas (ex: front faz 3 requests em 1s ao abrir voice library).
     * Roda no máx 1× a cada 30s; chamadas dentro da janela retornam 0.
     */
    private volatile long lastCleanupTs = 0L;
    private static final long CLEANUP_THROTTLE_MS = 30_000L;

    /**
     * Limpeza proativa: encontra rows em DB cujo arquivo WAV não existe no
     * disco e deleta. Roda a cada 30min via @Scheduled (registrado no
     * controller).
     *
     * Cobre:
     *  - Orphan reserves (sem filePath)
     *  - Clipes deletados manualmente fora do app
     *  - Volume Docker remontado com dados antigos perdidos
     *
     * Retorna nº de rows fantasma removidas. Throttled (30s mínimo entre runs).
     */
    @Transactional
    public int cleanupOrphanClips() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTs < CLEANUP_THROTTLE_MS) return 0;
        lastCleanupTs = now;

        int removed = 0;
        int orphans = 0;
        // Page por 500 pra não estourar memória
        int page = 0;
        while (true) {
            var pg = repo.findAll(org.springframework.data.domain.PageRequest.of(page, 500));
            if (pg.isEmpty()) break;
            for (VoiceClip c : pg) {
                // Type 1: filePath = null (reserve sem upload)
                if (c.getFilePath() == null) {
                    // Só remove se for antigo (> 5min) — pode estar em meio do upload
                    long age = System.currentTimeMillis() -
                            (c.getCreatedAt() != null ? c.getCreatedAt().toEpochMilli() : System.currentTimeMillis());
                    if (age > 5 * 60_000) {
                        try { repo.deleteById(c.getId()); orphans++; } catch (Exception ignored) {}
                    }
                    continue;
                }
                // Type 2: filePath aponta pra arquivo que não existe
                Path file = Paths.get(storageDir, c.getFilePath());
                if (!Files.exists(file) && !c.isProtectedFromCleanup()) {
                    try { repo.deleteById(c.getId()); removed++; } catch (Exception ignored) {}
                }
            }
            if (!pg.hasNext()) break;
            page++;
        }
        if (orphans + removed > 0) {
            LOG.info("[Voice] cleanupOrphanClips: {} orphan reserves + {} arquivos fantasma deletados",
                    orphans, removed);
        }
        return orphans + removed;
    }

    /** Stats agregadas pra dashboard de transcrição: counts por status. */
    public java.util.Map<String, Object> transcriptionStats() {
        long total = repo.count();
        long done = repo.countByTranscriptionStatus("DONE");
        long pending = repo.countByTranscriptionStatus("PENDING") +
                repo.countByTranscriptionStatusIsNull();
        long processing = repo.countByTranscriptionStatus("PROCESSING");
        long failed = repo.countByTranscriptionStatus("FAILED");
        long skipped = repo.countByTranscriptionStatus("SKIPPED");
        return java.util.Map.of(
                "total", total,
                "done", done,
                "pending", pending,
                "processing", processing,
                "failed", failed,
                "skipped", skipped,
                "pctDone", total > 0 ? (done * 100.0 / total) : 0.0
        );
    }

    /**
     * Reseta TODOS os clipes em PROCESSING pra PENDING. Usado on-startup do
     * WhisperTranscriptionService pra recuperar de crashes/restarts onde o
     * worker morreu mid-transcribe e deixou clipe órfão.
     */
    @Transactional
    public int resetStuckProcessing() {
        // Não precisamos da entity completa — direct UPDATE é mais barato.
        int n = 0;
        for (VoiceClip c : repo.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent()) {
            if ("PROCESSING".equals(c.getTranscriptionStatus())) {
                c.setTranscriptionStatus("PENDING");
                repo.save(c);
                n++;
            }
        }
        return n;
    }

    /**
     * Reseta clipes em PROCESSING há mais de X ms. Safety net pra clipes
     * cuja transcrição hangou mais que o timeout. Usa a coluna `updatedAt`
     * implícita do save — não temos ela direto, então usamos `createdAt` +
     * margem (eventualmente substituir por column dedicada).
     */
    @Transactional
    public int resetStuckProcessingOlderThan(long thresholdMs) {
        Instant cutoff = Instant.now().minusMillis(thresholdMs);
        int n = 0;
        // Itera os PROCESSING — provavelmente poucos
        var page = repo.findAll(org.springframework.data.domain.PageRequest.of(0, 1000));
        for (VoiceClip c : page.getContent()) {
            if (!"PROCESSING".equals(c.getTranscriptionStatus())) continue;
            // ts é o timestamp do clipe, não da transição pra PROCESSING.
            // Mesmo assim funciona como proxy: se o clipe é antigo + ainda
            // PROCESSING, definitivamente travou.
            if (c.getTs() < cutoff.toEpochMilli()) {
                c.setTranscriptionStatus("PENDING");
                repo.save(c);
                n++;
            }
        }
        return n;
    }

    /** Atualiza só o status (ex: PENDING → PROCESSING) sem mexer no texto. */
    @Transactional
    public void setTranscriptionStatus(Long id, String status) {
        VoiceClip c = repo.findById(id).orElse(null);
        if (c == null) return;
        c.setTranscriptionStatus(status);
        repo.save(c);
    }

    /**
     * Limpa fragmentos de erro de IO ("open: failed to open ... for writing")
     * que vazaram pras transcrições em versões v85-v87. Roda em massa sobre
     * todos os clipes com transcrição preenchida.
     * Retorna número de linhas corrigidas.
     */
    @Transactional
    public int cleanIoErrorFragmentsInTranscriptions() {
        int fixed = 0;
        // Itera em batch — chunk de 1000 pra não estourar memória se tabela for grande
        int page = 0;
        final int SIZE = 1000;
        while (true) {
            var pg = repo.findAll(org.springframework.data.domain.PageRequest.of(page, SIZE));
            if (pg.isEmpty()) break;
            for (VoiceClip c : pg) {
                String t = c.getTranscription();
                if (t == null || t.isEmpty()) continue;
                if (!t.contains("open: failed to open")) continue;
                String cleaned = stripWhisperIoError(t);
                if (!cleaned.equals(t)) {
                    c.setTranscription(cleaned);
                    repo.save(c);
                    fixed++;
                }
            }
            if (!pg.hasNext()) break;
            page++;
        }
        LOG.info("[Voice] cleanIoErrorFragmentsInTranscriptions: {} clipes corrigidos", fixed);
        return fixed;
    }

    /** Idêntico ao stripIoErrorFragments do WhisperTranscriptionService — pega
     *  texto antes do "open:" + depois de "for writing" e junta. */
    private static String stripWhisperIoError(String s) {
        int openIdx = s.indexOf("open: failed to open");
        if (openIdx < 0) return s;
        String before = s.substring(0, openIdx).trim();
        int afterIdx = s.indexOf("for writing", openIdx);
        if (afterIdx >= 0) {
            String after = s.substring(afterIdx + "for writing".length()).trim();
            return (before + " " + after).trim();
        }
        return before;
    }

    /** Salva resultado de uma transcrição (sucesso ou falha). */
    @Transactional
    public void setTranscription(Long id, String text, String status, String errorMsg) {
        VoiceClip c = repo.findById(id).orElse(null);
        if (c == null) {
            LOG.warn("[Voice] setTranscription({}): clipe NÃO encontrado no DB", id);
            return;
        }
        if (text != null) c.setTranscription(text);
        if (status != null) c.setTranscriptionStatus(status);
        // Não armazenamos errorMsg em VoiceClip atualmente.
        repo.save(c);
        LOG.info("[Voice] setTranscription({}, status={}): salvo (text={} chars)",
                id, status, text == null ? 0 : text.length());
    }

    @Transactional
    public void delete(Long id) {
        VoiceClip c = repo.findById(id).orElse(null);
        if (c == null) return;
        if (c.getFilePath() != null) {
            Path file = Paths.get(storageDir, c.getFilePath());
            try {
                boolean existed = Files.deleteIfExists(file);
                if (!existed) {
                    LOG.warn("[Voice] delete({}): arquivo não existia em {} (DB tinha ref mas disco não)", id, file.toAbsolutePath());
                }
            } catch (Exception e) {
                // ANTES era 'catch ignored' silencioso — escondia falhas de
                // permissão / path mismatch que deixavam o WAV vazando no
                // volume. Agora loga pra detecção.
                LOG.error("[Voice] delete({}): falha apagando {} ({}). DB row será removido mesmo assim.",
                        id, file.toAbsolutePath(), e.toString());
            }
        }
        repo.deleteById(id);
    }

    /** Deleta múltiplos clipes (arquivos em disco + linhas no DB). */
    @Transactional
    public int deleteBulk(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        int n = 0;
        for (Long id : ids) {
            try { delete(id); n++; }
            catch (Exception ignored) {}
        }
        return n;
    }

    /** Deleta TODOS os clipes de um player. Útil pra purge rápida. */
    @Transactional
    public int deleteByPlayer(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) return 0;
        List<VoiceClip> clips = repo.findRecent(playerUuid,
                org.springframework.data.domain.PageRequest.of(0, 10_000));
        int n = 0;
        for (VoiceClip c : clips) {
            try { delete(c.getId()); n++; }
            catch (Exception ignored) {}
        }
        return n;
    }

    /** Deleta TODOS os clipes do arquivo (purge global). Operação destrutiva.
     *
     * <p>Faz 2 passos:
     * <ol>
     *   <li>Itera o DB e chama {@code delete(id)} pra cada — apaga linha+arquivo.</li>
     *   <li>Roda {@link #cleanupOrphanFiles()} pra varrer o disco e apagar qualquer
     *       WAV que tenha sobrado (delete parcial falhou, arquivos sem DB row,
     *       restos de migração, etc).</li>
     * </ol>
     * Antes só fazia o passo 1 — restavam WAVs órfãos ocupando o volume Docker
     * com nada apontando pra eles. (Bug reportado pelo admin: "fiz clear mas
     * os áudios continuam ocupando espaço".)
     */
    @Transactional
    public int deleteAll() {
        List<VoiceClip> clips = repo.findAll();
        int n = 0;
        for (VoiceClip c : clips) {
            try { delete(c.getId()); n++; }
            catch (Exception e) {
                LOG.warn("[Voice] deleteAll: falha em clip {}: {}", c.getId(), e.getMessage());
            }
        }
        // Sweep do disco: garante que NENHUM .wav sobre depois do purge.
        int orphans = cleanupOrphanFiles();
        LOG.warn("[Voice] deleteAll: {} clipe(s) removido(s) via DB + {} arquivo(s) órfão(s) varridos do disco",
                n, orphans);
        return n;
    }

    /**
     * Varre o diretório de storage e apaga arquivos {@code .wav} sem DB row
     * correspondente. Complementa {@link #cleanupOrphanClips()} (que faz o
     * inverso: deleta DB rows sem arquivo).
     *
     * <p>Layout esperado: {@code <storageDir>/yyyy-MM/<id>.wav}. Walking depth=2.
     * Para cada wav, extrai o ID do nome (basename sem extensão), checa se
     * existe no DB. Se não, deleta o arquivo.
     *
     * <p>Subdir vazio também é apagado pra não acumular pastas de meses sem
     * uso.
     *
     * <p>Returns total de arquivos apagados.
     *
     * <p><b>Cuidado:</b> a chamada lista todos os IDs do DB em memória (use
     * só pra purges/cron, não em tight loop). Pra catálogos grandes >1M
     * clipes ainda é seguro — IDs são longs (8 bytes cada).
     */
    public int cleanupOrphanFiles() {
        Path root = Paths.get(storageDir);
        if (!Files.exists(root)) {
            LOG.debug("[Voice] cleanupOrphanFiles: storage dir não existe ({})", root.toAbsolutePath());
            return 0;
        }
        // Snapshot dos IDs no DB (Set<Long>).
        java.util.Set<Long> dbIds = new java.util.HashSet<>();
        for (VoiceClip c : repo.findAll()) {
            dbIds.add(c.getId());
        }

        int filesDeleted = 0;
        long bytesFreed = 0;
        java.util.List<Path> emptyDirs = new java.util.ArrayList<>();

        try (var stream = Files.walk(root, 2)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(p)) continue;
                String name = p.getFileName().toString();
                if (!name.endsWith(".wav")) continue;
                String idStr = name.substring(0, name.length() - 4);
                Long id;
                try { id = Long.parseLong(idStr); }
                catch (NumberFormatException e) {
                    // Nome não-padrão (não é "<id>.wav") — não tocamos pra não
                    // apagar arquivos que outros sistemas/admin possa ter colocado.
                    LOG.debug("[Voice] cleanupOrphanFiles: ignorando {} (nome não-numérico)", p);
                    continue;
                }
                if (dbIds.contains(id)) continue; // tem DB row, mantém

                try {
                    long size = Files.size(p);
                    Files.delete(p);
                    filesDeleted++;
                    bytesFreed += size;
                } catch (IOException e) {
                    LOG.warn("[Voice] cleanupOrphanFiles: falha apagando {} ({})", p, e.toString());
                }
            }
        } catch (IOException e) {
            LOG.error("[Voice] cleanupOrphanFiles: walk falhou em {}: {}", root, e.toString());
            return filesDeleted;
        }

        // Apaga subdirs vazios (limpa pastas yyyy-MM sem nenhum wav).
        try (var stream = Files.list(root)) {
            for (Path dir : (Iterable<Path>) stream::iterator) {
                if (!Files.isDirectory(dir)) continue;
                try (var contents = Files.list(dir)) {
                    if (contents.findAny().isEmpty()) emptyDirs.add(dir);
                } catch (IOException ignored) {}
            }
        } catch (IOException ignored) {}
        for (Path d : emptyDirs) {
            try { Files.delete(d); }
            catch (IOException e) { LOG.debug("[Voice] cleanupOrphanFiles: falha apagando dir vazio {}", d); }
        }

        if (filesDeleted > 0 || !emptyDirs.isEmpty()) {
            LOG.info("[Voice] cleanupOrphanFiles: {} arquivo(s) órfão(s) ({} bytes / {}MB) + {} subdir(s) vazios apagados",
                    filesDeleted, bytesFreed, bytesFreed / 1024 / 1024, emptyDirs.size());
        }
        return filesDeleted;
    }

    /** Manda o mod tocar este clipe no mundo. */
    public boolean playInGame(Long clipId, double x, double y, double z,
                              String dimension, float volume, String category) {
        VoiceClip c = repo.findById(clipId).orElse(null);
        if (c == null || !"DONE".equals(c.getUploadStatus())) return false;
        try {
            mod.voicePlay(java.util.Map.of(
                    "clipId", clipId,
                    "x", x, "y", y, "z", z,
                    "dimension", dimension == null ? "minecraft:overworld" : dimension,
                    "volume", volume,
                    "category", category == null ? "liberthia_voice" : category
            ));
            return true;
        } catch (Exception e) {
            LOG.warn("[Voice] playInGame fail: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Cleanup diário às 5h. Le settings DINÂMICO do DB (não env) — admin pode
     * desabilitar via painel ou mudar retention days.
     * MAS preserva os marcados como protectedFromCleanup. Roda 1x/dia pra não
     * sobrecarregar o servidor — sempre cedo na madrugada.
     */
    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void cleanup() {
        try {
            VoiceRetentionSettings settings = retention.get();
            if (!settings.isEnabled()) {
                LOG.info("[Voice] cleanup desabilitado via painel — pulando.");
                return;
            }
            int days = settings.getRetentionDays();
            int removed = runCleanup(days);
            LOG.info("[Voice] cleanup automático: removeu {} clipes (>{}d). Protegidos preservados.",
                    removed, days);

            // Enforce per-player limit no mesmo job (uma vez por dia).
            if (settings.getMaxClipsPerPlayer() > 0) {
                int perPlayer = enforceAllPerPlayerLimits();
                LOG.info("[Voice] cleanup automático: per-player limit removeu mais {} clipes (max={}/player)",
                        perPlayer, settings.getMaxClipsPerPlayer());
            }

            // Orphans: reserve sem upload concluído após 1h — sempre roda,
            // mesmo se retention desabilitado (são lixo, não clipe válido).
            Instant orphanCutoff = Instant.now().minus(1, ChronoUnit.HOURS);
            List<VoiceClip> orphans = repo.findOrphans(orphanCutoff);
            for (VoiceClip o : orphans) repo.delete(o);
            if (!orphans.isEmpty()) LOG.info("[Voice] cleanup removeu {} orphans", orphans.size());
        } catch (Exception e) {
            LOG.warn("[Voice] cleanup falhou: {}", e.getMessage());
        }
    }

    /**
     * Cleanup manual (botão "rodar agora" no painel) — usa days passado ou,
     * se null, lê dos settings do DB. Retorna nº de clipes removidos.
     * NÃO depende de enabled — quem clicou o botão tem intenção explícita.
     */
    @Transactional
    public int runManualCleanup(Integer overrideDays) {
        int days = overrideDays != null ? Math.max(1, overrideDays)
                : retention.get().getRetentionDays();
        return runCleanup(days);
    }

    /** Implementação compartilhada cleanup(). NÃO é transacional aqui — caller decide. */
    private int runCleanup(int days) {
        long cutoff = System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000;
        List<VoiceClip> doomed = repo.findExpiredUnprotected(cutoff);
        int filesDeleted = 0;
        for (VoiceClip c : doomed) {
            if (c.getFilePath() == null) continue;
            Path file = Paths.get(storageDir, c.getFilePath());
            try {
                if (Files.deleteIfExists(file)) filesDeleted++;
            } catch (Exception e) {
                LOG.warn("[Voice] runCleanup: falha apagando {} ({})", file, e.toString());
            }
        }
        int removed = repo.deleteOlderThanUnprotected(cutoff);
        // Sweep adicional: arquivos órfãos no disco (DB row já apagado mas WAV
        // sobreviveu por delete falho, ou WAVs sem DB row de versões antigas).
        int orphanFiles = cleanupOrphanFiles();
        LOG.info("[Voice] runCleanup(days={}): {} rows DB, {} arquivos disco (expired) + {} arquivos órfãos varridos",
                days, removed, filesDeleted, orphanFiles);
        return removed;
    }

    /** Conta quantos clipes SERIAM deletados sem realmente apagar — preview pro painel. */
    public java.util.Map<String, Object> previewCleanup(int days) {
        long cutoff = System.currentTimeMillis() - (long) Math.max(1, days) * 24 * 60 * 60 * 1000;
        List<VoiceClip> doomed = repo.findExpiredUnprotected(cutoff);
        long totalBytes = 0L;
        long totalDuration = 0L;
        for (VoiceClip c : doomed) {
            totalBytes += c.getSizeBytes();
            totalDuration += c.getDurationMs();
        }
        return java.util.Map.of(
                "days", days,
                "cutoff", cutoff,
                "wouldDelete", doomed.size(),
                "totalBytes", totalBytes,
                "totalDurationMs", totalDuration
        );
    }

    // =========================================================================
    // Proteção (preserva clipes selecionados da auto-cleanup)
    // =========================================================================

    @Transactional
    public VoiceClip setProtection(Long id, boolean protect, String reason, String by) {
        VoiceClip c = repo.findById(id).orElseThrow();
        c.setProtectedFromCleanup(protect);
        c.setProtectionReason(protect ? reason : null);
        c.setProtectedBy(protect ? by : null);
        c.setProtectedAt(protect ? Instant.now() : null);
        return repo.save(c);
    }

    @Transactional
    public int setProtectionBulk(List<Long> ids, boolean protect, String reason, String by) {
        if (ids == null || ids.isEmpty()) return 0;
        int n = 0;
        Instant now = Instant.now();
        for (Long id : ids) {
            VoiceClip c = repo.findById(id).orElse(null);
            if (c == null) continue;
            c.setProtectedFromCleanup(protect);
            c.setProtectionReason(protect ? reason : null);
            c.setProtectedBy(protect ? by : null);
            c.setProtectedAt(protect ? now : null);
            repo.save(c);
            n++;
        }
        return n;
    }

    // =========================================================================
    // ZIP download (todos os clipes de 1 player)
    // =========================================================================

    /** Retorna byte[] com ZIP de todos os WAVs do player. */
    public byte[] zipForPlayer(String playerUuid) throws IOException {
        List<VoiceClip> clips = repo.findAllByPlayer(playerUuid);
        var baos = new java.io.ByteArrayOutputStream();
        try (var zos = new java.util.zip.ZipOutputStream(baos)) {
            int idx = 0;
            for (VoiceClip c : clips) {
                if (c.getFilePath() == null) continue;
                Path file = Paths.get(storageDir, c.getFilePath());
                if (!Files.exists(file)) continue;
                String name = String.format("%04d_%s_%dms_%d.wav",
                        ++idx,
                        c.getPlayerName() == null ? "unknown" : c.getPlayerName().replaceAll("[^A-Za-z0-9_-]", "_"),
                        c.getDurationMs(), c.getTs());
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                zos.write(Files.readAllBytes(file));
                zos.closeEntry();
            }
            // README com metadata
            zos.putNextEntry(new java.util.zip.ZipEntry("README.txt"));
            StringBuilder sb = new StringBuilder();
            sb.append("Voice clips export\n");
            sb.append("Player UUID: ").append(playerUuid).append("\n");
            sb.append("Total: ").append(idx).append(" clips\n");
            sb.append("Exported at: ").append(Instant.now()).append("\n");
            zos.write(sb.toString().getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
