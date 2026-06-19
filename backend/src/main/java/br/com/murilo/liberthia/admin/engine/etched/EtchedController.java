package br.com.murilo.liberthia.admin.engine.etched;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Endpoints da Music Disc Library (Etched). Admin cadastra URLs e dispara
 * /give pra entregar o disco no jogo via NBT customizado.
 */
@RestController
@RequestMapping("/api/etched")
public class EtchedController {

    private static final Logger LOG = LoggerFactory.getLogger(EtchedController.class);

    private final EtchedDiscRepository repo;
    private final ModBridgeClient mod;

    /**
     * URL pública do backend (vai pro NBT do disco). Tem que ser acessível
     * pelo cliente do Minecraft — players acessam pra baixar o mp3.
     * Default `https://backend.astaroneremita.com` mas override via env BACKEND_PUBLIC_URL.
     */
    @Value("${backend.public-url:https://backend.astaroneremita.com}")
    private String backendPublicUrl;

    /** Diretório onde os mp3 extraídos via yt-dlp ficam. */
    @Value("${etched.audio-dir:/app/data/etched-audio}")
    private String audioDir;

    public EtchedController(EtchedDiscRepository repo, ModBridgeClient mod) {
        this.repo = repo;
        this.mod = mod;
    }

    @GetMapping("/discs")
    public List<EtchedDisc> list(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank())
            return repo.findByCategoryOrderByCreatedAtDesc(category);
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/discs")
    public EtchedDisc create(@RequestBody EtchedDisc d) {
        if (d.getTitle() == null || d.getTitle().isBlank())
            throw new IllegalArgumentException("title obrigatório");
        // URL é opcional: admin pode subir mp3 manualmente via /upload-audio
        // depois. Pra dar o disco antes do upload precisaria URL — mas o flow
        // normal é: cria → upload → dar.
        if (d.getUrl() == null) d.setUrl("");
        return repo.save(d);
    }

    @PutMapping("/discs/{id}")
    public ResponseEntity<EtchedDisc> update(@PathVariable Long id, @RequestBody EtchedDisc body) {
        return repo.findById(id).map(d -> {
            if (body.getTitle() != null) d.setTitle(body.getTitle());
            if (body.getAuthor() != null) d.setAuthor(body.getAuthor());
            if (body.getUrl() != null) d.setUrl(body.getUrl());
            if (body.getDiscColor() != null) d.setDiscColor(body.getDiscColor());
            if (body.getDescription() != null) d.setDescription(body.getDescription());
            if (body.getCategory() != null) d.setCategory(body.getCategory());
            d.setDurationSec(body.getDurationSec());
            return ResponseEntity.ok(repo.save(d));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/discs/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("ok", true);
    }

    /**
     * Dá o disco a um player via /give com NBT do Etched.
     *
     * Item ID correto (Etched 3.x, verificado via /api/items do mod):
     *   etched:etched_music_disc — o disco gravado/customizado
     *   etched:blank_music_disc  — disco em branco (não usado aqui)
     *   etched:music_label       — etiqueta (não usado aqui)
     *
     * NBT format: {Music:{Title:"X",Author:"Y",Url:"https://..."}}.
     */
    @PostMapping("/discs/{id}/give")
    public Map<String, Object> give(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EtchedDisc d = repo.findById(id).orElse(null);
        if (d == null) return Map.of("ok", false, "error", "disco não existe");
        String name = body.getOrDefault("playerName", "").toString();
        int count = body.get("count") instanceof Number n ? n.intValue() : 1;
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");

        String cmd = String.format("give %s %s %d", name, buildEtchedItemSnbt(d), count);
        mod.runCommand(cmd);

        d.setPlayCount(d.getPlayCount() + 1);
        repo.save(d);
        return Map.of("ok", true, "cmd", cmd);
    }

    /** Dá o disco a vários players de uma vez (target = @a ou seleção). */
    @PostMapping("/discs/{id}/play-at")
    public Map<String, Object> playAt(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EtchedDisc d = repo.findById(id).orElse(null);
        if (d == null) return Map.of("ok", false, "error", "disco não existe");
        String target = body.getOrDefault("target", "@a").toString();
        mod.runCommand("execute as " + target + " run give @s " + buildEtchedItemSnbt(d) + " 1");
        d.setPlayCount(d.getPlayCount() + 1);
        repo.save(d);
        return Map.of("ok", true);
    }

    /**
     * Constrói o SNBT do item etched:etched_music_disc.
     *
     * NBT REAL (verificado em MoonflowerTeam/Etched/1.20.x branch,
     * EtchedMusicDiscItem.setMusic / setColor / setPattern):
     *
     *   Music:       compound (1 track) ou list (2+ tracks)
     *                  Inside: Url (required), Title, Author
     *   Album:       compound (só se tracks ≥ 2 — primeiro track virá aqui)
     *   DiscColor:   int  RGB
     *   LabelColor:  compound {Primary:int, Secondary:int}
     *   Pattern:     byte (ordinal do enum LabelPattern, default 0 = flat)
     *
     * Bugs históricos:
     *  - Eu usava `Tracks` (lista) → não existe, devia ser `Music` compound
     *  - Campos do TrackData: minha versão usava `url/title/artist` (lowercase)
     *    mas o codec real espera `Url/Title/Author` (capitalizado)
     */
    private String buildEtchedItemSnbt(EtchedDisc d) {
        String urlValue;
        if ("READY".equals(d.getAudioStatus())) {
            urlValue = backendPublicUrl.replaceAll("/+$", "")
                    + "/api/etched/audio/" + d.getId() + ".mp3";
        } else {
            urlValue = d.getUrl();
        }
        String title = jsonString(d.getTitle());
        String author = jsonString(d.getAuthor() == null || d.getAuthor().isBlank()
                ? "Unknown" : d.getAuthor());
        String url = jsonString(urlValue);
        int discColor = colorFromName(d.getDiscColor() == null ? "blank" : d.getDiscColor());
        // Duration em TICKS (20 ticks = 1 segundo). Default 6000 (5 min) se 0.
        // Esse campo é CRUCIAL pro mod sophisticatedbackpacksetchedintegration
        // funcionar — seu `EtchedDiscHandler.getLengthInTicks()` lê esse campo
        // e se for 0/missing, o handler trata o disco como inválido e a
        // mochila não toca. O mixin TrackDataMixin desse mod normalmente
        // popula isso baixando o mp3 no save() — mas via /give pulamos save().
        int duration = d.getDurationSec() > 0 ? d.getDurationSec() * 20 : 6000;
        return "etched:etched_music_disc{"
                + "Music:{Url:" + url + ",Title:" + title + ",Author:" + author
                + ",Duration:" + duration + "},"
                + "DiscColor:" + discColor + ","
                + "LabelColor:{Primary:0,Secondary:0},"
                + "Pattern:0b"
                + "}";
    }

    /** Mapeia o discColor (preset) pra um int RGB pra coloração visual. */
    private static int colorFromName(String name) {
        return switch (name == null ? "" : name.toLowerCase()) {
            case "lapis"   -> 0x1A237E; // azul-roxo
            case "gold"    -> 0xFFD700;
            case "diamond" -> 0x4DD0E1;
            case "emerald" -> 0x66BB6A;
            case "nether"  -> 0xC62828;
            default        -> 0xCCCCCC; // blank — cinza claro
        };
    }

    /**
     * Kit Etching completo — dá os 3 items necessários pro player gravar disco
     * no jogo via etching table:
     *   1× etched:etching_table (bloco)
     *   1× etched:music_label   (com URL/título/artista no NBT)
     *   1× etched:blank_music_disc
     *
     * Player coloca a table, RMB nela, junta label+disco no slot da GUI →
     * sai disco gravado pelo MOD com NBT 100% correto. Esse disco TOCA em
     * minecraft:jukebox vanilla e em todas as jukeboxes do Etched.
     */
    @PostMapping("/discs/{id}/give-kit")
    public Map<String, Object> giveKit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EtchedDisc d = repo.findById(id).orElse(null);
        if (d == null) return Map.of("ok", false, "error", "disco não existe");
        String name = body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        boolean includeTable = body.get("includeTable") instanceof Boolean b ? b : true;

        String urlValue = "READY".equals(d.getAudioStatus())
                ? backendPublicUrl.replaceAll("/+$", "") + "/api/etched/audio/" + d.getId() + ".mp3"
                : d.getUrl();
        String title = jsonString(d.getTitle());
        String author = jsonString(d.getAuthor() == null || d.getAuthor().isBlank()
                ? "Unknown" : d.getAuthor());
        String url = jsonString(urlValue);
        // music_label: mesma estrutura Music:{Url,Title,Author} capitalizada do disco
        String labelNbt = "{Music:{Url:" + url + ",Title:" + title + ",Author:" + author + "}}";

        if (includeTable) mod.runCommand("give " + name + " etched:etching_table 1");
        mod.runCommand("give " + name + " etched:music_label" + labelNbt + " 1");
        mod.runCommand("give " + name + " etched:blank_music_disc 1");
        d.setPlayCount(d.getPlayCount() + 1);
        repo.save(d);
        return Map.of("ok", true,
                "note", "Kit entregue: " + (includeTable ? "etching_table + " : "") +
                        "music_label (com URL) + blank_music_disc. " +
                        "Player junta label+disco na etching table → disco final TOCA em qualquer jukebox.");
    }

    /** Discos vanilla que a mochila SB Jukebox Upgrade aceita (instanceof RecordItem). */
    public static final List<String> VANILLA_DISCS = List.of(
            "minecraft:music_disc_13",
            "minecraft:music_disc_cat",
            "minecraft:music_disc_blocks",
            "minecraft:music_disc_chirp",
            "minecraft:music_disc_far",
            "minecraft:music_disc_mall",
            "minecraft:music_disc_mellohi",
            "minecraft:music_disc_stal",
            "minecraft:music_disc_strad",
            "minecraft:music_disc_ward",
            "minecraft:music_disc_11",
            "minecraft:music_disc_wait",
            "minecraft:music_disc_pigstep",
            "minecraft:music_disc_otherside",
            "minecraft:music_disc_5",
            "minecraft:music_disc_relic"
    );

    /**
     * Mochila musical (Sophisticated Backpacks + Jukebox Upgrade).
     *
     * Body:
     *  - playerName (req)
     *  - advanced (bool) — jukebox_upgrade vs advanced_jukebox_upgrade
     *  - discType (string) — "etched" (requer mod de compat) | "vanilla" (sempre OK)
     *  - vanillaDisc (string, opcional, default music_disc_cat) — só se discType=vanilla
     *
     * NOTA: `discType=etched` só funciona se o mod
     * `sophisticatedbackpacksetchedintegration` estiver instalado no servidor +
     * clientes. Sem ele, o VanillaDiscHandler do SB faz `instanceof RecordItem`
     * e o disco Etched não passa.
     */
    @PostMapping("/discs/{id}/give-backpack")
    public Map<String, Object> giveBackpack(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EtchedDisc d = repo.findById(id).orElse(null);
        if (d == null) return Map.of("ok", false, "error", "disco não existe");
        String name = body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        boolean advanced = body.get("advanced") instanceof Boolean a ? a : false;
        String discType = body.getOrDefault("discType", "vanilla").toString().toLowerCase();
        String vanillaDisc = body.getOrDefault("vanillaDisc", "minecraft:music_disc_cat").toString();
        if (!VANILLA_DISCS.contains(vanillaDisc)) vanillaDisc = "minecraft:music_disc_cat";

        // 1. Backpack base
        mod.runCommand("give " + name + " sophisticatedbackpacks:backpack 1");
        // 2. Jukebox upgrade
        String upgradeId = advanced
                ? "sophisticatedbackpacks:advanced_jukebox_upgrade"
                : "sophisticatedbackpacks:jukebox_upgrade";
        mod.runCommand("give " + name + " " + upgradeId + " 1");

        String givenDisc;
        if ("etched".equals(discType)) {
            // Disco Etched JÁ gravado com NBT real (Music:{Url,Title,Author},
            // DiscColor, LabelColor, Pattern) — mesmo NBT que o EtchingMenu gera.
            // Requer o mod de compat `sophisticatedbackpacksetchedintegration`.
            String discSnbt = buildEtchedItemSnbt(d);
            mod.runCommand("give " + name + " " + discSnbt + " 1");
            givenDisc = "etched:etched_music_disc (com URL: " + d.getTitle() + ")";
        } else {
            // Disco vanilla — funciona em qualquer mochila SB sem mod de compat
            mod.runCommand("give " + name + " " + vanillaDisc + " 1");
            givenDisc = vanillaDisc.replace("minecraft:music_disc_", "");
        }

        // Tutorial no chat
        mod.runCommand("tellraw " + name + " [\"\"," +
                "{\"text\":\"🎒 Mochila Musical entregue!\\n\",\"color\":\"gold\",\"bold\":true}," +
                "{\"text\":\"1. RMB na mochila → arrasta o Jukebox Upgrade pro slot de upgrade\\n\",\"color\":\"white\"}," +
                "{\"text\":\"2. Coloca o disco no slot do upgrade\\n\",\"color\":\"white\"}," +
                "{\"text\":\"3. Equipa a mochila e anda → ela toca\\n\",\"color\":\"aqua\"}" +
                ("etched".equals(discType) ?
                ",{\"text\":\"\\n⚠ Disco Etched só toca na mochila se o mod 'sophisticatedbackpacksetchedintegration' estiver instalado no server + cliente.\",\"color\":\"yellow\",\"italic\":true}" :
                "") +
                "]");

        d.setPlayCount(d.getPlayCount() + 1);
        repo.save(d);
        return Map.of("ok", true,
                "note", "Entregue: backpack + " + upgradeId.replace("sophisticatedbackpacks:", "") +
                        " + " + givenDisc + ".");
    }

    /**
     * Alternativa SEM disco: dá um etched:portal_radio (bloco standalone que
     * stream URL direto). Player coloca, RMB pra abrir GUI, cola a URL.
     * Vantagem: zero NBT — o player configura in-game, então funciona 100%.
     */
    @PostMapping("/discs/{id}/give-radio")
    public Map<String, Object> giveRadio(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EtchedDisc d = repo.findById(id).orElse(null);
        if (d == null) return Map.of("ok", false, "error", "disco não existe");
        String name = body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");

        String urlValue = "READY".equals(d.getAudioStatus())
                ? backendPublicUrl.replaceAll("/+$", "") + "/api/etched/audio/" + d.getId() + ".mp3"
                : d.getUrl();
        // Display name com instruções pra player saber qual URL colar
        String displayName = "'{\"text\":\"📡 " + escapeForJson(d.getTitle()) +
                "\",\"italic\":false,\"color\":\"aqua\"}'";
        String loreUrl = "'{\"text\":\"URL: " + escapeForJson(urlValue.length() > 60 ?
                urlValue.substring(0, 60) + "..." : urlValue) +
                "\",\"italic\":true,\"color\":\"gray\"}'";
        String loreHint = "'{\"text\":\"Coloque no chão e RMB pra configurar\"," +
                "\"italic\":true,\"color\":\"yellow\"}'";
        String nbt = "{display:{Name:" + displayName + ",Lore:[" + loreUrl + "," + loreHint + "]}}";

        mod.runCommand("give " + name + " etched:portal_radio" + nbt + " 1");
        // Anuncia no chat com a URL pra player copiar/colar
        mod.runCommand("tellraw " + name + " [\"\"," +
                "{\"text\":\"📡 Portal Radio entregue. Cole essa URL no GUI do bloco:\\n\",\"color\":\"aqua\"}," +
                "{\"text\":\"" + escapeForJson(urlValue) + "\",\"color\":\"white\",\"underlined\":true,\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"" + escapeForJson(urlValue) + "\"}}," +
                "{\"text\":\"\\n(clica pra copiar)\",\"color\":\"gray\",\"italic\":true}]");
        d.setPlayCount(d.getPlayCount() + 1);
        repo.save(d);
        return Map.of("ok", true,
                "note", "Portal Radio entregue. URL enviada no chat — player clica pra copiar e cola no GUI do bloco.");
    }

    // =========================================================================
    // EXTRAÇÃO DE ÁUDIO (yt-dlp + ffmpeg)
    // =========================================================================

    /**
     * Dispara extração assíncrona de áudio via yt-dlp. Funciona com YouTube,
     * SoundCloud, Bandcamp, mp3 direto, etc. (qualquer site suportado pelo yt-dlp).
     *
     * O endpoint retorna imediatamente; status fica em "EXTRACTING" e atualiza
     * pra "READY" quando o mp3 ficar pronto em data/etched-audio/{id}.mp3.
     */
    @PostMapping("/discs/{id}/extract")
    public Map<String, Object> extract(@PathVariable Long id) {
        EtchedDisc d = repo.findById(id).orElse(null);
        if (d == null) return Map.of("ok", false, "error", "disco não existe");
        if (d.getUrl() == null || d.getUrl().isBlank())
            return Map.of("ok", false, "error", "url vazia");

        d.setAudioStatus("EXTRACTING");
        d.setAudioError(null);
        repo.save(d);
        runExtract(d.getId(), d.getUrl());
        return Map.of("ok", true, "status", "EXTRACTING");
    }

    @Async
    public void runExtract(Long id, String url) {
        try {
            Path dir = Paths.get(audioDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(id + ".mp3");
            Files.deleteIfExists(target);
            Path tmpl = dir.resolve(id + ".%(ext)s");

            // Tenta 3 estratégias diferentes — cada uma escapa de bloqueios YT distintos:
            //  1. android client (player_client=android) — funciona em ~80% dos casos sem cookies
            //  2. mweb/web_creator clients — alternativa quando android falha
            //  3. cookies.txt se admin subiu (último recurso)
            String[] strategies = {
                    // Estratégia 1: android client (mais leve, geralmente bypassa bot-check)
                    "--extractor-args=youtube:player_client=android",
                    // Estratégia 2: mweb + web_creator (clients que normalmente não exigem login)
                    "--extractor-args=youtube:player_client=mweb,web_creator,android_creator",
                    // Estratégia 3: tvhtml5 + web + cookies se houver
                    "--extractor-args=youtube:player_client=tvhtml5_simply_embedded,web"
            };
            Path cookies = Paths.get(audioDir, "yt-cookies.txt");
            boolean hasCookies = Files.exists(cookies);

            int lastExit = -1;
            String lastErr = "";
            for (int i = 0; i < strategies.length; i++) {
                List<String> args = new java.util.ArrayList<>(List.of(
                        "yt-dlp", "-x",
                        "--audio-format", "mp3",
                        "--audio-quality", "5",
                        "--no-playlist",
                        "--max-filesize", "50M",
                        "--no-warnings",
                        "--no-check-certificate",
                        strategies[i],
                        "-o", tmpl.toAbsolutePath().toString()
                ));
                if (hasCookies) {
                    args.add("--cookies");
                    args.add(cookies.toAbsolutePath().toString());
                }
                args.add(url);

                LOG.info("[Etched] disco {} tentativa {}/{}: client={}",
                        id, i + 1, strategies.length, strategies[i]);
                ProcessBuilder pb = new ProcessBuilder(args);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                StringBuilder log = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (log.length() < 4000) log.append(line).append("\n");
                    }
                }
                lastExit = p.waitFor();
                lastErr = log.toString();
                if (lastExit == 0 && Files.exists(target)) {
                    break; // sucesso, sai do loop
                }
                LOG.info("[Etched] disco {} estratégia {} falhou (exit={}), tentando próxima",
                        id, i + 1, lastExit);
                Files.deleteIfExists(target); // limpa lixo antes de tentar de novo
            }

            EtchedDisc d = repo.findById(id).orElse(null);
            if (d == null) return;
            if (lastExit == 0 && Files.exists(target)) {
                d.setAudioStatus("READY");
                d.setAudioSizeBytes(Files.size(target));
                d.setAudioError(null);
                LOG.info("[Etched] disco {} extraído com sucesso ({} bytes)", id, d.getAudioSizeBytes());
            } else {
                d.setAudioStatus("FAILED");
                String hint = !hasCookies && lastErr.contains("Sign in to confirm")
                        ? " | DICA: YouTube exigiu login. Suba cookies.txt via POST /api/etched/cookies"
                        : "";
                String err = lastErr + hint;
                d.setAudioError(err.length() > 500 ? err.substring(err.length() - 500) : err);
                LOG.warn("[Etched] disco {} todas as estratégias falharam (exit={}): {}",
                        id, lastExit, err);
            }
            repo.save(d);
        } catch (Exception e) {
            LOG.warn("[Etched] disco {} extract exception: {}", id, e.getMessage());
            EtchedDisc d = repo.findById(id).orElse(null);
            if (d != null) {
                d.setAudioStatus("FAILED");
                d.setAudioError(e.getMessage());
                repo.save(d);
            }
        }
    }

    /**
     * Upload de cookies.txt do YouTube. Quando o player-client=android também
     * falha (vídeos age-restricted ou region-locked), admin pode subir cookies
     * exportados do navegador (extensão "Get cookies.txt LOCALLY") aqui.
     *
     * Cookies ficam em /app/data/etched-audio/yt-cookies.txt e são automaticamente
     * usados em todas as extrações futuras.
     */
    @PostMapping(value = "/cookies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadCookies(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        if (file.isEmpty()) return Map.of("ok", false, "error", "arquivo vazio");
        Path dir = Paths.get(audioDir);
        Files.createDirectories(dir);
        Path target = dir.resolve("yt-cookies.txt");
        Files.write(target, file.getBytes());
        LOG.info("[Etched] cookies.txt atualizado ({} bytes)", file.getSize());
        return Map.of("ok", true, "size", file.getSize());
    }

    /** Status do cookies.txt — existe? quando foi subido? */
    @GetMapping("/cookies/status")
    public Map<String, Object> cookiesStatus() throws java.io.IOException {
        Path cookies = Paths.get(audioDir, "yt-cookies.txt");
        if (!Files.exists(cookies)) return Map.of("present", false);
        return Map.of("present", true,
                "size", Files.size(cookies),
                "lastModified", Files.getLastModifiedTime(cookies).toMillis());
    }

    @DeleteMapping("/cookies")
    public Map<String, Object> deleteCookies() throws java.io.IOException {
        Path cookies = Paths.get(audioDir, "yt-cookies.txt");
        Files.deleteIfExists(cookies);
        return Map.of("ok", true);
    }

    /**
     * Serve o mp3 extraído pra o cliente Etched baixar. PÚBLICO — qualquer um
     * que tenha o ID pode baixar (Etched client não passa auth header).
     * Path com extensão `.mp3` pra Etched reconhecer o tipo de conteúdo.
     */
    @GetMapping("/audio/{id}.mp3")
    public ResponseEntity<?> audio(@PathVariable Long id) {
        Path f = Paths.get(audioDir).resolve(id + ".mp3");
        if (!Files.exists(f)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(new FileSystemResource(f));
    }

    /**
     * Upload manual de áudio — alternativa à extração via yt-dlp. Admin pode
     * subir mp3/wav/ogg/m4a/flac/opus direto do desktop. Se for mp3, é copiado
     * direto; outros formatos são convertidos pra mp3 via ffmpeg.
     *
     * Limit 50MB. Substitui qualquer mp3 anterior do disco (incluindo extraído).
     */
    @PostMapping(value = "/discs/{id}/upload-audio",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAudio(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        EtchedDisc d = repo.findById(id).orElse(null);
        if (d == null) return Map.of("ok", false, "error", "disco não existe");
        if (file.isEmpty()) return Map.of("ok", false, "error", "arquivo vazio");
        if (file.getSize() > 50L * 1024 * 1024)
            return Map.of("ok", false, "error", "arquivo > 50MB");

        try {
            Path dir = Paths.get(audioDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(id + ".mp3");

            // Detecta extensão pelo content-type ou original filename
            String fname = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            boolean isMp3 = fname.endsWith(".mp3") ||
                    "audio/mpeg".equalsIgnoreCase(file.getContentType());

            if (isMp3) {
                // mp3 direto — copia raw
                Files.write(target, file.getBytes());
                LOG.info("[Etched] disco {} mp3 upload direto ({} bytes)", id, file.getSize());
            } else {
                // Outros formatos — converte com ffmpeg pra mp3
                Path tmp = dir.resolve("upload_" + id + "_" + System.currentTimeMillis() + getExt(fname));
                Files.write(tmp, file.getBytes());
                try {
                    Files.deleteIfExists(target);
                    ProcessBuilder pb = new ProcessBuilder(
                            "ffmpeg", "-y", "-loglevel", "error",
                            "-i", tmp.toAbsolutePath().toString(),
                            "-vn", // sem vídeo
                            "-acodec", "libmp3lame",
                            "-q:a", "5",
                            target.toAbsolutePath().toString()
                    );
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    StringBuilder log = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (log.length() < 2000) log.append(line).append("\n");
                        }
                    }
                    int exit = p.waitFor();
                    if (exit != 0 || !Files.exists(target)) {
                        d.setAudioStatus("FAILED");
                        d.setAudioError("ffmpeg convert failed: " + log);
                        repo.save(d);
                        return Map.of("ok", false, "error", "conversão falhou: " + log);
                    }
                    LOG.info("[Etched] disco {} convertido de {} → mp3 ({} bytes)",
                            id, fname, Files.size(target));
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }

            d.setAudioStatus("READY");
            d.setAudioSizeBytes(Files.size(target));
            d.setAudioError(null);
            repo.save(d);
            return Map.of("ok", true, "size", Files.size(target),
                    "audioUrl", "/api/etched/audio/" + id + ".mp3");
        } catch (Exception e) {
            LOG.warn("[Etched] disco {} upload-audio falhou: {}", id, e.getMessage());
            d.setAudioStatus("FAILED");
            d.setAudioError(e.getMessage());
            repo.save(d);
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    private static String getExt(String fname) {
        int dot = fname.lastIndexOf('.');
        return dot < 0 ? ".bin" : fname.substring(dot);
    }

    // Helpers de string SNBT
    private static String jsonString(String s) {
        return "\"" + escapeForJson(s == null ? "" : s) + "\"";
    }
    private static String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
