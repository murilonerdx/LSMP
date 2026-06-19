package br.com.murilo.liberthia.admin.engine.structures;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Catálogo de estruturas dos mods (DungeonsArise, YungsBetter*, Repurposed,
 * TwilightForest, Stalwart). Usa /locate vanilla — funciona em qualquer
 * estrutura registrada no servidor sem precisar mexer no mod.
 *
 * Comandos:
 *  /locate structure {id}       → retorna coords (output no chat do executor)
 *  /tp {admin} {x} {y} {z}      → admin vai pra estrutura encontrada
 */
@RestController
@RequestMapping("/api/structures")
public class StructuresController {

    /** Catálogo agrupado por mod-source. */
    private static final List<Map<String, Object>> CATALOG = List.of(
            modGroup("DungeonsArise", "⚔", List.of(
                    "dungeons_arise:abandoned_temple",
                    "dungeons_arise:aviary",
                    "dungeons_arise:bandit_towers",
                    "dungeons_arise:bandit_village",
                    "dungeons_arise:coliseum",
                    "dungeons_arise:foundry",
                    "dungeons_arise:greenwood_pub",
                    "dungeons_arise:heavenly_challenger",
                    "dungeons_arise:heavenly_conqueror",
                    "dungeons_arise:heavenly_rider",
                    "dungeons_arise:illager_campsite",
                    "dungeons_arise:illager_corsair",
                    "dungeons_arise:illager_galley",
                    "dungeons_arise:infested_temple",
                    "dungeons_arise:jungle_pyramid",
                    "dungeons_arise:keep_kayra",
                    "dungeons_arise:lighthouse",
                    "dungeons_arise:mechanical_nest",
                    "dungeons_arise:mineshaft",
                    "dungeons_arise:mining_system",
                    "dungeons_arise:mushroom_house",
                    "dungeons_arise:plague_asylum",
                    "dungeons_arise:scorched_mines",
                    "dungeons_arise:shiraz_palace",
                    "dungeons_arise:small_blimp",
                    "dungeons_arise:thornborn_towers",
                    "dungeons_arise:typhon",
                    "dungeons_arise:undead_pirate_ship"
            )),
            modGroup("StalwartDungeons", "🏰", List.of(
                    "stalwart_dungeons:bandit_camp",
                    "stalwart_dungeons:guard_tower",
                    "stalwart_dungeons:mage_tower",
                    "stalwart_dungeons:obsidian_castle",
                    "stalwart_dungeons:plague_tower",
                    "stalwart_dungeons:plains_castle",
                    "stalwart_dungeons:savanna_castle"
            )),
            modGroup("YungsBetter", "🪨", List.of(
                    "yungsbetterdungeons:better_dungeon",
                    "yungsbetterstrongholds:better_stronghold",
                    "yungsbetterdeserttemples:better_desert_temple",
                    "yungsbetterjungletemples:better_jungle_temple",
                    "yungsbetteroceanmonuments:better_ocean_monument",
                    "yungsbetternetherfortresses:better_fortress",
                    "yungsbetterwitchhuts:better_witch_hut",
                    "yungsbridges:wooden_bridge",
                    "yungsbridges:stone_bridge"
            )),
            modGroup("RepurposedStructures", "🏛", List.of(
                    "repurposed_structures:mansions/badlands",
                    "repurposed_structures:mansions/birch",
                    "repurposed_structures:mansions/desert",
                    "repurposed_structures:mansions/jungle",
                    "repurposed_structures:mansions/oak",
                    "repurposed_structures:villages/badlands",
                    "repurposed_structures:villages/birch_oak",
                    "repurposed_structures:villages/crimson",
                    "repurposed_structures:villages/dark_oak",
                    "repurposed_structures:villages/giant_taiga",
                    "repurposed_structures:villages/jungle",
                    "repurposed_structures:villages/mountains",
                    "repurposed_structures:villages/swamp",
                    "repurposed_structures:villages/warped",
                    "repurposed_structures:fortresses/jungle",
                    "repurposed_structures:fortresses/nether_brick",
                    "repurposed_structures:igloos/grassy",
                    "repurposed_structures:igloos/stone",
                    "repurposed_structures:outposts/icy",
                    "repurposed_structures:outposts/desert",
                    "repurposed_structures:strongholds/nether",
                    "repurposed_structures:temples/end",
                    "repurposed_structures:temples/snowy"
            )),
            modGroup("TwilightForest", "🌲", List.of(
                    "twilightforest:naga_courtyard",
                    "twilightforest:lich_tower",
                    "twilightforest:knight_stronghold",
                    "twilightforest:hydra_lair",
                    "twilightforest:hollow_hill_small",
                    "twilightforest:hollow_hill_medium",
                    "twilightforest:hollow_hill_large",
                    "twilightforest:dark_tower",
                    "twilightforest:final_castle",
                    "twilightforest:goblin_stronghold",
                    "twilightforest:labyrinth",
                    "twilightforest:troll_cave",
                    "twilightforest:yeti_cave",
                    "twilightforest:hedge_maze",
                    "twilightforest:quest_grove",
                    "twilightforest:enchanted_forest",
                    "twilightforest:mushroom_tower",
                    "twilightforest:graveyard"
            )),
            modGroup("Vanilla", "⛏", List.of(
                    "minecraft:village_plains",
                    "minecraft:village_desert",
                    "minecraft:village_savanna",
                    "minecraft:village_snowy",
                    "minecraft:village_taiga",
                    "minecraft:stronghold",
                    "minecraft:monument",
                    "minecraft:mansion",
                    "minecraft:fortress",
                    "minecraft:bastion_remnant",
                    "minecraft:end_city",
                    "minecraft:ancient_city",
                    "minecraft:trail_ruins",
                    "minecraft:trial_chambers",
                    "minecraft:pillager_outpost",
                    "minecraft:ruined_portal",
                    "minecraft:shipwreck",
                    "minecraft:buried_treasure"
            ))
    );

    private static Map<String, Object> modGroup(String name, String emoji, List<String> ids) {
        return Map.of("name", name, "emoji", emoji, "count", ids.size(), "structures", ids);
    }

    private final ModBridgeClient mod;

    public StructuresController(ModBridgeClient mod) {
        this.mod = mod;
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        int total = CATALOG.stream().mapToInt(g -> ((List<?>) g.get("structures")).size()).sum();
        return Map.of("groups", CATALOG, "total", total);
    }

    /**
     * Dispara /locate. O comando manda o output pro executor do command — não temos
     * retorno direto, então admin precisa ler o chat do servidor. Painel só dispara
     * e mostra confirmação.
     */
    @PostMapping("/locate")
    public Map<String, Object> locate(@RequestBody Map<String, String> body) {
        String structureId = body.getOrDefault("structureId", "");
        String origin = body.getOrDefault("origin", "");
        if (structureId.isBlank())
            return Map.of("ok", false, "error", "structureId missing");

        // Se origin passada (player ou coord), usa execute as / execute at
        String cmd;
        if (!origin.isBlank()) {
            cmd = "execute as " + origin + " at @s run locate structure " + structureId;
        } else {
            cmd = "locate structure " + structureId;
        }
        mod.runCommand(cmd);
        return Map.of("ok", true, "cmd", cmd,
                "note", "Output aparece no chat do executor. Veja no console do servidor pra coords.");
    }

    /** TP admin pra coord conhecida. */
    @PostMapping("/teleport")
    public Map<String, Object> teleport(@RequestBody Map<String, Object> body) {
        String name = body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        double x = num(body.get("x"), 0);
        double y = num(body.get("y"), 64);
        double z = num(body.get("z"), 0);
        String dim = body.getOrDefault("dimension", "minecraft:overworld").toString();
        String cmd = String.format(Locale.US, "execute in %s run tp %s %f %f %f", dim, name, x, y, z);
        mod.runCommand(cmd);
        return Map.of("ok", true);
    }

    private static double num(Object o, double d) {
        return o instanceof Number n ? n.doubleValue() : d;
    }
}
