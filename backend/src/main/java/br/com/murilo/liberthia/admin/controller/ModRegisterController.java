package br.com.murilo.liberthia.admin.controller;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import br.com.murilo.liberthia.admin.mod.ModRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint que o MOD chama no startup pra anunciar onde ele tá rodando.
 * Body: {"url":"http://1.2.3.4:25580","token":"abc..."}
 *
 * Permite que o backend rode em qualquer host (cloud) e o mod descobre+anuncia.
 * Sem auth nesse endpoint — qualquer mod pode se registrar; o backend só guarda
 * o último que apareceu.
 *
 * O endpoint /status agora também retorna o estado do CIRCUIT BREAKER do
 * ModBridgeClient (consecutiveFails, circuitOpen, retryInSec) pro frontend
 * mostrar banner "🔴 Mod offline" em vez de erros gritando.
 */
@RestController
@RequestMapping("/api/mod")
public class ModRegisterController {

    private final ModRegistry registry;
    private final ModBridgeClient bridge;

    public ModRegisterController(ModRegistry registry, ModBridgeClient bridge) {
        this.registry = registry;
        this.bridge = bridge;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String url = (String) body.get("url");
        String token = (String) body.get("token");
        String fromIp = req.getRemoteAddr();
        registry.update(url, token, fromIp);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("registeredUrl", registry.getUrl());
        resp.put("at", Instant.now().toString());
        return resp;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("modUrl", registry.getUrl());
        m.put("hasToken", registry.getToken() != null && !registry.getToken().isBlank());
        m.put("lastRegisteredAt", registry.getLastRegisteredAt() == null ? null : registry.getLastRegisteredAt().toString());
        m.put("lastRegisteredFromIp", registry.getLastRegisteredFromIp());
        // Reachability: o backend consegue ALCANÇAR a URL registrada?
        // Diferente de hasToken+modUrl, isso testa de verdade.
        m.put("reachable", registry.getLastReachable());
        m.put("reachableMessage", registry.getLastReachableMessage());
        m.put("reachableCheckedAt", registry.getLastReachableCheckAt() == null ? null : registry.getLastReachableCheckAt().toString());
        // Circuit breaker — estado interno do ModBridgeClient.
        // Permite o frontend mostrar "Reconectando em 12s..." em vez de erros loop.
        m.put("bridge", bridge.bridgeStatus());
        // online = reachable OR circuito fechado E última chamada OK (rede ativa)
        Boolean reach = registry.getLastReachable();
        boolean circuitOk = !bridge.isCircuitOpen();
        m.put("online", (reach == null ? false : reach) || circuitOk);
        return m;
    }

    /**
     * Alias público em /api/mod-status (sem barra antes do "status" — não casa
     * com /api/mod/status pra evitar conflito com prefixos). Frontend pode
     * fazer polling sem auth.
     */
    @RestController
    public static class PublicModStatusController {
        private final ModRegistry registry;
        private final ModBridgeClient bridge;
        public PublicModStatusController(ModRegistry registry, ModBridgeClient bridge) {
            this.registry = registry;
            this.bridge = bridge;
        }
        @GetMapping("/api/mod-status")
        public Map<String, Object> publicStatus() {
            Map<String, Object> m = new LinkedHashMap<>();
            Boolean reach = registry.getLastReachable();
            boolean circuitOk = !bridge.isCircuitOpen();
            // Online se acessível OU circuito ainda fechado (ainda não detectou problema)
            m.put("online", (reach == null ? false : reach) && circuitOk);
            m.put("reachable", reach);
            m.put("circuitOpen", bridge.isCircuitOpen());
            Map<String, Object> b = bridge.bridgeStatus();
            m.put("retryInSec", b.get("openForSec"));
            m.put("consecutiveFails", b.get("consecutiveFails"));
            m.put("lastFailReason", b.get("lastFailReason"));
            m.put("lastSuccessAt", b.get("lastSuccessAt"));
            return m;
        }
    }
}
