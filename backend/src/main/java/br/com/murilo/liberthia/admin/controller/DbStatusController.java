package br.com.murilo.liberthia.admin.controller;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoint de diagnóstico do banco. Útil pra confirmar:
 *  - Backend está conectado ao DB?
 *  - Em qual URL/DB está conectado?
 *  - Quais tabelas existem?
 *  - Quantas linhas em cada tabela?
 *
 * Acesse: http://seu-host:8090/api/db-status
 * (Está atrás do AuthFilter — precisa do token de admin)
 */
@RestController
@RequestMapping("/api/db-status")
public class DbStatusController {

    private static final Logger LOG = LoggerFactory.getLogger(DbStatusController.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    @Value("${spring.datasource.url:?}")
    private String configuredUrl;

    @Value("${spring.datasource.username:?}")
    private String configuredUser;

    public DbStatusController(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void logConnection() {
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData m = c.getMetaData();
            LOG.info("📊 DB CONECTADO: {} - user={} version={}",
                    sanitize(m.getURL()), m.getUserName(), m.getDatabaseProductVersion());
        } catch (Exception e) {
            LOG.error("📊 DB ERRO DE CONEXÃO: {}", e.getMessage());
        }
    }

    @GetMapping
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configuredUrl", sanitize(configuredUrl));
        out.put("configuredUser", configuredUser);

        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData m = c.getMetaData();
            out.put("connected", true);
            out.put("realUrl", sanitize(m.getURL()));
            out.put("realUser", m.getUserName());
            out.put("dbProduct", m.getDatabaseProductName() + " " + m.getDatabaseProductVersion());
            out.put("driver", m.getDriverName() + " " + m.getDriverVersion());

            // Lista tabelas + row counts
            Map<String, Object> tables = new LinkedHashMap<>();
            try (ResultSet rs = m.getTables(c.getCatalog(), "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String t = rs.getString("TABLE_NAME");
                    if (t == null || t.startsWith("pg_")) continue;
                    try {
                        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM \"" + t + "\"", Long.class);
                        tables.put(t, count == null ? 0 : count);
                    } catch (Exception ex) {
                        tables.put(t, "ERRO: " + ex.getMessage());
                    }
                }
            }
            out.put("tables", tables);
            out.put("tableCount", tables.size());

            // Sanity check: SELECT 1
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            out.put("selectOneOk", one != null && one == 1);
        } catch (Exception e) {
            out.put("connected", false);
            out.put("error", e.getMessage());
            out.put("errorClass", e.getClass().getName());
        }
        return out;
    }

    /** Esconde senha da URL JDBC nos logs/output. */
    private String sanitize(String url) {
        if (url == null) return "?";
        return url.replaceAll("password=[^&;]*", "password=***");
    }

    /** Endpoint pra inserir um row de teste — confirma que escrita funciona. */
    @GetMapping("/test-write")
    public Map<String, Object> testWrite() {
        Map<String, Object> out = new HashMap<>();
        String testKey = "__test_" + System.currentTimeMillis();
        try {
            jdbc.update(
                    "INSERT INTO kv_configs (key, data_json, updated_at) VALUES (?, ?, NOW()) " +
                    "ON CONFLICT (key) DO UPDATE SET data_json = EXCLUDED.data_json",
                    testKey, "{\"test\":true,\"ts\":" + System.currentTimeMillis() + "}"
            );
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM kv_configs WHERE key = ?", Integer.class, testKey);
            jdbc.update("DELETE FROM kv_configs WHERE key = ?", testKey);
            out.put("ok", true);
            out.put("message", "Escrita+leitura+delete funcionou na tabela kv_configs (inserted=" + count + " row)");
        } catch (Exception e) {
            out.put("ok", false);
            out.put("error", e.getMessage());
            out.put("errorClass", e.getClass().getName());
            LOG.error("test-write fail:", e);
        }
        return out;
    }
}
