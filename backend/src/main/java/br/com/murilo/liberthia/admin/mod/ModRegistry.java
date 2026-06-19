package br.com.murilo.liberthia.admin.mod;

import br.com.murilo.liberthia.admin.config.BackendConfig;
import br.com.murilo.liberthia.admin.config.BackendConfigService;
import br.com.murilo.liberthia.admin.config.ModBridgeConfig;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Registry mutável do mod conectado. Inicializa com valores do application.yml,
 * mas pode ser sobrescrito em runtime via POST /api/mod/register (chamado pelo
 * próprio mod no startup). Isso permite que o backend rode em ambiente público
 * (cloud) e o mod descobre+anuncia onde está.
 */
@Component
public class ModRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModRegistry.class);

    private final ModBridgeConfig config;
    private final BackendConfigService backendConfig;

    private volatile String url;
    private volatile String token;
    /**
     * Onde o token atual veio: "DB" (override persistido em backend_config) ou
     * "ENV" (fallback do application.yml/MOD_TOKEN). Frontend usa pra mostrar
     * pro operador qual fonte tá ativa.
     *
     * <p>v0.1.48+: o auto-register do mod NUNCA muda o token mais. Apenas o
     * painel admin via {@link #applyToken(String)}. Por isso source só pode
     * passar de "ENV" pra "DB" via {@code applyToken} — nunca pelo register.
     */
    private volatile String tokenSource = "ENV";
    /**
     * URL que veio da CONFIG INICIAL (application.yml / MOD_URL env). Quando
     * setada, ela é a fonte de verdade — o operador definiu explicitamente
     * onde o mod está. Registros vindos do próprio mod via POST /api/mod/register
     * só atualizam o TOKEN, nunca a URL. Isso evita o caso clássico onde o
     * `liberthia-server.toml` do MC tem um IP cacheado/velho hardcoded em
     * `admin_api.public_address` e fica sobrescrevendo a URL boa do operador.
     */
    private volatile String configuredUrl;
    private volatile Instant lastRegisteredAt;
    private volatile String lastRegisteredFromIp;
    private volatile WebClient webClient;
    private volatile Boolean lastReachable;
    private volatile String lastReachableMessage;
    private volatile Instant lastReachableCheckAt;

    @Autowired
    public ModRegistry(ModBridgeConfig config, BackendConfigService backendConfig) {
        this.config = config;
        this.backendConfig = backendConfig;
    }

    @PostConstruct
    public void init() {
        this.configuredUrl = normalizeUrl(config.getUrl());
        this.url = this.configuredUrl;

        // Prioridade do TOKEN no boot:
        //   1) DB (backend_config.MOD_TOKEN)  — persistido em runtime, sobrevive restart
        //   2) ENV (config.getToken()/MOD_TOKEN application.yml) — fallback inicial
        // O DB ganha porque historicamente o operador esquecia de sincronizar o
        // env var do compose com o token real gerado pelo mod no servidor MC.
        String dbToken = backendConfig.getValue(BackendConfig.KEY_MOD_TOKEN);
        if (dbToken != null && !dbToken.isBlank()) {
            this.token = dbToken;
            this.tokenSource = "DB";
            log.info("[ModRegistry] token carregado do DB (override persistido)");
        } else {
            this.token = config.getToken();
            this.tokenSource = "ENV";
            log.info("[ModRegistry] token carregado do env var MOD_TOKEN (sem override no DB)");
        }
        // v0.1.48+: NÃO carrega mais flag de lock — o comportamento agora é
        // PERMANENTE: token só muda via painel admin. O auto-register do mod
        // é silenciosamente ignorado pro token (URL continua aceita).
        rebuildClient();
        if (this.configuredUrl != null && !this.configuredUrl.isBlank()
                && !isLocalhost(this.configuredUrl)) {
            log.info("[ModRegistry] MOD_URL fixa do env: {} — registros do mod não vão sobrescrever a URL",
                    this.configuredUrl);
        }
    }

    public synchronized void update(String url, String token, String fromIp) {
        String normalized = normalizeUrl(url);
        // Se o operador configurou MOD_URL no env (não localhost), IGNORA a URL
        // que o mod manda. O mod só pode atualizar o TOKEN. Isso protege contra
        // bug onde `liberthia-server.toml` do MC tem IP velho cacheado em
        // admin_api.public_address e fica re-registrando com URL morta a cada
        // 60s, sobrescrevendo a URL boa do compose. Logado uma vez por par
        // (url-recebida, fromIp) pra ficar visível no diagnóstico.
        boolean operatorConfigured = this.configuredUrl != null
                && !this.configuredUrl.isBlank()
                && !isLocalhost(this.configuredUrl);
        boolean urlMismatch = normalized != null && !normalized.isBlank()
                && !normalized.equals(this.configuredUrl);

        if (operatorConfigured && urlMismatch) {
            // Loga em DEBUG depois da 1ª vez pra não poluir log (cada 60s o mod
            // tenta de novo). Em INFO no primeiro mismatch detectado.
            if (this.url != null && this.url.equals(this.configuredUrl)) {
                log.warn("[ModRegistry] mod tentou registrar url={} mas MOD_URL do env é {} — IGNORANDO. " +
                                "Edite admin_api.public_address no config/liberthia-server.toml " +
                                "do servidor MC pra eliminar essa mensagem.",
                        normalized, this.configuredUrl);
            } else {
                log.debug("[ModRegistry] mod tentou registrar url={} (ignorado, MOD_URL fixo)", normalized);
            }
            // Mantém this.url = configuredUrl, só atualiza token + timestamps
        } else if (normalized != null && !normalized.isBlank() && !normalized.equals(this.url)) {
            // Caso normal: operador não fixou URL, ou URL bate. Aceita.
            log.info("[ModRegistry] mod re-registered: url={} (from {})", normalized, fromIp);
            this.url = normalized;
            rebuildClient();
        }
        // v0.1.48+: o auto-register do mod NUNCA muda o token — APENAS o painel
        // admin (/api/admin/mod-config/token, que chama applyToken()) muda.
        // User pediu explicitamente: "a única forma de salvar/modificar o token é
        // pelo painel — não sobrescreva no banco nada que não venha de lá".
        // Aqui só logamos diagnóstico (silenciosamente, só na 1ª vez por mismatch)
        // pra ajudar a debugar quando o token do mod difere do salvo:
        if (token != null && !token.isBlank() && !token.equals(this.token)) {
            // Log apenas no primeiro detect — depois fica spammy.
            if (lastRegisteredAt == null) {
                log.info("[ModRegistry] mod registrou com token diferente do painel — IGNORANDO. " +
                                "Para mudar, use o painel admin em /mod-config (POST /api/admin/mod-config/token).");
            }
        }
        this.lastRegisteredAt = Instant.now();
        this.lastRegisteredFromIp = fromIp;
        // Testa conectividade em thread separada — não bloqueia o POST /register
        new Thread(this::probeReachable, "ModReachabilityProbe").start();
    }

    private static boolean isLocalhost(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        return u.contains("://localhost") || u.contains("://127.0.0.1")
                || u.contains("://0.0.0.0") || u.contains("://[::1]");
    }

    /**
     * Faz HEAD/GET no /health do mod pra confirmar que o backend consegue
     * alcançar a URL registrada. Resultado fica disponível via /api/mod/status.
     */
    private void probeReachable() {
        String target = this.url;
        if (target == null || target.isBlank()) {
            this.lastReachable = false;
            this.lastReachableMessage = "url vazia";
            this.lastReachableCheckAt = Instant.now();
            return;
        }
        String healthUrl = target.replaceAll("/+$", "") + "/health";
        try {
            // FQN aqui porque importamos reactor.netty.http.client.HttpClient pro WebClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                this.lastReachable = true;
                this.lastReachableMessage = "OK";
                log.info("[ModRegistry] reachability OK: {}", healthUrl);
            } else {
                this.lastReachable = false;
                this.lastReachableMessage = "HTTP " + resp.statusCode();
                log.warn("[ModRegistry] reachability FAIL: {} → HTTP {}", healthUrl, resp.statusCode());
            }
        } catch (Exception e) {
            this.lastReachable = false;
            this.lastReachableMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("[ModRegistry] reachability FAIL: {} → {}", healthUrl, this.lastReachableMessage);
        }
        this.lastReachableCheckAt = Instant.now();
    }

    /** Garante http:// na frente se faltar scheme. */
    private static String normalizeUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return s;
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            return "http://" + s;
        }
        return s;
    }

    private synchronized void rebuildClient() {
        // v0.1.23 DEBUG: loga o token COMPLETO + bytes em hex pra detectar
        // caracteres invisíveis (BOM, espaços, \r, etc) que podem fazer o mod
        // rejeitar mesmo o token parecendo idêntico visualmente.
        if (token != null) {
            StringBuilder hex = new StringBuilder();
            for (byte b : token.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
                hex.append(String.format("%02x ", b & 0xFF));
            }
            log.info("[ModRegistry] TOKEN DEBUG — usando token (len={}): '{}'", token.length(), token);
            log.info("[ModRegistry] TOKEN DEBUG — bytes (hex): {}", hex.toString().trim());
        } else {
            log.warn("[ModRegistry] TOKEN é NULL — verifique MOD_TOKEN env var");
        }
        log.info("[ModRegistry] URL configurada: '{}'", url);

        // CRÍTICO: WebClient default usa timeout INFINITO. Sem isso, quando o
        // mod cai, cada request fica pendurado por ~30s no TCP do kernel —
        // o pool do reactor enche, threads do tomcat empilham, e o frontend
        // vê 503 em CASCATA em todas as APIs por 30s+ antes do circuit breaker
        // do ModBridgeClient sequer perceber.
        //
        // Timeouts:
        //   connectTimeout = 3s  — falha rápido se IP:porta tá fechado
        //   responseTimeout = 8s — corta se mod tá lento ou travado
        //   read/write = 8s     — handler de I/O do netty
        //
        // Com isso, falha real demora ~3-8s em vez de 30s. Após 3 falhas
        // consecutivas o circuit breaker do ModBridgeClient abre por 15s e
        // as próximas calls retornam em ~1ms com 503 mod_offline.
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
                .responseTimeout(Duration.ofSeconds(8))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(8, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(8, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl(url == null ? "http://localhost:25580" : url)
                .defaultHeader("X-Liberthia-Token", token == null ? "" : token)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * Substitui o token em runtime — chamado pelo {@code ModConfigController}
     * quando o operador atualiza o token via painel admin. Persiste no DB,
     * marca a origem como "DB" e reconstrói o WebClient pra próximas calls já
     * irem com o novo token.
     *
     * <p>Diferente de {@link #update(String, String, String)} (que é chamado
     * pelo próprio mod), aqui o operador é a fonte explícita — então NÃO
     * tocamos em URL e NÃO atualizamos {@code lastRegisteredAt} (que é métrica
     * do mod re-registrar).
     */
    public synchronized void applyToken(String newToken) {
        if (newToken == null || newToken.isBlank()) {
            throw new IllegalArgumentException("token vazio");
        }
        backendConfig.setValue(BackendConfig.KEY_MOD_TOKEN, newToken);
        this.token = newToken;
        this.tokenSource = "DB";
        log.info("[ModRegistry] token substituído via painel admin — persistido no DB");
        rebuildClient();
    }

    public WebClient client() { return webClient; }
    public String getUrl() { return url; }
    public String getToken() { return token; }
    public String getTokenSource() { return tokenSource; }
    public Instant getLastRegisteredAt() { return lastRegisteredAt; }
    public String getLastRegisteredFromIp() { return lastRegisteredFromIp; }
    public Boolean getLastReachable() { return lastReachable; }
    public String getLastReachableMessage() { return lastReachableMessage; }
    public Instant getLastReachableCheckAt() { return lastReachableCheckAt; }
}
