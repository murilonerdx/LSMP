package br.com.murilo.liberthia.admin.engine.autogift;

import br.com.murilo.liberthia.admin.engine.loot.LootItem;
import br.com.murilo.liberthia.admin.engine.loot.LootItemRepository;
import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Scheduler do AutoGift. Roda a cada 1 minuto, busca regras ativas cujo
 * nextRunAt já passou, e executa.
 */
@Service
public class AutoGiftScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AutoGiftScheduler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AutoGiftRuleRepository rules;
    private final AutoGiftRunRepository runs;
    private final AutoGiftMetricService metrics;
    private final ModBridgeClient mod;
    private final LootItemRepository lootRepo;

    public AutoGiftScheduler(AutoGiftRuleRepository rules, AutoGiftRunRepository runs,
                             AutoGiftMetricService metrics, ModBridgeClient mod,
                             LootItemRepository lootRepo) {
        this.rules = rules;
        this.runs = runs;
        this.metrics = metrics;
        this.mod = mod;
        this.lootRepo = lootRepo;
    }

    /** Roda a cada 60s pra verificar quais regras devem disparar. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void tick() {
        Instant now = Instant.now();
        List<AutoGiftRule> dueRules = rules.findByActiveTrueAndNextRunAtBefore(now);
        if (dueRules.isEmpty()) return;
        LOG.info("[AutoGift] tick — {} regras pra disparar", dueRules.size());
        for (AutoGiftRule rule : dueRules) {
            try {
                executeRule(rule);
            } catch (Exception e) {
                LOG.error("[AutoGift] regra {} ({}) erro: {}", rule.getId(), rule.getName(), e.getMessage());
                AutoGiftRun run = new AutoGiftRun();
                run.setRuleId(rule.getId());
                run.setRuleName(rule.getName());
                run.setStatus("ERROR");
                run.setErrorMessage(e.getMessage());
                runs.save(run);
            }
            // Atualiza nextRunAt mesmo se deu erro (não trava no loop)
            rule.setLastRunAt(now);
            rule.setNextRunAt(rule.computeNextRunAt(now));
            rule.setRunCount(rule.getRunCount() + 1);
            rules.save(rule);
        }
    }

    /** Executa 1 regra: calcula ranking + distribui rewards. */
    @Transactional
    public AutoGiftRun executeRule(AutoGiftRule rule) throws Exception {
        List<AutoGiftMetricService.Entry> ranking = metrics.rank(
                rule.getMetric(), rule.getTopN(), rule.getMinValue());

        AutoGiftRun run = new AutoGiftRun();
        run.setRuleId(rule.getId());
        run.setRuleName(rule.getName());
        run.setWinnersCount(ranking.size());

        if (ranking.isEmpty()) {
            run.setStatus("NO_WINNERS");
            run.setWinnersJson("[]");
            run.setCommandsJson("[]");
            return runs.save(run);
        }

        // Parse rewards
        JsonNode rewards = MAPPER.readTree(rule.getRewardsJson() == null ? "[]" : rule.getRewardsJson());
        List<String> cmdsRun = new ArrayList<>();
        List<Map<String, Object>> winners = new ArrayList<>();

        for (int i = 0; i < ranking.size(); i++) {
            var winner = ranking.get(i);
            int rank = i + 1;
            winners.add(Map.of(
                    "rank", rank,
                    "playerUuid", winner.playerUuid(),
                    "playerName", winner.playerName(),
                    "value", winner.value()
            ));
            for (JsonNode reward : rewards) {
                String cmd = buildCommandForReward(reward, winner, rank, rule.isScaleByRank(), ranking.size());
                if (cmd == null) continue;
                mod.runCommand(cmd);
                cmdsRun.add(cmd);
            }
        }

        run.setWinnersJson(MAPPER.writeValueAsString(winners));
        run.setCommandsJson(MAPPER.writeValueAsString(cmdsRun));
        run.setStatus("SUCCESS");
        AutoGiftRun saved = runs.save(run);

        // Broadcast geral
        if (!winners.isEmpty()) {
            String top = winners.get(0).get("playerName").toString();
            mod.runCommand("tellraw @a [\"\"," +
                    "{\"text\":\"🎁 Auto-Gift: \",\"color\":\"gold\",\"bold\":true}," +
                    "{\"text\":\"" + escape(rule.getName()) + "\",\"color\":\"aqua\"}," +
                    "{\"text\":\" — Top: \",\"color\":\"gold\"}," +
                    "{\"text\":\"" + escape(top) + "\",\"color\":\"yellow\",\"bold\":true}]");
        }
        return saved;
    }

    /** Constrói o comando MC pra cada reward. Suporta {player}, {rank}. */
    private String buildCommandForReward(JsonNode reward, AutoGiftMetricService.Entry winner,
                                         int rank, boolean scaleByRank, int totalRanks) {
        String type = reward.path("type").asText("");
        String playerName = winner.playerName();
        return switch (type) {
            case "item" -> {
                String itemId = reward.path("id").asText("");
                int baseCount = reward.path("count").asInt(1);
                String nbt = reward.path("nbt").asText("");
                if (itemId.isBlank()) yield null;
                int count = scaleByRank
                        ? Math.max(1, baseCount * (totalRanks - rank + 1))
                        : baseCount;
                yield "give " + playerName + " " + itemId + (nbt.isBlank() ? "" : nbt) + " " + count;
            }
            case "command" -> {
                String tmpl = reward.path("value").asText("");
                if (tmpl.isBlank()) yield null;
                yield tmpl
                        .replace("{player}", playerName)
                        .replace("{rank}", String.valueOf(rank));
            }
            case "broadcast" -> {
                String text = reward.path("value").asText("");
                if (text.isBlank()) yield null;
                String filled = text
                        .replace("{player}", playerName)
                        .replace("{rank}", String.valueOf(rank));
                yield "tellraw @a {\"text\":\"" + escape(filled) + "\",\"color\":\"gold\"}";
            }
            case "loot" -> {
                // Sorteia 1 item da loot table com filtros opcionais
                String category = reward.path("category").asText("");
                String rarity = reward.path("rarity").asText("");
                LootItem picked = drawLoot(category, rarity);
                if (picked == null) yield null;
                picked.setDrawCount(picked.getDrawCount() + 1);
                lootRepo.save(picked);
                String nbt = picked.getNbt() == null || picked.getNbt().isBlank() ? "" : picked.getNbt();
                yield "give " + playerName + " " + picked.getItemId() + nbt + " " + picked.getCount();
            }
            default -> null;
        };
    }

    /** Sorteio ponderado da loot table — mesma lógica do LootController. */
    private LootItem drawLoot(String category, String rarity) {
        var pool = lootRepo.findByEnabledTrue().stream()
                .filter(it -> category == null || category.isBlank() || category.equalsIgnoreCase(it.getCategory()))
                .filter(it -> rarity == null || rarity.isBlank() || rarity.equalsIgnoreCase(it.getRarity()))
                .toList();
        if (pool.isEmpty()) return null;
        long totalW = pool.stream().mapToLong(LootItem::getWeight).sum();
        if (totalW <= 0) return pool.get(0);
        long pick = java.util.concurrent.ThreadLocalRandom.current().nextLong(totalW);
        long acc = 0;
        for (LootItem it : pool) {
            acc += it.getWeight();
            if (pick < acc) return it;
        }
        return pool.get(pool.size() - 1);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
