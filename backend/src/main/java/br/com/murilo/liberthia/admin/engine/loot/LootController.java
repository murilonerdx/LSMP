package br.com.murilo.liberthia.admin.engine.loot;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/loot")
public class LootController {

    /** Weight default sugerido por rarity. */
    public static final Map<String, Integer> DEFAULT_WEIGHTS = Map.of(
            "common", 50, "uncommon", 25, "rare", 10,
            "epic", 4, "legendary", 1, "mythic", 1
    );

    private final LootItemRepository repo;
    private final ModBridgeClient mod;

    public LootController(LootItemRepository repo, ModBridgeClient mod) {
        this.repo = repo;
        this.mod = mod;
    }

    // ======================== CRUD ========================
    @GetMapping("/items")
    public List<LootItem> list(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String rarity) {
        if (category != null && !category.isBlank())
            return repo.findByEnabledTrueAndCategory(category);
        if (rarity != null && !rarity.isBlank())
            return repo.findByEnabledTrueAndRarity(rarity);
        return repo.findAllByOrderByRarityAscNameAsc();
    }

    @PostMapping("/items")
    public LootItem create(@RequestBody LootItem item) {
        if (item.getName() == null || item.getName().isBlank())
            throw new IllegalArgumentException("name obrigatório");
        if (item.getItemId() == null || item.getItemId().isBlank())
            throw new IllegalArgumentException("itemId obrigatório");
        if (item.getWeight() <= 0) item.setWeight(DEFAULT_WEIGHTS.getOrDefault(item.getRarity(), 10));
        return repo.save(item);
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<LootItem> update(@PathVariable Long id, @RequestBody LootItem body) {
        return repo.findById(id).map(it -> {
            if (body.getName() != null) it.setName(body.getName());
            if (body.getItemId() != null) it.setItemId(body.getItemId());
            if (body.getNbt() != null) it.setNbt(body.getNbt());
            if (body.getRarity() != null) it.setRarity(body.getRarity());
            if (body.getCategory() != null) it.setCategory(body.getCategory());
            if (body.getDescription() != null) it.setDescription(body.getDescription());
            it.setCount(Math.max(1, body.getCount()));
            it.setWeight(Math.max(1, body.getWeight()));
            it.setEnabled(body.isEnabled());
            return ResponseEntity.ok(repo.save(it));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/items/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("ok", true);
    }

    // ======================== DRAW (sorteio) ========================

    /** Sorteia 1 item ponderado pelo weight. Filtros opcionais: category, rarity. */
    @PostMapping("/draw")
    public Map<String, Object> draw(@RequestBody(required = false) Map<String, Object> body) {
        String category = body == null ? null : (String) body.get("category");
        String rarity = body == null ? null : (String) body.get("rarity");
        LootItem picked = pickWeighted(category, rarity);
        if (picked == null) return Map.of("ok", false, "error", "loot table vazia");
        picked.setDrawCount(picked.getDrawCount() + 1);
        repo.save(picked);
        return Map.of("ok", true, "item", picked);
    }

    /** Sorteia 1 item E dá pro player. */
    @PostMapping("/give")
    public Map<String, Object> give(@RequestBody Map<String, Object> body) {
        String name = body == null ? "" : body.getOrDefault("playerName", "").toString();
        if (name.isBlank()) return Map.of("ok", false, "error", "playerName missing");
        String category = body == null ? null : (String) body.get("category");
        String rarity = body == null ? null : (String) body.get("rarity");

        LootItem picked = pickWeighted(category, rarity);
        if (picked == null) return Map.of("ok", false, "error", "loot table vazia");

        String giveCmd = "give " + name + " " + picked.getItemId()
                + (picked.getNbt() == null || picked.getNbt().isBlank() ? "" : picked.getNbt())
                + " " + picked.getCount();
        mod.runCommand(giveCmd);

        // Broadcast colorido pelo rarity
        String color = colorForRarity(picked.getRarity());
        mod.runCommand("tellraw " + name + " [\"\"," +
                "{\"text\":\"🎁 Você ganhou: \",\"color\":\"gold\"}," +
                "{\"text\":\"" + escape(picked.getName()) + "\",\"color\":\"" + color + "\",\"bold\":true}," +
                "{\"text\":\" (" + picked.getRarity() + ")\",\"color\":\"gray\",\"italic\":true}]");

        picked.setDrawCount(picked.getDrawCount() + 1);
        repo.save(picked);
        return Map.of("ok", true, "item", picked, "cmd", giveCmd);
    }

    private LootItem pickWeighted(String category, String rarity) {
        List<LootItem> pool = repo.findByEnabledTrue().stream()
                .filter(it -> category == null || category.isBlank() || category.equalsIgnoreCase(it.getCategory()))
                .filter(it -> rarity == null || rarity.isBlank() || rarity.equalsIgnoreCase(it.getRarity()))
                .toList();
        if (pool.isEmpty()) return null;
        long totalWeight = pool.stream().mapToLong(LootItem::getWeight).sum();
        if (totalWeight <= 0) return pool.get(0);
        long pick = ThreadLocalRandom.current().nextLong(totalWeight);
        long acc = 0;
        for (LootItem it : pool) {
            acc += it.getWeight();
            if (pick < acc) return it;
        }
        return pool.get(pool.size() - 1);
    }

    private String colorForRarity(String rarity) {
        return switch (rarity == null ? "" : rarity.toLowerCase()) {
            case "uncommon" -> "green";
            case "rare" -> "aqua";
            case "epic" -> "light_purple";
            case "legendary" -> "gold";
            case "mythic" -> "red";
            default -> "white";
        };
    }
    private String escape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }

    // ======================== Preview / Stats ========================

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<LootItem> all = repo.findByEnabledTrue();
        Map<String, Integer> byRarity = new HashMap<>();
        Map<String, Integer> byCategory = new HashMap<>();
        long totalWeight = 0;
        for (LootItem it : all) {
            byRarity.merge(it.getRarity(), 1, Integer::sum);
            byCategory.merge(it.getCategory(), 1, Integer::sum);
            totalWeight += it.getWeight();
        }
        return Map.of(
                "total", all.size(),
                "totalWeight", totalWeight,
                "byRarity", byRarity,
                "byCategory", byCategory
        );
    }

    /** Simula N sorteios pra mostrar a distribuição real. */
    @GetMapping("/simulate")
    public Map<String, Object> simulate(@RequestParam(defaultValue = "100") int n) {
        Map<String, Integer> hits = new HashMap<>();
        Map<String, Integer> rarityHits = new HashMap<>();
        for (int i = 0; i < Math.min(10000, Math.max(1, n)); i++) {
            LootItem picked = pickWeighted(null, null);
            if (picked == null) break;
            hits.merge(picked.getName(), 1, Integer::sum);
            rarityHits.merge(picked.getRarity(), 1, Integer::sum);
        }
        return Map.of("n", n, "hits", hits, "rarityHits", rarityHits);
    }

    // ======================== Presets ========================

    /**
     * Items pré-cadastrados famosos com raridades sensatas. Admin clica
     * "import all" e a loot table fica populada com 30+ items vanilla bons.
     */
    @GetMapping("/presets")
    public List<Map<String, Object>> presets() {
        return List.of(
                // ===== COMMON (50w) — items úteis básicos =====
                preset("Pão", "minecraft:bread", 4, "common", "food", "Comida básica"),
                preset("Maçã", "minecraft:apple", 3, "common", "food", "Recupera fome"),
                preset("Tocha", "minecraft:torch", 16, "common", "misc", "Iluminação básica"),
                preset("Ferro", "minecraft:iron_ingot", 5, "common", "currency", "Mineral comum"),
                preset("Carvão", "minecraft:coal", 16, "common", "currency", "Combustível"),
                preset("Lã", "minecraft:white_wool", 8, "common", "decoration", "Construção"),
                preset("Corda", "minecraft:string", 8, "common", "misc", "Crafting"),
                preset("Flecha", "minecraft:arrow", 16, "common", "tool", "Munição"),
                preset("Ovo", "minecraft:egg", 4, "common", "food", "Lança/cozinha"),

                // ===== UNCOMMON (25w) — items úteis avançados =====
                preset("Ouro", "minecraft:gold_ingot", 3, "uncommon", "currency", "Mineral valioso"),
                preset("Lápis-Lazúli", "minecraft:lapis_lazuli", 8, "uncommon", "magic", "Encantamento"),
                preset("Pólvora", "minecraft:gunpowder", 8, "uncommon", "misc", "Crafting bombas"),
                preset("Pão fortificado", "minecraft:golden_apple", 1, "uncommon", "food", "Maçã dourada"),
                preset("Garrafa de XP", "minecraft:experience_bottle", 5, "uncommon", "magic", "Ganha XP"),
                preset("Cogumelo Brilhante", "minecraft:glowstone_dust", 8, "uncommon", "magic", "Poções"),
                preset("Cana", "minecraft:sugar_cane", 16, "uncommon", "misc", "Papel/açúcar"),

                // ===== RARE (10w) — items poderosos =====
                preset("Diamante", "minecraft:diamond", 2, "rare", "currency", "Item raro"),
                preset("Esmeralda", "minecraft:emerald", 3, "rare", "currency", "Moeda dos villagers"),
                preset("Mecha", "minecraft:nautilus_shell", 1, "rare", "magic", "Conduit"),
                preset("Cristal do End", "minecraft:end_crystal", 1, "rare", "magic", "Cristal energético"),
                preset("Olho de Ender", "minecraft:ender_eye", 4, "rare", "magic", "Acha stronghold"),
                preset("Espada de Diamante", "minecraft:diamond_sword", 1, "rare", "gear",
                        "Espada de diamante encantada",
                        "{Enchantments:[{id:\"minecraft:sharpness\",lvl:3s}]}"),
                preset("Picareta de Diamante", "minecraft:diamond_pickaxe", 1, "rare", "tool",
                        "Picareta com Efficiency",
                        "{Enchantments:[{id:\"minecraft:efficiency\",lvl:3s}]}"),

                // ===== EPIC (4w) — items muito raros =====
                preset("Lingote de Netherite", "minecraft:netherite_ingot", 1, "epic", "currency",
                        "Material end-game"),
                preset("Coração do Mar", "minecraft:heart_of_the_sea", 1, "epic", "magic",
                        "Centro do conduit"),
                preset("Cabaça do Totem", "minecraft:totem_of_undying", 1, "epic", "magic",
                        "Salva da morte uma vez"),
                preset("Maçã Dourada Encantada", "minecraft:enchanted_golden_apple", 1, "epic", "food",
                        "Buffs extremos por 2 min"),
                preset("Espada de Netherite Aguçada", "minecraft:netherite_sword", 1, "epic", "gear",
                        "Espada brutal com Sharp V",
                        "{Enchantments:[{id:\"minecraft:sharpness\",lvl:5s},{id:\"minecraft:unbreaking\",lvl:3s}]}"),
                preset("Armadura Netherite", "minecraft:netherite_chestplate", 1, "epic", "gear",
                        "Peitoral com Prot IV",
                        "{Enchantments:[{id:\"minecraft:protection\",lvl:4s},{id:\"minecraft:unbreaking\",lvl:3s}]}"),
                preset("Trident", "minecraft:trident", 1, "epic", "gear",
                        "Trident com Loyalty + Channeling",
                        "{Enchantments:[{id:\"minecraft:loyalty\",lvl:3s},{id:\"minecraft:channeling\",lvl:1s}]}"),

                // ===== LEGENDARY (1w) — itens lendários =====
                preset("Ovo de Dragão", "minecraft:dragon_egg", 1, "legendary", "special",
                        "Troféu do Ender Dragon"),
                preset("Elytra", "minecraft:elytra", 1, "legendary", "gear",
                        "Asas pra voar"),
                preset("Cabeça do Wither", "minecraft:wither_skeleton_skull", 1, "legendary", "special",
                        "Crafting do Wither"),
                preset("Picareta de Netherite Lendária", "minecraft:netherite_pickaxe", 1, "legendary", "tool",
                        "Picareta com tudo: Fortune III + Efficiency V + Unbreaking III",
                        "{Enchantments:[{id:\"minecraft:fortune\",lvl:3s},{id:\"minecraft:efficiency\",lvl:5s},{id:\"minecraft:unbreaking\",lvl:3s}],display:{Name:'{\"text\":\"⚒ Picareta Lendária\",\"color\":\"gold\",\"italic\":false}'}}"),
                preset("Espada Mata-Dragão", "minecraft:netherite_sword", 1, "legendary", "gear",
                        "Espada lendária com TUDO encantado",
                        "{Enchantments:[{id:\"minecraft:sharpness\",lvl:5s},{id:\"minecraft:fire_aspect\",lvl:2s},{id:\"minecraft:looting\",lvl:3s},{id:\"minecraft:unbreaking\",lvl:3s},{id:\"minecraft:mending\",lvl:1s}],display:{Name:'{\"text\":\"⚔ Espada Mata-Dragão\",\"color\":\"red\",\"italic\":false,\"bold\":true}'}}"),

                // ===== MYTHIC (1w) — easter eggs =====
                preset("Beacon", "minecraft:beacon", 1, "mythic", "special",
                        "Item raríssimo, dá buff de área"),
                preset("Nether Star", "minecraft:nether_star", 1, "mythic", "special",
                        "Drop do Wither"),

                // ============================================================
                // ITEMS DE MODS — só são criados se o mod estiver instalado.
                // Se o id não existir no runtime, o give vira ar (no-op).
                // ============================================================

                // ===== Numismatics (moeda Crown — usado pelo Liberthia) =====
                preset("Crown", "numismatics:crown", 5, "uncommon", "currency",
                        "Moeda do servidor (Numismatics mod)"),
                preset("Sun Gold", "numismatics:sun", 1, "rare", "currency",
                        "Cunhagem dourada — alta denominação"),

                // ===== Liberthia (este mod) =====
                preset("Voice Modulator", "liberthia:voice_modulator", 1, "epic", "magic",
                        "Item do Liberthia: modula voz de quem segura"),
                preset("Dark Matter Block", "liberthia:dark_matter_block", 4, "rare", "magic",
                        "Bloco DM puro — útil em rituais"),
                preset("White Matter Block", "liberthia:white_matter_block", 4, "rare", "magic",
                        "Bloco WM puro — bênção em rituais"),
                preset("Matter Analyzer", "liberthia:matter_analyzer", 1, "rare", "tool",
                        "Bloco que analisa matéria de quem pisar perto"),
                preset("Dimensional Compass", "liberthia:dimensional_compass", 1, "rare", "tool",
                        "Aponta pra fenda dimensional mais próxima"),
                preset("Dimensional Extractor", "liberthia:dimensional_extractor", 1, "epic", "tool",
                        "Extrai 'baldes' de matéria de fendas"),

                // ===== Create =====
                preset("Andesite Alloy", "create:andesite_alloy", 8, "common", "currency",
                        "Liga básica do Create — toda construção"),
                preset("Brass Ingot", "create:brass_ingot", 4, "uncommon", "currency",
                        "Latão — components avançados do Create"),
                preset("Cogwheel", "create:cogwheel", 4, "uncommon", "tool",
                        "Engrenagem motriz do Create"),
                preset("Mechanical Press", "create:mechanical_press", 1, "rare", "tool",
                        "Prensa mecânica — comprime items"),
                preset("Train Track", "create:track", 16, "uncommon", "tool",
                        "Trilho de trem (Create Trains)"),

                // ===== Tinkers' Construct (legacy id) =====
                preset("Manyullyn Ingot", "tconstruct:manyullyn_ingot", 1, "epic", "currency",
                        "Liga lendária de Tinkers (cobalt+ardite)"),
                preset("Smeltery Controller", "tconstruct:smeltery_controller", 1, "rare", "tool",
                        "Controle da fundição"),
                preset("Tinker's Sword Blade Pattern", "tconstruct:pattern", 8, "uncommon", "tool",
                        "Padrão pra forjar lâmina"),

                // ===== Forbidden & Arcanus =====
                preset("Eternal Stella", "forbidden_arcanus:eternal_stella", 1, "legendary", "magic",
                        "Pedra eterna — combustível mágico"),
                preset("Soulless Stone", "forbidden_arcanus:soulless_stone", 4, "rare", "magic",
                        "Pedra sem alma — ritual"),
                preset("Awakening Brewer", "forbidden_arcanus:hephaestus_forge", 1, "epic", "tool",
                        "Forja para items mágicos"),

                // ===== Twilight Forest =====
                preset("Naga Trophy", "twilightforest:naga_trophy", 1, "epic", "special",
                        "Troféu do Naga — primeira boss"),
                preset("Lich Trophy", "twilightforest:lich_trophy", 1, "legendary", "special",
                        "Troféu do Lich — boss avançada"),
                preset("Magic Map", "twilightforest:magic_map", 1, "uncommon", "tool",
                        "Mapa que mostra estruturas mágicas"),
                preset("Ironwood Ingot", "twilightforest:ironwood_ingot", 3, "uncommon", "currency",
                        "Material twilight pra armaduras"),

                // ===== Aquaculture =====
                preset("Neptunium Ingot", "aquaculture:neptunium_ingot", 2, "rare", "currency",
                        "Liga aquática rara — armadura sub"),
                preset("Tackle Box", "aquaculture:tackle_box", 1, "uncommon", "tool",
                        "Caixa que guarda iscas/varas"),

                // ===== Alex's Mobs =====
                preset("Roadrunner Feather", "alexsmobs:roadrunner_feather", 4, "uncommon", "magic",
                        "Pena rara — encanta botas de velocidade"),
                preset("Mimicream", "alexsmobs:mimicream", 2, "rare", "magic",
                        "Creme do Mimicube — duplica items"),
                preset("Soul Heart", "alexsmobs:soul_heart", 1, "epic", "magic",
                        "Coração da alma — aumenta vida"),

                // ===== Ars Nouveau =====
                preset("Source Gem", "ars_nouveau:source_gem", 8, "uncommon", "magic",
                        "Cristal fonte — magia Ars"),
                preset("Spell Parchment", "ars_nouveau:spell_parchment", 4, "uncommon", "tool",
                        "Pergaminho pra escrever feitiços"),
                preset("Mage Tome", "ars_nouveau:mage_block", 1, "rare", "magic",
                        "Tomo do mago — base de feitiços"),

                // ===== Ice and Fire =====
                preset("Dragon Scale (Fire)", "iceandfire:fire_dragonscales", 4, "rare", "currency",
                        "Escama de dragão de fogo"),
                preset("Dragonsteel Fire Sword", "iceandfire:dragonsteel_fire_sword", 1, "legendary", "gear",
                        "Espada do dragão de fogo — endgame"),
                preset("Sea Serpent Scale", "iceandfire:sea_serpent_scales_blue", 4, "rare", "currency",
                        "Escama de serpente marinha"),

                // ===== Supplementaries =====
                preset("Soap", "supplementaries:soap", 4, "common", "misc",
                        "Sabão — limpa banners"),
                preset("Globe", "supplementaries:globe", 1, "rare", "decoration",
                        "Globo decorativo (artefato)"),
                preset("Pulley Block", "supplementaries:pulley_block", 1, "uncommon", "tool",
                        "Polia — sobe e desce items"),

                // ===== Sophisticated Backpacks =====
                preset("Iron Backpack", "sophisticatedbackpacks:iron_backpack", 1, "uncommon", "tool",
                        "Mochila de ferro — 27 slots"),
                preset("Netherite Backpack", "sophisticatedbackpacks:netherite_backpack", 1, "epic", "tool",
                        "Mochila netherite — 144 slots"),

                // ===== Blood Magic =====
                preset("Weak Blood Orb", "bloodmagic:weakbloodorb", 1, "uncommon", "magic",
                        "Orbe de sangue inicial — Blood Magic"),
                preset("Master Blood Orb", "bloodmagic:masterbloodorb", 1, "legendary", "magic",
                        "Orbe master — endgame Blood Magic"),

                // ===== Immersive Engineering =====
                preset("Steel Ingot", "immersiveengineering:ingot_steel", 4, "uncommon", "currency",
                        "Aço — tier 2 Immersive Eng"),
                preset("Revolver", "immersiveengineering:revolver", 1, "rare", "gear",
                        "Revolver vintage de IE")
        );
    }

    private Map<String, Object> preset(String name, String id, int count, String rarity, String category, String desc) {
        return preset(name, id, count, rarity, category, desc, null);
    }
    private Map<String, Object> preset(String name, String id, int count, String rarity, String category, String desc, String nbt) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", name);
        p.put("itemId", id);
        p.put("count", count);
        p.put("rarity", rarity);
        p.put("category", category);
        p.put("description", desc);
        p.put("weight", DEFAULT_WEIGHTS.getOrDefault(rarity, 10));
        if (nbt != null) p.put("nbt", nbt);
        p.put("enabled", true);
        return p;
    }

    /** Importa todos os presets de uma vez (skip os que já existem com mesmo itemId+name). */
    @PostMapping("/import-presets")
    public Map<String, Object> importPresets() {
        List<Map<String, Object>> presets = presets();
        List<LootItem> existing = repo.findAll();
        int added = 0, skipped = 0;
        for (var p : presets) {
            String name = (String) p.get("name");
            String itemId = (String) p.get("itemId");
            boolean dup = existing.stream().anyMatch(e ->
                    name.equals(e.getName()) && itemId.equals(e.getItemId()));
            if (dup) { skipped++; continue; }
            LootItem it = new LootItem();
            it.setName(name);
            it.setItemId(itemId);
            it.setCount(((Number) p.get("count")).intValue());
            it.setRarity((String) p.get("rarity"));
            it.setCategory((String) p.get("category"));
            it.setDescription((String) p.get("description"));
            it.setWeight(((Number) p.get("weight")).intValue());
            it.setNbt((String) p.get("nbt"));
            it.setEnabled(true);
            repo.save(it);
            added++;
        }
        return Map.of("ok", true, "added", added, "skipped", skipped, "total", presets.size());
    }
}
