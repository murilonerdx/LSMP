package br.com.murilo.liberthia.admin.engine.autogift;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auto-gifts")
public class AutoGiftController {

    private final AutoGiftRuleRepository rules;
    private final AutoGiftRunRepository runs;
    private final AutoGiftMetricService metrics;
    private final AutoGiftScheduler scheduler;

    public AutoGiftController(AutoGiftRuleRepository rules, AutoGiftRunRepository runs,
                              AutoGiftMetricService metrics, AutoGiftScheduler scheduler) {
        this.rules = rules;
        this.runs = runs;
        this.metrics = metrics;
        this.scheduler = scheduler;
    }

    // ====== Rules CRUD ======
    @GetMapping("/rules")
    public List<AutoGiftRule> list() { return rules.findAllByOrderByCreatedAtDesc(); }

    @PostMapping("/rules")
    public AutoGiftRule create(@RequestBody AutoGiftRule rule) {
        if (rule.getName() == null || rule.getName().isBlank())
            throw new IllegalArgumentException("name obrigatório");
        return rules.save(rule);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<AutoGiftRule> update(@PathVariable Long id, @RequestBody AutoGiftRule body) {
        return rules.findById(id).map(r -> {
            if (body.getName() != null) r.setName(body.getName());
            if (body.getDescription() != null) r.setDescription(body.getDescription());
            if (body.getMetric() != null) r.setMetric(body.getMetric());
            if (body.getSchedule() != null) {
                r.setSchedule(body.getSchedule());
                // Recalcula próximo run com base no novo schedule
                r.setNextRunAt(r.computeNextRunAt(Instant.now()));
            }
            if (body.getRewardsJson() != null) r.setRewardsJson(body.getRewardsJson());
            r.setActive(body.isActive());
            r.setTopN(Math.max(1, body.getTopN()));
            r.setScaleByRank(body.isScaleByRank());
            r.setMinValue(Math.max(0, body.getMinValue()));
            return ResponseEntity.ok(rules.save(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/rules/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        rules.deleteById(id);
        return Map.of("ok", true);
    }

    /** Dispara a regra agora (manualmente), independente do schedule. */
    @PostMapping("/rules/{id}/run-now")
    public Map<String, Object> runNow(@PathVariable Long id) {
        AutoGiftRule rule = rules.findById(id).orElse(null);
        if (rule == null) return Map.of("ok", false, "error", "not found");
        try {
            AutoGiftRun result = scheduler.executeRule(rule);
            rule.setLastRunAt(Instant.now());
            rule.setRunCount(rule.getRunCount() + 1);
            rules.save(rule);
            return Map.of("ok", true,
                    "winners", result.getWinnersCount(),
                    "status", result.getStatus());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ====== Runs / History ======
    @GetMapping("/rules/{id}/runs")
    public Object listRuns(@PathVariable Long id,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "30") int size) {
        return runs.findByRuleIdOrderByRanAtDesc(id, PageRequest.of(page, Math.min(100, size)));
    }

    @GetMapping("/runs")
    public Object listAllRuns(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "50") int size) {
        return runs.findAllByOrderByRanAtDesc(PageRequest.of(page, Math.min(200, size)));
    }

    // ====== Helpers pro frontend ======
    @GetMapping("/metrics")
    public Object metrics() { return AutoGiftMetricService.describeMetrics(); }

    /** Preview do ranking atual sem executar rewards. */
    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam String metric,
                                       @RequestParam(defaultValue = "10") int topN,
                                       @RequestParam(defaultValue = "0") long minValue) {
        var entries = metrics.rank(metric, topN, minValue);
        return Map.of("metric", metric, "topN", topN, "minValue", minValue,
                "entries", entries);
    }

    @GetMapping("/schedules")
    public Object schedules() {
        return List.of(
                Map.of("id", "every_minute", "label", "⏱ A cada minuto (debug)"),
                Map.of("id", "hourly",       "label", "⏰ A cada hora"),
                Map.of("id", "every6h",      "label", "🕕 A cada 6h"),
                Map.of("id", "every12h",     "label", "🕛 A cada 12h"),
                Map.of("id", "daily",        "label", "📅 Diário (a cada 24h)"),
                Map.of("id", "weekly",       "label", "📆 Semanal"),
                Map.of("id", "monthly",      "label", "🗓 Mensal")
        );
    }

    /** Templates prontos pra criar regras rapidamente. */
    @GetMapping("/templates")
    public Object templates() {
        return List.of(
                // ===== Vozes / Chat =====
                Map.of("name", "🏆 Top 3 Vozes Semanais",
                        "description", "Quem mais falou no voice chat ganha diamantes",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 3, "schedule", "weekly",
                        "scaleByRank", true,
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:diamond\",\"count\":3}," +
                                "{\"type\":\"broadcast\",\"value\":\"🏆 {player} é o rank {rank} de voz da semana!\"}]"),
                Map.of("name", "💬 Tagarelas Diários",
                        "description", "Top 5 do chat ganha XP bottle",
                        "metric", "CHAT_MESSAGES",
                        "topN", 5, "schedule", "daily",
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:experience_bottle\",\"count\":10}]"),
                Map.of("name", "🎙 Voice Champion (Top 1)",
                        "description", "Apenas o #1 ganha — netherite + hero of village + broadcast",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 1, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:netherite_ingot\",\"count\":1}," +
                                "{\"type\":\"command\",\"value\":\"effect give {player} minecraft:hero_of_the_village 86400 0\"}," +
                                "{\"type\":\"broadcast\",\"value\":\"👑 {player} é o CAMPEÃO DE VOZ DA SEMANA!\"}]"),

                // ===== Sobrevivência / Mortes =====
                Map.of("name", "💀 Sobreviventes Mensal",
                        "description", "Quem MENOS morreu — totem of undying",
                        "metric", "DEATHS",
                        "topN", 3, "schedule", "monthly",
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:totem_of_undying\",\"count\":1}]"),
                Map.of("name", "☠ Hall da Morte (top mortes)",
                        "description", "Quem mais morreu ganha enchant book de Mending — pra reanimar a armadura",
                        "metric", "DEATHS",
                        "topN", 3, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:enchanted_book\",\"count\":1}," +
                                "{\"type\":\"command\",\"value\":\"tellraw {player} {\\\"text\\\":\\\"§7Você morreu muito essa semana... aqui um consolo. Tente sobreviver mais.\\\"}\"}]"),

                // ===== Inventory / Acumulação =====
                Map.of("name", "🎒 Acumuladores",
                        "description", "Top 3 com mais items no inventário ganha shulker box",
                        "metric", "INVENTORY_ITEMS",
                        "topN", 3, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:shulker_box\",\"count\":1}]"),
                Map.of("name", "🏗 Construtores (uses creative loot)",
                        "description", "Top inventário ganha kit de construção (16 stacks de wool variados)",
                        "metric", "INVENTORY_ITEMS",
                        "topN", 5, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"give {player} minecraft:white_wool 64\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:black_wool 64\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:red_wool 64\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:blue_wool 64\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:scaffolding 64\"}]"),

                // ===== Efeitos / Buffs temporários =====
                Map.of("name", "⚡ Speed Buff Diário (top voz)",
                        "description", "Top 1 voz do dia ganha speed II por 1h",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 1, "schedule", "daily",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"effect give {player} minecraft:speed 3600 1\"}," +
                                "{\"type\":\"command\",\"value\":\"effect give {player} minecraft:jump_boost 3600 0\"}," +
                                "{\"type\":\"broadcast\",\"value\":\"⚡ {player} ganhou velocidade pelos próximos 60min!\"}]"),
                Map.of("name", "🛡 Resistance Pack (sobrevivente)",
                        "description", "Top 3 menos mortos ganham resistance 30min + regeneration",
                        "metric", "DEATHS",
                        "topN", 3, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"effect give {player} minecraft:resistance 1800 1\"}," +
                                "{\"type\":\"command\",\"value\":\"effect give {player} minecraft:regeneration 1800 0\"}]"),
                Map.of("name", "🌟 Glowing all chatterboxes",
                        "description", "Top 5 do chat ficam brilhando 10min (visíveis pra todo mundo)",
                        "metric", "CHAT_MESSAGES",
                        "topN", 5, "schedule", "daily",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"effect give {player} minecraft:glowing 600 0\"}]"),

                // ===== Comandos vanilla criativos =====
                Map.of("name", "🎆 Fogos de festa (top voz)",
                        "description", "Top 1 voz tem fogos de festa lançados na cabeça via summon",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 1, "schedule", "daily",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"execute at {player} run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:30,FireworksItem:{id:\\\"minecraft:firework_rocket\\\",Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16711680,16776960,65280]}]}}}}\"}," +
                                "{\"type\":\"broadcast\",\"value\":\"🎆 {player} subiu ao topo da voz!\"}]"),
                Map.of("name", "🌙 Mudar pra noite (top morte)",
                        "description", "Quem mais morreu acorda à noite — empatia simbólica",
                        "metric", "DEATHS",
                        "topN", 1, "schedule", "daily",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"time set night\"}," +
                                "{\"type\":\"broadcast\",\"value\":\"🌙 {player} morreu tanto que o sol se escondeu...\"}]"),
                Map.of("name", "📜 Crônica auto-gerada",
                        "description", "Top voz vira crônica eterna do servidor",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 1, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"tellraw @a {\\\"text\\\":\\\"§6§l📜 A semana de {player}: a voz que ressoou na floresta...\\\"}\"}," +
                                "{\"type\":\"item\",\"id\":\"minecraft:writable_book\",\"count\":1}]"),

                // ===== Item raros com NBT =====
                Map.of("name", "🗡 Espada do guerreiro (top voz)",
                        "description", "Diamond sword com Sharpness V + Unbreaking III pro top voz",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 1, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"give {player} minecraft:diamond_sword{Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5s},{id:\\\"minecraft:unbreaking\\\",lvl:3s}],display:{Name:'{\\\"text\\\":\\\"Espada de {player}\\\",\\\"italic\\\":false,\\\"color\\\":\\\"gold\\\"}'}} 1\"}]"),
                Map.of("name", "📦 Mistery Box semanal",
                        "description", "Caixa surpresa pro top 5 — random itens via loot table",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 5, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:chest\",\"count\":1}," +
                                "{\"type\":\"command\",\"value\":\"loot give {player} loot minecraft:chests/simple_dungeon\"}]"),

                // ===== Sociais =====
                Map.of("name", "💖 Heart Container (super social)",
                        "description", "Top chat ganha aumento permanente de HP via attribute",
                        "metric", "CHAT_MESSAGES",
                        "topN", 1, "schedule", "monthly",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"attribute {player} minecraft:generic.max_health modifier add 00000000-0000-0000-0000-000000000001 \\\"social_heart\\\" 2 add_value\"}," +
                                "{\"type\":\"broadcast\",\"value\":\"💖 {player} ganhou +1 coração permanente por ser social!\"}]"),
                Map.of("name", "🎵 Music Disc raro",
                        "description", "Top voz mensal ganha music disc 'Pigstep' (raro)",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 1, "schedule", "monthly",
                        "rewardsJson", "[{\"type\":\"item\",\"id\":\"minecraft:music_disc_pigstep\",\"count\":1}]"),

                // ===== Combos elaborados =====
                Map.of("name", "🏰 Pacote VIP completo (top mensal)",
                        "description", "Top voz mensal: netherite full + elytra + totem + 64 levels XP",
                        "metric", "VOICE_DURATION_MS",
                        "topN", 1, "schedule", "monthly",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"give {player} minecraft:netherite_helmet 1\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:netherite_chestplate 1\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:netherite_leggings 1\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:netherite_boots 1\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:elytra 1\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:totem_of_undying 3\"}," +
                                "{\"type\":\"command\",\"value\":\"xp add {player} 64 levels\"}," +
                                "{\"type\":\"broadcast\",\"value\":\"🏰 {player} recebeu o PACOTE VIP MENSAL!\"}]"),
                Map.of("name", "🌳 Pacote naturalista (top inv)",
                        "description", "Top inventário ganha kit de plantação (saplings, bone meal, sementes)",
                        "metric", "INVENTORY_ITEMS",
                        "topN", 3, "schedule", "weekly",
                        "rewardsJson", "[{\"type\":\"command\",\"value\":\"give {player} minecraft:oak_sapling 16\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:birch_sapling 16\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:dark_oak_sapling 16\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:bone_meal 64\"}," +
                                "{\"type\":\"command\",\"value\":\"give {player} minecraft:wheat_seeds 64\"}]")
        );
    }
}
