package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.1.22 r47: <b>The Wrong Player</b> — gera periodicamente uma "identidade
 * falsa" de player que não existe.
 *
 * <h2>Manifestações</h2>
 * <ul>
 *   <li>Fake join messages no chat (only to specific players)</li>
 *   <li>Fake death messages</li>
 *   <li>Fake "you were attacked by X" mentions</li>
 *   <li>Phantom footsteps em coords aleatórias</li>
 *   <li>Fake structure mentions ("X built nearby")</li>
 * </ul>
 *
 * <h2>Per-player</h2>
 * Cada player tem um Wrong Player diferente sendo gerado pra ele. Players
 * podem comparar e descobrir que o nome é diferente — paranoia social.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class WrongPlayerManager {

    /** Pool de nomes fake — mistos português/inglês/uncanny. */
    private static final String[] FAKE_NAMES = {
            "OldSteve", "_Shadow_", "Renan_24", "Marlon", "EmptyName",
            "Daniel__", "PedroSilva", "user_404", "Gabriel_91",
            "Operator", "[deleted]", "guest_a3f", "Carlos21",
            "Caio_dev", "PlayerOne", "miguelnopvp", "anonymous_",
            "Lucas_BR", "Vinicius", "ThiagoX", "joao95",
            "RogerioPVP", "BrendaA", "Camila_22", "Iceshade",
            "Allan_M", "MateusGG", "GabrielX", "FelipeDR",
            "EnzoAlmeida", "RafaelM", "Tiago77", "Mauro_dev"
    };

    /** Per-player NBT: nome do wrong player atual. */
    public static final String NBT_WRONG_NAME = "liberthia.wrong_player_name";
    /** Per-player NBT: quando assigned. */
    public static final String NBT_WRONG_SINCE = "liberthia.wrong_player_since";

    private WrongPlayerManager() {}

    /** Atribui um wrong player a um player. */
    public static String assign(ServerPlayer sp) {
        // Garante que não é o nome do próprio player ou de outro online
        MinecraftServer server = sp.server;
        List<String> online = new ArrayList<>();
        for (ServerPlayer s : server.getPlayerList().getPlayers()) {
            online.add(s.getName().getString());
        }
        String fake;
        int tries = 10;
        do {
            fake = FAKE_NAMES[(int)(Math.random() * FAKE_NAMES.length)];
            if (!online.contains(fake)) break;
        } while (--tries > 0);

        sp.getPersistentData().putString(NBT_WRONG_NAME, fake);
        sp.getPersistentData().putLong(NBT_WRONG_SINCE, sp.level().getGameTime());
        return fake;
    }

    public static String currentName(ServerPlayer sp) {
        return sp.getPersistentData().getString(NBT_WRONG_NAME);
    }

    public static void clear(ServerPlayer sp) {
        sp.getPersistentData().remove(NBT_WRONG_NAME);
        sp.getPersistentData().remove(NBT_WRONG_SINCE);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // r112: kill-switch — desativa spam de "jogador entrou/saiu/morreu"
        if (!br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicChatSpamEnabled.get()) return;
        long now = event.getServer().overworld().getGameTime();
        if (now % 600 != 0) return; // 30s check

        for (ServerPlayer sp : event.getServer().getPlayerList().getPlayers()) {
            String name = currentName(sp);
            if (name == null || name.isEmpty()) {
                // 5% chance per 30s de assign
                if (Math.random() < 0.05) {
                    name = assign(sp);
                    HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                            "§e§l" + name + " §ejoined the game");
                    LiberthiaMod.LOGGER.info("[WrongPlayer] {} assigned fake='{}'",
                            sp.getName().getString(), name);
                }
                continue;
            }

            // Tem wrong player — gera manifestação aleatória
            int rand = (int)(Math.random() * 8);
            String fakeName = "§7§o" + name;
            switch (rand) {
                case 0 -> HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§7<§r" + fakeName + "§7> §o" + pickFakeMsg());
                case 1 -> HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§7" + fakeName + "§7 was slain by Zombie");
                case 2 -> HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§7" + fakeName + "§7 left the game");
                case 3 -> HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§7" + fakeName + "§7 fell from a high place");
                case 4 -> {
                    HallucinationManager.force(sp, HallucinationType.FAKE_FOOTSTEP, 0.8F, 30, "");
                    HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                            "§8§o*passos de " + name + " nas suas costas*");
                }
                case 5 -> {
                    int fx = (int)(sp.getX() + (Math.random() - 0.5) * 500);
                    int fz = (int)(sp.getZ() + (Math.random() - 0.5) * 500);
                    HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                            "§8§o*você se lembra de " + name + " construindo em " + fx + ", ?, " + fz + "*");
                }
                case 6 -> HallucinationManager.force(sp, HallucinationType.FAKE_CHAT_MESSAGE, 1.0F, 1,
                        "§e§l" + name + " §ejoined the game");
                case 7 -> HallucinationManager.force(sp, HallucinationType.NAME_WHISPER, 0.7F, 30,
                        name);
            }

            // Eventually clears (1% per 30s)
            if (Math.random() < 0.01) {
                clear(sp);
            }
        }
    }

    private static String pickFakeMsg() {
        String[] msgs = {
                "oi", "alguém aí?", "olha o que achei", "to com problema",
                "vai te dar tempo de chegar?", "voltei", "to indo até você",
                "viu aquilo?", "não consegui dormir", "achei sua base",
                "espera", "to morrendo", "to atrás de você", "?",
                "/spawn não funciona", "alguém ouve passos?"
        };
        return msgs[(int)(Math.random() * msgs.length)];
    }
}
