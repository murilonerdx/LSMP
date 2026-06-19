package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r27: Hook de chat — mensagens com prefixo {@link #PREFIX}
 * enviadas por player próximo de antena ativa são broadcast pra todas
 * antenas tunadas na mesma freq.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Mensagem começa com "{@code }" — verifica antena próxima
 *       (raio {@link #BROADCAST_RANGE})</li>
 *   <li>Se acha antena ativa: cancela chat público, dispara
 *       {@link AntennaNetwork#broadcastMessage}</li>
 *   <li>Se NÃO acha antena: deixa o chat normal (não estrague o gameplay)</li>
 * </ul>
 *
 * <h2>Por que prefixo invés de keybind</h2>
 * Funciona sem mod cliente custom. Qualquer player pode usar imediatamente.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AntennaChatHandler {

    /** Prefixo da mensagem pra broadcast via antena. */
    public static final String PREFIX = ">";
    /** Raio em blocos pra detecção de "perto de antena". */
    public static final double BROADCAST_RANGE = 8.0;

    private AntennaChatHandler() {}

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String msg = event.getMessage().getString();
        if (!msg.startsWith(PREFIX)) return;

        // Procura antena ativa próxima
        GlobalPos antennaPos = AntennaNetwork.findNearestActive(sender, BROADCAST_RANGE);
        if (antennaPos == null) {
            // Sem antena próxima — deixa chat normal acontecer
            // (player pode ter querido falar "> oi" normal)
            return;
        }

        String freq = AntennaNetwork.getFrequency(antennaPos);
        if (freq == null || freq.isEmpty()) return;

        // Cancela chat público
        event.setCanceled(true);
        // Broadcast pra antenas tunadas
        String content = msg.substring(PREFIX.length()).trim();
        if (content.isEmpty()) {
            sender.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§7Mensagem vazia. Use §e>seu_texto§7."), false);
            return;
        }
        MinecraftServer server = sender.server;
        AntennaNetwork.broadcastMessage(server, freq, sender, content, antennaPos);

        // Também ecoa pro player que falou (formato bonito)
        sender.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§5§l📡 §r§d[" + freq + "] §fvocê§7: §f" + content), false);
        LiberthiaMod.LOGGER.debug("[Antenna] {} → freq={} msg={}",
                sender.getName().getString(), freq, content);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        AntennaNetwork.clearAll();
    }
}
