package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.SpiritMagicItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r31: Véu da Testemunha Silenciosa — você ouve sofrimento alheio.
 *
 * <p>Quando QUALQUER player no servidor toma dano, o player que carrega o
 * Véu recebe um sussurro em actionbar com nome + dano sofrido.
 *
 * <p>Hauntologia aplicada: o presente é assombrado pelo que não foi seu.
 * Você se torna a testemunha cósmica do sofrimento alheio.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class WitnessVeilHandler {

    private WitnessVeilHandler() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getAmount() < 1) return;
        var server = event.getEntity().getServer();
        if (server == null) return;

        String victimName = victim.getName().getString();
        float dmg = event.getAmount();
        String sourceName;
        var srcEntity = event.getSource().getEntity();
        if (srcEntity != null) {
            sourceName = srcEntity.getName().getString();
        } else {
            sourceName = event.getSource().getMsgId(); // generic key
        }

        Component whisper = Component.literal(
                String.format("§8§o»%s sangra %.1f§r§8§o por %s«",
                        victimName, dmg, sourceName));

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p == victim) continue;
            if (!SpiritMagicItems.WhisperingVeil.hasVeil(p)) continue;
            p.displayClientMessage(whisper, true);
        }
    }
}
