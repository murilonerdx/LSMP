package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.event.ClearMatterPowersHandler;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.ClearMatterTeleportC2SPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lado-cliente: detecta shift+right-click no AR (sem mirar em bloco/entidade)
 * e manda packet pro server pra executar o teleport da Clear Matter armor.
 *
 * <p>{@code PlayerInteractEvent.RightClickEmpty} SÓ é fired no cliente — o
 * server não tem como saber dessa interação a não ser via packet.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class ClearMatterTeleportClientHandler {

    private ClearMatterTeleportClientHandler() {}

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (!(event.getEntity() instanceof LocalPlayer lp)) return;
        if (!lp.isShiftKeyDown()) return;
        // Pre-check no cliente: só envia packet se o set está completo, pra
        // evitar packet spam quando o player usa mão vazia em outras situações.
        if (!ClearMatterPowersHandler.hasFullClearSet(lp)) return;
        ModNetwork.CHANNEL.sendToServer(new ClearMatterTeleportC2SPacket());
    }
}
