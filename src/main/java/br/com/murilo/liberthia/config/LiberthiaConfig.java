package br.com.murilo.liberthia.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LiberthiaConfig {
    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();
        SERVER = new Server(serverBuilder);
        SERVER_SPEC = serverBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }

    private LiberthiaConfig() {
    }

    public static final class Client {
        public final ForgeConfigSpec.IntValue infectionX;
        public final ForgeConfigSpec.IntValue infectionY;
        public final ForgeConfigSpec.IntValue exposureX;
        public final ForgeConfigSpec.IntValue exposureY;
        public final ForgeConfigSpec.IntValue dnaX;
        public final ForgeConfigSpec.IntValue dnaY;
        // r55: posição customizável da HUD de sanidade
        public final ForgeConfigSpec.IntValue sanityX;
        public final ForgeConfigSpec.IntValue sanityY;
        /**
         * Anchor da HUD: 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right.
         * Coords são relativas a esse canto.
         */
        public final ForgeConfigSpec.IntValue sanityAnchor;
        public final ForgeConfigSpec.BooleanValue sanityHudVisible;

        private Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Configuração de interface (HUD).").push("hud");

            infectionX = builder
                    .comment("Posição X da barra de infecção.")
                    .defineInRange("infection_x", 10, 0, 4000);

            infectionY = builder
                    .comment("Posição Y da barra de infecção.")
                    .defineInRange("infection_y", 10, 0, 4000);

            exposureX = builder
                    .comment("Posição X do alerta de exposição.")
                    .defineInRange("exposure_x", 10, 0, 4000);

            exposureY = builder
                    .comment("Posição Y do alerta de exposição.")
                    .defineInRange("exposure_y", 50, 0, 4000);

            dnaX = builder
                    .comment("Posição X do painel de mutação de DNA.")
                    .defineInRange("dna_x", 10, 0, 4000);

            dnaY = builder
                    .comment("Posição Y do painel de mutação de DNA.")
                    .defineInRange("dna_y", 95, 0, 4000);

            // r55: HUD de sanidade
            sanityX = builder
                    .comment("Posição X da barra de sanidade (relativa ao anchor).")
                    .defineInRange("sanity_x", 92, 0, 4000);
            sanityY = builder
                    .comment("Posição Y da barra de sanidade (relativa ao anchor).")
                    .defineInRange("sanity_y", 50, 0, 4000);
            sanityAnchor = builder
                    .comment("Anchor da barra de sanidade. 0=top-left, 1=top-right, "
                            + "2=bottom-left, 3=bottom-right. Default 3 (canto inf direito).")
                    .defineInRange("sanity_anchor", 3, 0, 3);
            sanityHudVisible = builder
                    .comment("Mostrar a barra de sanidade na HUD?")
                    .define("sanity_hud_visible", true);

            builder.pop();
        }
    }

    public static final class Server {
        public final ForgeConfigSpec.BooleanValue worldSpawnsEnabled;
        public final ForgeConfigSpec.IntValue spawnIntervalTicks;

        // r112: Toggle global do Cosmic Horror (kill switch pro spam)
        public final ForgeConfigSpec.BooleanValue cosmicHorrorEnabled;
        public final ForgeConfigSpec.BooleanValue cosmicChatSpamEnabled;
        public final ForgeConfigSpec.BooleanValue cosmicHorrorAutoTick;

        // Admin HTTP API
        public final ForgeConfigSpec.BooleanValue adminApiEnabled;
        public final ForgeConfigSpec.IntValue adminApiPort;
        public final ForgeConfigSpec.ConfigValue<String> adminApiToken;
        public final ForgeConfigSpec.ConfigValue<String> adminApiBindAddress;
        public final ForgeConfigSpec.BooleanValue adminApiHideCommand;
        public final ForgeConfigSpec.ConfigValue<String> adminBackendUrl;
        public final ForgeConfigSpec.BooleanValue adminBackendAutoRegister;
        public final ForgeConfigSpec.ConfigValue<String> adminApiPublicAddress;

        // Voice capture (integração com Simple Voice Chat)
        public final ForgeConfigSpec.BooleanValue voiceCaptureEnabled;
        public final ForgeConfigSpec.ConfigValue<String> voiceBackendUrl;

        // Telemetria (sistema nervoso do servidor)
        public final ForgeConfigSpec.BooleanValue telemetryEnabled;
        public final ForgeConfigSpec.IntValue telemetryPushIntervalSeconds;
        public final ForgeConfigSpec.ConfigValue<String> telemetryBackendUrl;

        // r186: Motor de Matéria Escura (overhaul)
        public final ForgeConfigSpec.BooleanValue blackMatterEngineEnabled;
        public final ForgeConfigSpec.IntValue blackMatterEngineMaxRadius;
        public final ForgeConfigSpec.IntValue blackMatterEngineSpeedMultiplier;
        public final ForgeConfigSpec.IntValue blackMatterCrystalChancePercent;

        private Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Configuração dos surtos de Matéria Escura no mundo.").push("world");

            worldSpawnsEnabled = builder
                    .comment("Permite surtos periódicos de Matéria Escura no Overworld.")
                    .define("world_spawns_enabled", true);

            spawnIntervalTicks = builder
                    .comment("Intervalo em ticks para tentar gerar um foco de Matéria Escura.")
                    .defineInRange("spawn_interval_ticks", 2400, 200, 24000);

            // r112: Kill-switch global pra Cosmic Horror
            cosmicHorrorEnabled = builder
                    .comment("Liga/desliga TODOS os eventos de cosmic horror (fake player names, "
                            + "hallucinations, paranoia chat). Default: false — só ativa quando "
                            + "player pega item de horror (Tome, Forbidden Tome, etc).")
                    .define("cosmic_horror_enabled", false);

            cosmicChatSpamEnabled = builder
                    .comment("Permite mensagens fake de chat (jogador entrou/saiu, morreu pra zombie, etc). "
                            + "Default: false — desativa o spam do WrongPlayerManager.")
                    .define("cosmic_chat_spam_enabled", false);

            cosmicHorrorAutoTick = builder
                    .comment("Permite que o Horror Framework ticke automaticamente os 18 sistemas. "
                            + "Default: false — só roda quando explicitamente acionado.")
                    .define("cosmic_horror_auto_tick", false);

            builder.pop();

            // r186: Motor de Matéria Escura — controle da infecção do terreno
            builder.comment("Motor de Matéria Escura — controle da infecção do terreno.").push("black_matter_engine");
            blackMatterEngineEnabled = builder
                    .comment("Quando false, o Motor de Matéria Escura para (sem spread/efeitos).")
                    .define("enabled", true);
            blackMatterEngineMaxRadius = builder
                    .comment("Raio máximo (blocos) da infecção. Cresce sem teto de código; este é o limite configurável.")
                    .defineInRange("max_radius", 128, 8, 2048);
            blackMatterEngineSpeedMultiplier = builder
                    .comment("Multiplicador de velocidade da infecção (1=normal, 10=máximo).")
                    .defineInRange("speed_multiplier", 1, 1, 10);
            blackMatterCrystalChancePercent = builder
                    .comment("Chance % (1-100) de gerar crystalized_dark_matter ao infectar. Default 8 (raro).")
                    .defineInRange("crystal_chance_percent", 8, 1, 100);
            builder.pop();

            builder.comment("Painel de administração via HTTP. Permite controlar players de fora do jogo.").push("admin_api");

            adminApiEnabled = builder
                    .comment("Liga o servidor HTTP. Quando true, o backend pode conectar nesse host:port.")
                    .define("enabled", true);

            adminApiPort = builder
                    .comment("Porta TCP do servidor HTTP.")
                    .defineInRange("port", 25580, 1024, 65535);

            adminApiToken = builder
                    .comment("Token compartilhado pra autenticar requests. Header: X-Liberthia-Token. " +
                            "Vazio = mod gera UUID randômico no primeiro start.")
                    .define("token", "");

            adminApiBindAddress = builder
                    .comment("Endereço de bind. 127.0.0.1 = local-only, 0.0.0.0 = todas interfaces.")
                    .define("bind_address", "0.0.0.0");

            adminApiHideCommand = builder
                    .comment("Quando true, o comando /liberthia admin NÃO aparece em autocomplete pra ninguém " +
                            "(nem ops). OPs ainda podem digitar e executar manualmente; o link sai no log do " +
                            "servidor + arquivo world/serverconfig/liberthia_admin_url.txt.")
                    .define("hide_command", true);

            adminBackendUrl = builder
                    .comment("URL pública do backend Spring Boot (ex: https://meudominio.com ou http://1.2.3.4:8090). " +
                            "Quando setado, o mod auto-registra a si mesmo no backend via POST /api/mod/register " +
                            "no startup, mandando seu próprio URL+token. Backend libera CORS pra qualquer origem, " +
                            "então o frontend pode rodar em qualquer host.")
                    .define("backend_url", "https://backend.astaroneremita.com");

            adminBackendAutoRegister = builder
                    .comment("Quando true (default), o mod tenta se registrar no backend a cada start.")
                    .define("backend_auto_register", true);

            adminApiPublicAddress = builder
                    .comment("Endereço público (DDNS, IP público ou hostname) pelo qual o BACKEND consegue " +
                            "alcançar este servidor MC. Necessário quando o backend roda em outra máquina " +
                            "(ex: VPS, cloud) e o MC está atrás de NAT. Ex: \"lsmp.ddns.net\", \"200.151.10.20\". " +
                            "Vazio = mod registra o IP da LAN (192.168.x.x), que SÓ funciona se o backend " +
                            "estiver na mesma rede. Lembre de abrir a porta " + 25580 + " (admin_api.port) " +
                            "no firewall + port-forward no roteador apontando pra esta máquina.")
                    .define("public_address", "lsmp.ddns.net");

            builder.pop();

            builder.comment("Captura de voz via Simple Voice Chat — grava todo player que falar.").push("voice");

            voiceCaptureEnabled = builder
                    .comment("Liga/desliga a captura de voz. Mesmo true, só funciona se SVC estiver instalado.")
                    .define("capture_enabled", true);

            voiceBackendUrl = builder
                    .comment("URL do backend pra fazer upload dos clipes. Geralmente = backend_url do admin_api. " +
                            "Vazio = não tenta upload (clipes descartados).")
                    .define("backend_url", "https://backend.astaroneremita.com");

            builder.pop();

            // ----- Telemetria (sistema nervoso) -----
            builder.comment("Sistema nervoso do servidor: captura eventos do player " +
                    "(movimento, combate, inventário, etc), computa features de comportamento " +
                    "(frustração, exploração, agressividade) e infere goal/mood. " +
                    "Pode ser desligado pra reduzir carga.").push("telemetry");

            telemetryEnabled = builder
                    .comment("Liga/desliga o sistema de telemetria. Se false, nenhum evento é " +
                            "capturado e nenhum push é feito pro backend. Útil pra debug/perf.")
                    .define("enabled", true);

            telemetryPushIntervalSeconds = builder
                    .comment("Intervalo entre pushes pro backend (segundos). 0 = não envia. " +
                            "Dados locais (JSONL no world dir) ainda são gravados.")
                    .defineInRange("push_interval_seconds", 10, 0, 600);

            telemetryBackendUrl = builder
                    .comment("URL do backend pra receber snapshots de telemetria. " +
                            "Vazio = usa adminBackendUrl como fallback.")
                    .define("backend_url", "");

            builder.pop();
        }
    }

    /** Helper estático pra outros módulos lerem o URL do backend de voz. */
    public static String voiceBackendUrl() {
        try {
            String u = SERVER.voiceBackendUrl.get();
            if (u != null && !u.isBlank()) return u;
            // Fallback: usa o backend admin se voice não tiver URL própria
            return SERVER.adminBackendUrl.get();
        } catch (Exception e) {
            return null;
        }
    }

    /** Helper estático pro URL do backend de telemetria (com fallback). */
    public static String telemetryBackendUrl() {
        try {
            String u = SERVER.telemetryBackendUrl.get();
            if (u != null && !u.isBlank()) return u;
            return SERVER.adminBackendUrl.get();
        } catch (Exception e) {
            return null;
        }
    }

    /** Toggle runtime: true = telemetria ligada. Cached + atualizado via config. */
    public static boolean isTelemetryEnabled() {
        try { return SERVER.telemetryEnabled.get(); }
        catch (Exception e) { return true; } // default on se config não carregou
    }
}
