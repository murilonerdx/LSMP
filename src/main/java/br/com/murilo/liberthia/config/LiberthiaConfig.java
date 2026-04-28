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

        private Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Configuração dos surtos de Matéria Escura no mundo.").push("world");

            worldSpawnsEnabled = builder
                    .comment("Permite surtos periódicos de Matéria Escura no Overworld.")
                    .define("world_spawns_enabled", true);

            spawnIntervalTicks = builder
                    .comment("Intervalo em ticks para tentar gerar um foco de Matéria Escura.")
                    .defineInRange("spawn_interval_ticks", 2400, 200, 24000);

            builder.pop();
        }
    }
}
