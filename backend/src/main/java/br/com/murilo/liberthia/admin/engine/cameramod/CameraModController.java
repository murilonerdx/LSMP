package br.com.murilo.liberthia.admin.engine.cameramod;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Painel pro mod "Camera" (camera-forge by Henkelmax) — distinto do Exposure.
 *
 * Camera salva fotos CLIENT-SIDE (não vão pro servidor), então integração é
 * limitada. O que dá pra fazer via comando vanilla:
 *
 *  - /give câmera + filmes pra player (com NBT de "preset")
 *  - Forçar 3rd-person ou modo cinematic via /camera (do mod) ou /title
 *  - Spawnar "photo prop" entities pra eventos
 *
 * Sem mexer no source do mod, isso aqui é o máximo administrativo.
 */
@RestController
@RequestMapping("/api/camera-mod")
public class CameraModController {

    /** Items conhecidos do camera-forge 1.0.19 (1.20.1). Pode ajustar conforme versão. */
    private static final List<Map<String, String>> CAMERA_ITEMS = List.of(
            Map.of("id", "camera:camera", "label", "📷 Câmera"),
            Map.of("id", "camera:photograph", "label", "🖼 Fotografia em branco"),
            Map.of("id", "camera:album", "label", "📚 Álbum")
    );

    private final ModBridgeClient mod;

    public CameraModController(ModBridgeClient mod) {
        this.mod = mod;
    }

    @GetMapping("/items")
    public Map<String, Object> items() {
        return Map.of("items", CAMERA_ITEMS);
    }

    /** Dá kit de câmera + álbum pra um player. */
    @PostMapping("/give-kit")
    public Map<String, Object> giveKit(@RequestBody Map<String, Object> body) {
        String name = body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        boolean withAlbum = (boolean) body.getOrDefault("withAlbum", true);
        int photoCount = body.get("photoCount") instanceof Number n ? n.intValue() : 16;

        mod.runCommand("give " + name + " camera:camera 1");
        if (photoCount > 0) mod.runCommand("give " + name + " camera:photograph " + photoCount);
        if (withAlbum) mod.runCommand("give " + name + " camera:album 1");
        return Map.of("ok", true);
    }

    /** Ativa modo "cinematic 3rd-person" forçado via camera angle. Útil pra cutscene improvisada. */
    @PostMapping("/cinematic")
    public Map<String, Object> cinematic(@RequestBody Map<String, Object> body) {
        String name = body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        boolean enable = (boolean) body.getOrDefault("enable", true);
        if (enable) {
            mod.runCommand("title " + name + " times 20 60 20");
            mod.runCommand("title " + name + " title {\"text\":\"\"}");
            mod.runCommand("execute as " + name + " run gamemode spectator");
        } else {
            mod.runCommand("execute as " + name + " run gamemode survival");
        }
        return Map.of("ok", true);
    }

    /** TP o admin pra "POV" de um player (gamemode spectator + tp). */
    @PostMapping("/pov")
    public Map<String, Object> pov(@RequestBody Map<String, Object> body) {
        String admin = body.getOrDefault("adminName", "").toString();
        String target = body.getOrDefault("targetName", "").toString();
        if (admin.isBlank() || target.isBlank())
            return Map.of("ok", false, "error", "adminName e targetName obrigatórios");
        mod.runCommand("execute as " + admin + " run gamemode spectator");
        mod.runCommand("execute as " + admin + " run spectate " + target);
        return Map.of("ok", true);
    }

    /** Sai do POV (spectator → survival). */
    @PostMapping("/pov-exit")
    public Map<String, Object> povExit(@RequestBody Map<String, Object> body) {
        String name = body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        mod.runCommand("spectate"); // sem args = sai
        mod.runCommand("execute as " + name + " run gamemode survival");
        return Map.of("ok", true);
    }
}
