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

            builder.pop();
        }
    }

    public static final class Server {
        public final ForgeConfigSpec.BooleanValue worldSpawnsEnabled;
        public final ForgeConfigSpec.IntValue spawnIntervalTicks;

        // Admin HTTP API
        public final ForgeConfigSpec.BooleanValue adminApiEnabled;
        public final ForgeConfigSpec.IntValue adminApiPort;
        public final ForgeConfigSpec.ConfigValue<String> adminApiToken;
        public final ForgeConfigSpec.ConfigValue<String> adminApiBindAddress;
        public final ForgeConfigSpec.BooleanValue adminApiHideCommand;
        public final ForgeConfigSpec.ConfigValue<String> adminBackendUrl;
        public final ForgeConfigSpec.BooleanValue adminBackendAutoRegister;

        // Voice capture (integração com Simple Voice Chat)
        public final ForgeConfigSpec.BooleanValue voiceCaptureEnabled;
        public final ForgeConfigSpec.ConfigValue<String> voiceBackendUrl;

        private Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Configuração dos surtos de Matéria Escura no mundo.").push("world");

            worldSpawnsEnabled = builder
                    .comment("Permite surtos periódicos de Matéria Escura no Overworld.")
                    .define("world_spawns_enabled", true);

            spawnIntervalTicks = builder
                    .comment("Intervalo em ticks para tentar gerar um foco de Matéria Escura.")
                    .defineInRange("spawn_interval_ticks", 2400, 200, 24000);

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
                    .define("backend_url", "");

            adminBackendAutoRegister = builder
                    .comment("Quando true (default), o mod tenta se registrar no backend a cada start.")
                    .define("backend_auto_register", true);

            builder.pop();

            builder.comment("Captura de voz via Simple Voice Chat — grava todo player que falar.").push("voice");

            voiceCaptureEnabled = builder
                    .comment("Liga/desliga a captura de voz. Mesmo true, só funciona se SVC estiver instalado.")
                    .define("capture_enabled", true);

            voiceBackendUrl = builder
                    .comment("URL do backend pra fazer upload dos clipes. Geralmente = backend_url do admin_api. " +
                            "Vazio = não tenta upload (clipes descartados).")
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
}
