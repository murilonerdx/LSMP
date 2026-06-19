package br.com.murilo.liberthia.admin.controller;

import br.com.murilo.liberthia.admin.mod.ModBridgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Pega exceptions e retorna JSON limpo em vez de stack trace.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Circuito ABERTO — mod sabidamente offline. Retorna 503 em ~1ms (sem
     * esperar timeout de 30s). Frontend usa o `error: "mod_offline"` pra
     * mostrar banner em vez de toast de erro genérico.
     */
    @ExceptionHandler(ModBridgeClient.ModOfflineException.class)
    public ResponseEntity<Map<String, Object>> handleModOffline(ModBridgeClient.ModOfflineException ex) {
        // Não logamos aqui — a abertura do circuito já é logada no ModBridgeClient.
        // Logar aqui em cascata vira spam (cada request bloqueada gera log).
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("error", "mod_offline");
        body.put("message", "Servidor MC offline. Backend tentando reconectar.");
        body.put("retryable", true);
        body.put("details", ex.getDetails());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(ModBridgeClient.ModBridgeException.class)
    public ResponseEntity<Map<String, Object>> handleBridge(ModBridgeClient.ModBridgeException ex) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = ex;
        while (cur != null) {
            if (cur.getMessage() != null) sb.append(cur.getMessage()).append(" || ");
            cur = cur.getCause();
        }
        String full = sb.toString().toLowerCase();
        LOG.debug("Mod bridge error: {}", full);
        if (full.contains("404") || full.contains("not found") || full.contains("not implemented") || full.contains("405")) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                    "error", "mod_outdated",
                    "message", "Endpoint não existe no mod rodando. Rebuilde o jar e reinicie o servidor MC."
            ));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "mod_offline",
                "message", "Servidor MC não está acessível. Verifique se o mod está rodando.",
                "retryable", true
        ));
    }

    /**
     * 404 explicito quando rota não existe. Spring lança NoResourceFoundException
     * quando nenhum @RequestMapping casa e não há static resource.
     *
     * Causa típica: jar do backend está DESATUALIZADO (rebuilde o backend!).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handle404(NoResourceFoundException ex) {
        String path = ex.getResourcePath();
        LOG.warn("404 — rota não registrada: {} (backend desatualizado? rebuilde o jar)", path);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "endpoint_not_found",
                "message", "Endpoint /" + path + " não existe neste backend. " +
                        "Provável causa: jar do backend desatualizado — execute `./gradlew bootJar` e reinicie."
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        // Map.of NÃO ACEITA value null — se ex.getMessage() vier null
        // o próprio exception handler explode com NPE, gerando log de
        // erro infinito. Defensive: substituir null por string vazia.
        return ResponseEntity.badRequest().body(Map.of(
                "error", "bad_request",
                "message", ex.getMessage() == null ? "argumento inválido" : ex.getMessage()
        ));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDbError(org.springframework.dao.DataAccessException ex) {
        LOG.error("Erro de banco: {}", ex.getMessage());
        String specific = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "database_error",
                "message", "Erro no PostgreSQL: " + (specific == null ? ex.getClass().getSimpleName() : specific)
        ));
    }

    /**
     * Broken pipe / cliente desconectou no meio da resposta — comum quando
     * o backend tava lento (ex: chamando mod offline) e o cliente desistiu
     * com timeout. NÃO é erro do backend — não vale poluir log com stack
     * trace de 60 linhas a cada ocorrência.
     *
     * <p>Detecta tanto {@code AsyncRequestNotUsableException} (Spring 6+)
     * quanto {@code ClientAbortException} (Tomcat) verificando o root cause
     * por "Broken pipe" ou "Connection reset by peer" no message chain.
     */
    @ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestNotUsableException.class)
    public void handleAsyncBroken(
            org.springframework.web.context.request.async.AsyncRequestNotUsableException ex) {
        // Resposta já foi descartada (socket fechado pelo cliente). Nada a fazer
        // — só evita o stack trace ENORME no log. Logar como debug pra não sumir
        // completamente (se virar epidemia, dá pra ligar o nível debug e ver).
        LOG.debug("Cliente desconectou durante resposta: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Detecta "broken pipe" / "connection reset" no chain — benigno
        Throwable cur = ex;
        for (int i = 0; cur != null && i < 8; i++) {
            String msg = cur.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("broken pipe") || lower.contains("connection reset")
                        || lower.contains("client abort")) {
                    LOG.debug("Cliente desconectou mid-response (benigno): {}", msg);
                    // Retorna 200 vazio — não há cliente pra ler de qualquer jeito,
                    // só evita propagar uma exception genérica pra cima.
                    return ResponseEntity.ok().build();
                }
            }
            cur = cur.getCause();
        }
        LOG.error("Erro inesperado", ex);
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "internal", "message", ex.getMessage() == null ? "" : ex.getMessage()
        ));
    }
}
