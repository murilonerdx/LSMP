package br.com.murilo.liberthia.admin.engine.calendar;

import br.com.murilo.liberthia.admin.engine.EngineActions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;

@Repository
interface CalendarConfigRepository extends JpaRepository<CalendarConfig, Integer> {}

@Repository
interface SpecialDayRepository extends JpaRepository<SpecialDay, String> {}

@Service
public class CalendarEngine {

    private static final Logger LOG = LoggerFactory.getLogger(CalendarEngine.class);
    private final CalendarConfigRepository cfgRepo;
    private final SpecialDayRepository dayRepo;
    private final EngineActions actions;
    private final ObjectMapper mapper = new ObjectMapper();

    public CalendarEngine(CalendarConfigRepository cfgRepo, SpecialDayRepository dayRepo, EngineActions actions) {
        this.cfgRepo = cfgRepo;
        this.dayRepo = dayRepo;
        this.actions = actions;
    }

    @PostConstruct
    public void init() {
        LOG.info("CalendarEngine ✓ ready (config via SQL seed)");
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 12000)
    public void tick() {
        CalendarConfig c;
        try { c = cfgRepo.findById(1).orElse(null); }
        catch (Exception e) { return; }
        if (c == null || c.isPaused()) return;

        int today = currentDay(c);
        long now = System.currentTimeMillis();
        for (SpecialDay sp : dayRepo.findAll()) {
            if (sp.getDayOfYear() != today) continue;
            long last = sp.getLastTriggerTs() == null ? 0 : sp.getLastTriggerTs();
            if (now - last < 20L * 3600L * 1000L) continue; // já disparou hoje
            fire(sp);
            sp.setLastTriggerTs(now);
            try { dayRepo.save(sp); } catch (Exception ignored) {}
        }
    }

    private void fire(SpecialDay sp) {
        if (sp.getTellraw() != null && !sp.getTellraw().isBlank())
            actions.tellraw("@a", sp.getTellraw());
        if (sp.getSound() != null && !sp.getSound().isBlank()) {
            for (var p : actions.getPlayers()) actions.sound(p.uuid(), sp.getSound(), 1, 1);
        }
        if (sp.getWeather() != null && !sp.getWeather().isBlank())
            actions.weather(sp.getWeather(), 6000);
        if (sp.getSetTimeTo() >= 0) actions.worldTime(sp.getSetTimeTo());
        try {
            JsonNode arr = mapper.readTree(sp.getEffectsJson() == null ? "[]" : sp.getEffectsJson());
            if (arr.isArray()) {
                for (var p : actions.getPlayers()) for (JsonNode e : arr)
                    actions.effect(p.uuid(),
                            e.path("effect").asText("minecraft:luck"),
                            e.path("durationSec").asInt(60) * 20,
                            e.path("amplifier").asInt(0));
            }
        } catch (Exception ignored) {}
        if (sp.getCustomCmd() != null && !sp.getCustomCmd().isBlank())
            actions.runCommand(sp.getCustomCmd());
    }

    public int currentDay(CalendarConfig c) {
        try {
            JsonNode months = mapper.readTree(c.getMonthsJson());
            int total = 0;
            if (months.isArray()) for (JsonNode m : months) total += m.path("days").asInt(30);
            if (total <= 0) total = 1;
            if ("real".equals(c.getMode())) {
                long elapsed = System.currentTimeMillis() - (c.getDayZeroTs() == null ? System.currentTimeMillis() : c.getDayZeroTs());
                long day = elapsed / 86400000L + 1;
                return (int) (((day - 1) % total + total) % total + 1);
            } else {
                // ingame: precisaria do tick count, mas pra simplicidade usamos real também
                long elapsed = System.currentTimeMillis() - (c.getDayZeroTs() == null ? System.currentTimeMillis() : c.getDayZeroTs());
                long day = elapsed / 86400000L + 1;
                return (int) (((day - 1) % total + total) % total + 1);
            }
        } catch (Exception e) { return 1; }
    }

    public CalendarConfig getConfig() {
        return cfgRepo.findById(1).orElseGet(() -> {
            // Cria config default se não existe (substitui o @PostConstruct seeder removido)
            CalendarConfig c = new CalendarConfig();
            c.setId(1);
            c.setYearName("Ano Vorashen I");
            c.setDayZeroTs(System.currentTimeMillis() - 12L * 86400000L);
            c.setMode("real");
            c.setMonthsJson("[" +
                    "{\"emoji\":\"👁\",\"name\":\"Olho Aberto\",\"days\":31,\"color\":\"#a78bfa\"}," +
                    "{\"emoji\":\"🌊\",\"name\":\"Ecos Profundos\",\"days\":28,\"color\":\"#06b6d4\"}," +
                    "{\"emoji\":\"🌫\",\"name\":\"Cinzas Pálidas\",\"days\":31,\"color\":\"#94a3b8\"}," +
                    "{\"emoji\":\"🌙\",\"name\":\"Lua Sangrenta\",\"days\":30,\"color\":\"#dc2626\"}," +
                    "{\"emoji\":\"🫀\",\"name\":\"Coração Mudo\",\"days\":31,\"color\":\"#000000\"}," +
                    "{\"emoji\":\"🪞\",\"name\":\"Véu Rachado\",\"days\":30,\"color\":\"#7c3aed\"}," +
                    "{\"emoji\":\"🕯\",\"name\":\"Última Vigília\",\"days\":28,\"color\":\"#fbbf24\"}]");
            c.setPaused(true);
            return cfgRepo.save(c);
        });
    }
    public CalendarConfig saveConfig(CalendarConfig c) { c.setId(1); return cfgRepo.save(c); }
    public List<SpecialDay> listDays() { return dayRepo.findAll(); }
    public SpecialDay saveDay(SpecialDay s) { return dayRepo.save(s); }
    public void deleteDay(String id) { dayRepo.deleteById(id); }
    public void fireNow(String id) { dayRepo.findById(id).ifPresent(this::fire); }
}
