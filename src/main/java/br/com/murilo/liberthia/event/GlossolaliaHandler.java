package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.SpiritMagicItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r31: Língua de Glossolália — chat scrambler.
 *
 * <p>Quando um player com a Língua manda mensagem no chat:
 * <ul>
 *   <li>Players que também têm a Língua → recebem texto NORMAL (cult speak)</li>
 *   <li>Players sem a Língua → recebem texto EMBARALHADO em "Enoquiano"
 *       (símbolos quase-Latin como ∆◊◉Ψϟ)</li>
 * </ul>
 *
 * <p>Permite comunicação secreta entre cultistas. Para "decodificar" você
 * precisa segurar (ou ter em inv) a sua própria Língua.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class GlossolaliaHandler {

    private GlossolaliaHandler() {}

    /**
     * r34 FIX: era HIGHEST priority com fallback errado. Agora HIGHEST pra
     * cancelar antes de outros mods + log explícito pra debug. Também
     * usa Forge MessageDecorator se disponível.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        boolean has = SpiritMagicItems.TongueOfOldOnes.hasTongue(sender);
        if (!has) return;

        String original = event.getMessage().getString();
        if (original.isEmpty()) return;
        String scrambled = SpiritMagicItems.TongueOfOldOnes.scramble(original);
        LiberthiaMod.LOGGER.info("[Glossolalia] {} OK scramble: '{}' → '{}'",
                sender.getName().getString(), original, scrambled);

        // r34: Forge ServerChatEvent IS cancelable in 1.20.1
        event.setCanceled(true);

        Component normal = Component.literal(
                "§5⌖ §d" + sender.getName().getString() + " §7▸ §f" + original);
        Component cult = Component.literal(
                "§5⌖ §d" + sender.getName().getString() + " §7▸ §8§o" + scrambled);

        // Manda normal pros que TÊM tongue + scramble pros outros
        for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
            if (SpiritMagicItems.TongueOfOldOnes.hasTongue(p) || p == sender) {
                p.sendSystemMessage(normal);
            } else {
                p.sendSystemMessage(cult);
            }
        }
    }
}
