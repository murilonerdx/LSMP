package br.com.murilo.liberthia.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Healthcheck + diagnostico do build atual.
 * Útil pra confirmar que o backend rodando é a versão correta (não cached).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final RequestMappingHandlerMapping handlerMapping;

    public HealthController(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    // /health já está no AdminController. Só adicionamos /version aqui.

    /**
     * Mostra build timestamp + lista de endpoints. Use no browser pra confirmar
     * que a jar rodando é a versão atual com TODOS os controllers.
     *
     * Acesse: http://seu-host:8090/api/version
     */
    @GetMapping("/version")
    public Map<String, Object> version() {
        Map<String, Object> out = new HashMap<>();
        out.put("service", "liberthia-admin-backend");
        out.put("startedAt", STARTUP_TIME.toString());
        out.put("classFileTimestamp", classTimestamp());

        // Lista todos os endpoints registrados
        List<String> endpoints = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
            if (info.getPathPatternsCondition() == null) return;
            info.getPathPatternsCondition().getPatterns().forEach(p -> {
                String methods = info.getMethodsCondition().getMethods().isEmpty()
                        ? "ANY"
                        : info.getMethodsCondition().getMethods().iterator().next().name();
                endpoints.add(methods + " " + p.getPatternString());
            });
        });
        endpoints.sort(String::compareTo);
        out.put("endpointCount", endpoints.size());
        out.put("endpoints", endpoints);
        return out;
    }

    private static final Instant STARTUP_TIME = Instant.now();

    /** Pega timestamp do .class atual — confirma se a jar foi recompilada. */
    private String classTimestamp() {
        try {
            URL res = HealthController.class.getResource("HealthController.class");
            if (res == null) return "unknown";
            URLConnection conn = res.openConnection();
            long ts = conn.getLastModified();
            return ts > 0 ? Instant.ofEpochMilli(ts).toString() : "unknown";
        } catch (IOException e) {
            return "error: " + e.getMessage();
        }
    }
}
