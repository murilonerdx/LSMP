package br.com.murilo.liberthia.admin.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loga endpoints + JPA entities + DB tables após startup. Útil pra debugar:
 *  - "404 endpoint not found" → endpoint registrado?
 *  - "nada no banco" → JPA descobriu a entity? a tabela existe?
 */
@Component
public class StartupInfo {

    private static final Logger LOG = LoggerFactory.getLogger(StartupInfo.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager em;

    public StartupInfo(RequestMappingHandlerMapping handlerMapping, JdbcTemplate jdbc) {
        this.handlerMapping = handlerMapping;
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logEverything() {
        logRoutes();
        logEntities();
        logTables();
    }

    private void logRoutes() {
        Map<RequestMappingInfo, HandlerMethod> all = handlerMapping.getHandlerMethods();
        List<String> rows = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : all.entrySet()) {
            RequestMappingInfo info = e.getKey();
            HandlerMethod h = e.getValue();
            Set<String> methods = info.getMethodsCondition().getMethods().stream()
                    .map(m -> m.name()).collect(java.util.stream.Collectors.toSet());
            Set<String> patterns = info.getPathPatternsCondition() != null
                    ? info.getPathPatternsCondition().getPatterns().stream()
                        .map(p -> p.getPatternString()).collect(java.util.stream.Collectors.toSet())
                    : Collections.emptySet();
            for (String p : patterns) {
                rows.add(String.format("%-7s %s  →  %s.%s",
                        methods.isEmpty() ? "ANY" : methods.iterator().next(),
                        p,
                        h.getBeanType().getSimpleName(),
                        h.getMethod().getName()));
            }
        }
        Collections.sort(rows);
        LOG.info("================================================================");
        LOG.info("🌐 ENDPOINTS REGISTRADOS ({} total)", rows.size());
        LOG.info("================================================================");
        for (String r : rows) LOG.info("  {}", r);
    }

    private void logEntities() {
        Set<EntityType<?>> entities = em.getMetamodel().getEntities();
        List<String> rows = new ArrayList<>();
        for (EntityType<?> et : entities) {
            rows.add(et.getJavaType().getName());
        }
        Collections.sort(rows);
        LOG.info("================================================================");
        LOG.info("🗃 JPA ENTITIES DESCOBERTAS ({} total)", rows.size());
        LOG.info("================================================================");
        for (String r : rows) LOG.info("  {}", r);
        if (rows.isEmpty()) {
            LOG.error("⚠ NENHUMA ENTITY JPA FOI DESCOBERTA! Verifique @EntityScan e package scanning.");
        }
    }

    private void logTables() {
        try {
            List<Map<String, Object>> tables = jdbc.queryForList(
                    "SELECT table_name, " +
                    "(SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name AND table_schema = 'public') as col_count " +
                    "FROM information_schema.tables t WHERE table_schema = 'public' ORDER BY table_name");
            LOG.info("================================================================");
            LOG.info("📊 TABELAS NO BANCO ({} total)", tables.size());
            LOG.info("================================================================");
            for (Map<String, Object> t : tables) {
                String name = (String) t.get("table_name");
                try {
                    Long cnt = jdbc.queryForObject("SELECT COUNT(*) FROM \"" + name + "\"", Long.class);
                    LOG.info("  {}  ({} colunas, {} rows)", padRight(name, 35), t.get("col_count"), cnt);
                } catch (Exception ex) {
                    LOG.info("  {}  (erro: {})", name, ex.getMessage());
                }
            }
            LOG.info("================================================================");
            if (tables.isEmpty()) {
                LOG.error("⚠ NENHUMA TABELA NO BANCO! Verifique:");
                LOG.error("    1. spring.jpa.hibernate.ddl-auto está em 'update' ou 'create'");
                LOG.error("    2. PostgreSQL está conectado (veja log de conexão acima)");
                LOG.error("    3. Permissões do usuário do banco");
            }
        } catch (Exception e) {
            LOG.error("⚠ ERRO ao listar tabelas: {}", e.getMessage());
            LOG.error("    Provavelmente o backend NÃO está conectado ao banco.");
        }
    }

    private String padRight(String s, int n) {
        return s.length() >= n ? s : s + " ".repeat(n - s.length());
    }
}
