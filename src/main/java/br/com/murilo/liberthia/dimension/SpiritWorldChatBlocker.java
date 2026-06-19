package br.com.murilo.liberthia.dimension;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * v0.1.22 r54: <b>Spirit World Chat Blocker</b> — silencia o player no Outro
 * Lado. Cancela {@link ServerChatEvent} e {@link CommandEvent} pra qualquer
 * comando de comunicação enquanto o player está em
 * {@link SpiritDimension#SPIRIT_WORLD}.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Chat normal: bloqueado, mostra "Sua voz não chega daqui"</li>
 *   <li>/tell /msg /w /whisper /r /me /say: bloqueados</li>
 *   <li>OPs em creative ainda bloqueados (a curse não respeita rank)</li>
 *   <li>Comandos administrativos (/give /tp /op) continuam funcionando se
 *       o player for OP — só comandos de COMUNICAÇÃO são silenciados</li>
 * </ul>
 *
 * <h2>Saída do Spirit World</h2>
 * O player precisa encontrar uma forma física de sair (Prayer Book, Soul Sever
 * reverso, ou esperar fim de timer do Forbidden Tome). Chat NÃO é uma rota.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpiritWorldChatBlocker {

    /** Comandos de comunicação bloqueados quando em spirit world. */
    private static final Set<String> SILENCED_COMMANDS = Set.of(
            "tell", "msg", "w", "whisper", "r", "me", "say",
            "teammsg", "tm", "minecraft:tell", "minecraft:msg",
            "minecraft:w", "minecraft:say", "minecraft:me",
            "minecraft:teammsg", "minecraft:tm"
    );

    private SpiritWorldChatBlocker() {}

    /**
     * Cancela chat normal se o sender está em spirit. Server chat event
     * dispara antes da broadcast — cancela aqui e nada chega aos outros.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sp = event.getPlayer();
        if (sp == null) return;
        if (!SpiritDimension.isInSpiritWorld(sp)) return;
        event.setCanceled(true);
        sendBlockedMessage(sp);
    }

    /**
     * Cancela comando de comunicação se o sender é um player em spirit.
     * Distingue command source — só player; console/cmd block continuam
     * funcionando normalmente.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommand(CommandEvent event) {
        ParseResults<CommandSourceStack> parse = event.getParseResults();
        CommandSourceStack source = parse.getContext().getSource();
        // Só interessa se um player real está executando o comando
        if (!(source.getEntity() instanceof ServerPlayer sp)) return;
        if (!SpiritDimension.isInSpiritWorld(sp)) return;

        // Pega nome do nó raiz (o comando: "tell", "msg", etc)
        String rootName = getRootCommandName(parse);
        if (rootName == null) return;
        if (!SILENCED_COMMANDS.contains(rootName.toLowerCase())) return;

        event.setCanceled(true);
        sendBlockedMessage(sp);
    }

    /** Pega o nome do comando raiz parseado (primeiro nó do dispatch tree). */
    private static String getRootCommandName(ParseResults<CommandSourceStack> parse) {
        try {
            var nodes = parse.getContext().getNodes();
            if (nodes.isEmpty()) return null;
            CommandNode<CommandSourceStack> first = nodes.get(0).getNode();
            return first == null ? null : first.getName();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Mensagem in-game pra avisar o player. */
    private static void sendBlockedMessage(ServerPlayer sp) {
        sp.displayClientMessage(Component.literal(
                "§5§o✦ Sua voz não chega daqui."), true);
        sp.displayClientMessage(Component.literal(
                "§8§oO Outro Lado silencia tudo. Encontre uma saída."), false);
    }
}
