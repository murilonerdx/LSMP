package br.com.murilo.liberthia.admin.engine.autogift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * r180: importa as regras de reward por ranking de {@code seed/ranking_rewards.json}
 * como {@link AutoGiftRule}s no startup. Itens EXCLUSIVOS sem craft pros top players.
 *
 * <p>Idempotente: pula regras cujo nome já existe (não duplica). As regras são criadas
 * <b>INATIVAS</b> de propósito — o admin revisa e ativa no painel (AutoGift) quando quiser,
 * ajustando metric/schedule/topN. Assim nada distribui artefato lendário sem revisão.
 */
@Component
public class RankingRewardsSeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(RankingRewardsSeeder.class);

    private final AutoGiftRuleRepository rules;
    private final ObjectMapper mapper;

    public RankingRewardsSeeder(AutoGiftRuleRepository rules, ObjectMapper mapper) {
        this.rules = rules;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) {
        try {
            ClassPathResource res = new ClassPathResource("seed/ranking_rewards.json");
            if (!res.exists()) {
                LOG.info("[RankingRewards] seed/ranking_rewards.json ausente — pulando seed.");
                return;
            }
            JsonNode root;
            try (var in = res.getInputStream()) {
                root = mapper.readTree(in);
            }
            JsonNode arr = root.path("rules");
            if (!arr.isArray() || arr.isEmpty()) return;

            Set<String> existing = new HashSet<>();
            for (AutoGiftRule r : rules.findAll()) existing.add(r.getName());

            int created = 0;
            for (JsonNode r : arr) {
                String name = r.path("name").asText("");
                if (name.isBlank() || existing.contains(name)) continue;

                AutoGiftRule rule = new AutoGiftRule();
                rule.setName(name);
                rule.setDescription(r.path("description").asText(""));
                rule.setMetric(r.path("metric").asText("LOGIN_COUNT"));
                rule.setTopN(r.path("topN").asInt(3));
                rule.setSchedule(r.path("schedule").asText("weekly"));
                rule.setScaleByRank(r.path("scaleByRank").asBoolean(false));
                rule.setMinValue(r.path("minValue").asLong(0));
                rule.setActive(false); // INATIVA — admin ativa no painel após revisar
                rule.setCreatedBy("seed:ranking_rewards");
                rule.setRewardsJson(mapper.writeValueAsString(r.path("rewards")));
                rules.save(rule);
                created++;
            }
            if (created > 0) {
                LOG.info("[RankingRewards] {} regra(s) de reward importada(s) (INATIVAS — ative no painel AutoGift).", created);
            }
        } catch (Exception e) {
            LOG.warn("[RankingRewards] falha ao importar rewards: {}", e.toString());
        }
    }
}
