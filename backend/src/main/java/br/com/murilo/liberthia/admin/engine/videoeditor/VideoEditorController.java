package br.com.murilo.liberthia.admin.engine.videoeditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Video Editor — compõe um MP4 misturando: imagem de fundo + música opcional
 * + N voice clips em timeline + título opcional.
 *
 * Pipeline:
 *  1. POST /api/video/upload-asset (multipart) → guarda imagem/musica em tmp
 *  2. POST /api/video/render → recebe project JSON, roda ffmpeg, retorna jobId
 *  3. GET /api/video/status/{jobId} → progresso/status do render
 *  4. GET /api/video/download/{jobId} → baixa o MP4 final
 *
 * Render é assíncrono (ffmpeg pesa CPU). Mantém últimos 5 renders em disco;
 * cleanup automático ao deletar.
 */
@RestController
@RequestMapping("/api/video")
public class VideoEditorController {

    private static final Logger LOG = LoggerFactory.getLogger(VideoEditorController.class);

    @Value("${voice.storage-dir:/app/data/voice-clips}")
    private String voiceStorageDir;

    @Value("${video.storage-dir:/app/data/videos}")
    private String videoStorageDir;

    /** Jobs em memória — pra produção poderia ser DB, mas videos são efêmeros. */
    private final Map<String, RenderJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService renderPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "video-render");
        t.setDaemon(true);
        return t;
    });

    private Path baseDir() {
        try {
            Path p = Paths.get(videoStorageDir);
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException("mkdir " + videoStorageDir + ": " + e.getMessage(), e);
        }
    }

    private Path tempDir() {
        try {
            Path p = baseDir().resolve("tmp");
            Files.createDirectories(p);
            return p;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Upload de imagem de fundo ou música. Retorna `assetId` referenciável no project. */
    @PostMapping(value = "/upload-asset", consumes = "multipart/form-data")
    public Map<String, Object> uploadAsset(@RequestParam("file") MultipartFile file,
                                           @RequestParam("kind") String kind) throws IOException {
        if (file == null || file.isEmpty()) {
            return Map.of("ok", false, "error", "file empty");
        }
        if (!"image".equals(kind) && !"music".equals(kind)) {
            return Map.of("ok", false, "error", "kind must be 'image' or 'music'");
        }
        String orig = file.getOriginalFilename() == null ? "asset" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')) : "";
        String assetId = kind + "_" + UUID.randomUUID() + ext;
        Path target = tempDir().resolve(assetId);
        Files.write(target, file.getBytes());
        return Map.of("ok", true, "assetId", assetId, "size", file.getSize(), "name", orig);
    }

    /** Inicia render. Retorna jobId pra polling. */
    @PostMapping("/render")
    public Map<String, Object> render(@RequestBody VideoProject project) {
        if (project.voiceClips() == null || project.voiceClips().isEmpty()) {
            return Map.of("ok", false, "error", "Sem voice clips no projeto");
        }
        // Limite duro de 30 clipes por vídeo. Acima disso, amix do ffmpeg
        // aloca buffers enormes (adelay × inputs × delay duration) e
        // estoura RAM em containers de 6GB.
        if (project.voiceClips().size() > 30) {
            return Map.of("ok", false,
                    "error", "Máximo 30 voice clips por vídeo (você enviou " + project.voiceClips().size() +
                            "). Reduz o número de clipes ou divide em vídeos separados.");
        }
        if (VideoRenderMutex.isRunning()) {
            return Map.of("ok", false,
                    "error", "Já tem um render rodando. Aguarda terminar antes de enviar outro.");
        }
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        RenderJob job = new RenderJob();
        job.id = jobId;
        job.status = "QUEUED";
        job.startedAt = Instant.now();
        job.project = project;
        jobs.put(jobId, job);

        renderPool.submit(() -> runRender(job));

        return Map.of("ok", true, "jobId", jobId, "status", "QUEUED");
    }

    @GetMapping("/status/{jobId}")
    public Map<String, Object> status(@PathVariable String jobId) {
        RenderJob j = jobs.get(jobId);
        if (j == null) return Map.of("ok", false, "error", "job not found");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("id", j.id);
        out.put("status", j.status);
        out.put("progress", j.progress);
        out.put("message", j.message == null ? "" : j.message);
        out.put("outputPath", j.outputPath == null ? null : j.outputPath.getFileName().toString());
        out.put("startedAt", j.startedAt.toString());
        if (j.finishedAt != null) out.put("finishedAt", j.finishedAt.toString());
        if (j.errorMsg != null) out.put("error", j.errorMsg);
        return out;
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String jobId) {
        RenderJob j = jobs.get(jobId);
        if (j == null || j.outputPath == null || !Files.exists(j.outputPath)) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource res = new FileSystemResource(j.outputPath.toFile());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + j.outputPath.getFileName() + "\"")
                .body(res);
    }

    /** Acessor pro SavedVideoController saber onde está o arquivo do render. */
    Path findRenderOutput(String jobId) {
        RenderJob j = jobs.get(jobId);
        return j == null ? null : j.outputPath;
    }

    @GetMapping("/jobs")
    public Map<String, Object> listJobs() {
        return Map.of("jobs", jobs.values().stream()
                .sorted((a, b) -> b.startedAt.compareTo(a.startedAt))
                .map(j -> Map.of(
                        "id", j.id,
                        "status", j.status,
                        "progress", j.progress,
                        "startedAt", j.startedAt.toString(),
                        "outputName", j.outputPath == null ? null : j.outputPath.getFileName().toString()
                )).toList());
    }

    /**
     * Versão do código rodando — pra debug. Se essa string não bater
     * com "v54-two-pass-mutex", você está em backend antigo (precisa atualizar
     * imagem Docker no Portainer + Re-pull image).
     */
    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of(
                "implementation", "v55-amix-normalize-off",
                "features", List.of(
                        "two-pass-ffmpeg",
                        "mutex-with-whisper",
                        "ultrafast-preset",
                        "2fps-image-loop",
                        "scale-1280x720-pad",
                        "max-30-voice-clips"
                ),
                "renderInProgress", VideoRenderMutex.isRunning()
        );
    }

    // =========================================================================
    // ffmpeg orchestration
    // =========================================================================

    private void runRender(RenderJob job) {
        job.status = "RENDERING";
        job.progress = 5;
        VideoRenderMutex.start();   // <- pausa Whisper workers
        try {
            VideoProject p = job.project;
            Path outFile = baseDir().resolve("video_" + job.id + "_" +
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(java.time.LocalDateTime.now()) + ".mp4");
            job.outputPath = outFile;

            // Resolve assets
            Path bgImage = p.backgroundImageId() != null ? tempDir().resolve(p.backgroundImageId()) : null;
            Path musicFile = p.musicId() != null ? tempDir().resolve(p.musicId()) : null;
            if (bgImage != null && !Files.exists(bgImage)) {
                throw new IOException("background image not found: " + p.backgroundImageId());
            }
            if (musicFile != null && !Files.exists(musicFile)) {
                throw new IOException("music not found: " + p.musicId());
            }

            // Calcula duração total
            long durationMs = 0;
            for (VoiceTrack vt : p.voiceClips()) {
                Path clipPath = resolveVoiceClip(vt.clipId());
                if (clipPath == null) throw new IOException("voice clip not found: " + vt.clipId());
                long clipDuration = probeDurationMs(clipPath);
                long endMs = vt.startMs() + clipDuration;
                if (endMs > durationMs) durationMs = endMs;
            }
            durationMs = Math.max(5000, durationMs + 1000);
            double durationSec = durationMs / 1000.0;
            job.progress = 10;

            // ============================================================
            // PASS 1: Renderiza só o áudio (mix de música + voices) em WAV.
            // Isolar áudio do vídeo evita acúmulo de buffer simultâneo no
            // ffmpeg — antes deu OOM (exit=137) em vídeos > 30s.
            // ============================================================
            Path tmpAudio = tempDir().resolve("audio_" + job.id + ".wav");
            renderAudioPass(job, p, musicFile, durationSec, tmpAudio);
            job.progress = 50;
            job.message = "audio renderizado; gerando vídeo…";

            // ============================================================
            // PASS 2: Renderiza vídeo da imagem (loop 2fps) + audio mixado.
            // 2fps é suficiente pra imagem estática. yuv420p exige
            // dimensão par — força scale pra 1280x720 (ou pad se aspect
            // ratio diferente).
            // ============================================================
            renderVideoPass(job, bgImage, tmpAudio, outFile, durationSec);

            // Cleanup tmp
            try { Files.deleteIfExists(tmpAudio); } catch (Exception ignored) {}

            job.progress = 100;
            job.status = "DONE";
            job.message = "render concluído: " + Files.size(outFile) + " bytes";
            job.finishedAt = Instant.now();
            LOG.info("[VideoEditor] job {} OK: {} ({} bytes)", job.id, outFile.getFileName(), Files.size(outFile));

            cleanupOldVideos(5);
        } catch (Exception e) {
            job.status = "FAILED";
            job.errorMsg = e.getMessage();
            job.finishedAt = Instant.now();
            LOG.error("[VideoEditor] job {} FAILED: {}", job.id, e.getMessage(), e);
        } finally {
            VideoRenderMutex.stop();   // <- libera Whisper workers
        }
    }

    /**
     * PASS 1: mix de todas as faixas de áudio em um WAV temporário.
     * Sem encode de vídeo competindo por buffer = muito menos RAM.
     */
    private void renderAudioPass(RenderJob job, VideoProject p, Path musicFile,
                                 double durationSec, Path tmpAudio) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-threads"); cmd.add("2");

        int inputIdx = 0;
        List<String> filterParts = new ArrayList<>();

        if (musicFile != null) {
            // -stream_loop -1 ANTES do -i loop o input infinitamente. O -t
            // global corta no fim. Se musicLoop=false (default), só toca 1×.
            boolean loop = Boolean.TRUE.equals(p.musicLoop());
            if (loop) {
                cmd.add("-stream_loop"); cmd.add("-1");
            }
            cmd.add("-i"); cmd.add(musicFile.toString());
            float vol = (p.musicVolume() != null ? p.musicVolume() : 0.3f);
            filterParts.add(String.format(Locale.ROOT, "[%d:a]volume=%.2f[m]", inputIdx, vol));
            inputIdx++;
        }

        List<String> voiceLabels = new ArrayList<>();
        for (int i = 0; i < p.voiceClips().size(); i++) {
            VoiceTrack vt = p.voiceClips().get(i);
            Path clipPath = resolveVoiceClip(vt.clipId());
            cmd.add("-i"); cmd.add(clipPath.toString());
            String label = "v" + i;
            voiceLabels.add(label);
            filterParts.add(String.format(Locale.ROOT,
                    "[%d:a]volume=%.2f,adelay=%d:all=1[%s]",
                    inputIdx, vt.volume() != null ? vt.volume() : 1.0f, vt.startMs(), label));
            inputIdx++;
        }

        StringBuilder mix = new StringBuilder();
        int mixCount = 0;
        if (musicFile != null) { mix.append("[m]"); mixCount++; }
        for (String l : voiceLabels) { mix.append("[").append(l).append("]"); mixCount++; }
        // normalize=0 → não divide volume por N inputs. Antes (default
        // normalize=1) com 30 tracks cada voice virava 1/30 do volume original
        // = quase inaudível. Com normalize=0 cada track mantém seu volume.
        mix.append(String.format(
                "amix=inputs=%d:duration=longest:dropout_transition=3:normalize=0,aresample=async=1[aout]",
                mixCount));
        filterParts.add(mix.toString());

        cmd.add("-filter_complex");
        cmd.add(String.join(";", filterParts));
        cmd.add("-map"); cmd.add("[aout]");
        cmd.add("-c:a"); cmd.add("pcm_s16le");
        cmd.add("-ar"); cmd.add("44100");
        cmd.add("-t"); cmd.add(String.format(Locale.ROOT, "%.3f", durationSec));
        cmd.add(tmpAudio.toString());

        runFfmpeg(job, cmd, "audio pass", durationSec, 10, 50);
    }

    /**
     * PASS 2: video a partir de imagem (2fps loop) + audio pré-mixado.
     * Resolução fixa 1280×720 com scale+pad (ratio agnóstico).
     */
    private void renderVideoPass(RenderJob job, Path bgImage, Path tmpAudio,
                                 Path outFile, double durationSec) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-threads"); cmd.add("2");

        // Input 0: image OR color filter
        if (bgImage != null) {
            cmd.add("-loop"); cmd.add("1");
            cmd.add("-framerate"); cmd.add("2");  // só 2 frames/s no INPUT — ffmpeg não enche buffer
            cmd.add("-i"); cmd.add(bgImage.toString());
        } else {
            cmd.add("-f"); cmd.add("lavfi");
            cmd.add("-i"); cmd.add("color=c=black:s=1280x720:r=2");
        }
        // Input 1: pre-rendered audio
        cmd.add("-i"); cmd.add(tmpAudio.toString());

        // Scale + pad pra 1280x720 (yuv420p exige largura/altura par).
        // force_original_aspect_ratio=decrease + pad mantém imagem proporcional
        // sem cortar; barra preta nas bordas se aspect ratio do user difere.
        cmd.add("-vf");
        cmd.add("scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2:color=black");

        cmd.add("-map"); cmd.add("0:v");
        cmd.add("-map"); cmd.add("1:a");

        // Encode — preset ultrafast + 2 threads = baixíssimo uso de RAM
        cmd.add("-c:v"); cmd.add("libx264");
        cmd.add("-preset"); cmd.add("ultrafast");
        cmd.add("-crf"); cmd.add("28");
        cmd.add("-pix_fmt"); cmd.add("yuv420p");
        cmd.add("-r"); cmd.add("2");  // OUTPUT 2fps — imagem estática não precisa de mais
        cmd.add("-max_muxing_queue_size"); cmd.add("256");
        cmd.add("-c:a"); cmd.add("aac");
        cmd.add("-b:a"); cmd.add("128k");
        cmd.add("-shortest");
        cmd.add("-t"); cmd.add(String.format(Locale.ROOT, "%.3f", durationSec));
        cmd.add(outFile.toString());

        runFfmpeg(job, cmd, "video pass", durationSec, 50, 95);
    }

    /** Executa ffmpeg + parse de progress. progressMin/Max mapeia o tempo do ffmpeg
     *  pra range de progress do job (ex: audio pass 10→50%, video pass 50→95%). */
    private void runFfmpeg(RenderJob job, List<String> cmd, String label,
                           double durationSec, int progressMin, int progressMax) throws Exception {
        LOG.info("[VideoEditor] job {} {} cmd: {}", job.id, label, String.join(" ", cmd));
        job.message = "executando ffmpeg " + label + "…";

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (out.length() < 32000) out.append(line).append("\n");
                int idx = line.indexOf("time=");
                if (idx >= 0 && durationSec > 0) {
                    try {
                        String t = line.substring(idx + 5, Math.min(line.length(), idx + 5 + 11));
                        String[] parts = t.split(":");
                        if (parts.length == 3) {
                            double sec = Integer.parseInt(parts[0]) * 3600
                                    + Integer.parseInt(parts[1]) * 60
                                    + Double.parseDouble(parts[2]);
                            int pct = (int) Math.min(progressMax,
                                    progressMin + (sec / durationSec * (progressMax - progressMin)));
                            job.progress = pct;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        int exit = proc.waitFor();
        if (exit != 0) {
            throw new RuntimeException("ffmpeg [" + label + "] exit=" + exit + ":\n" +
                    (out.length() > 800 ? out.substring(out.length() - 800) : out));
        }
    }

    private Path resolveVoiceClip(long clipId) {
        // Voice clips são armazenados em $voiceStorageDir/yyyy-MM/<id>.wav
        // Busca recursiva (simples) na pasta — fallback se cliquímos clipes antigos.
        try {
            Path root = Paths.get(voiceStorageDir);
            if (!Files.exists(root)) return null;
            try (var stream = Files.walk(root)) {
                return stream.filter(p -> p.getFileName().toString().equals(clipId + ".wav"))
                        .findFirst().orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private long probeDurationMs(Path file) {
        // ffprobe pra duração exata. Mais lento que ler WAV header, mas robusto.
        try {
            ProcessBuilder pb = new ProcessBuilder("ffprobe",
                    "-v", "error", "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1", file.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new BufferedReader(new InputStreamReader(p.getInputStream()))
                    .readLine();
            p.waitFor();
            if (output == null || output.isBlank()) return 5000;
            return (long) (Double.parseDouble(output.trim()) * 1000);
        } catch (Exception e) {
            // Fallback: assume 5s
            return 5000;
        }
    }

    private void cleanupOldVideos(int keep) {
        try {
            Path dir = baseDir();
            var videos = Files.list(dir)
                    .filter(p -> p.getFileName().toString().endsWith(".mp4"))
                    .sorted((a, b) -> {
                        try { return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a)); }
                        catch (Exception e) { return 0; }
                    })
                    .toList();
            for (int i = keep; i < videos.size(); i++) {
                Files.deleteIfExists(videos.get(i));
            }
        } catch (Exception e) {
            LOG.warn("[VideoEditor] cleanup falha: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Internal classes
    // =========================================================================

    static class RenderJob {
        String id;
        String status; // QUEUED, RENDERING, DONE, FAILED
        int progress;
        String message;
        String errorMsg;
        Path outputPath;
        Instant startedAt;
        Instant finishedAt;
        VideoProject project;
    }
}
