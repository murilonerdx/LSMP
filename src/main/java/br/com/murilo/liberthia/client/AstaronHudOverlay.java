package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r187 — HUD da Escala de Astaron (canto superior direito) quando o item está na mão: barra de
 * radiação (verde→vermelho) + nº de criaturas cósmicas.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AstaronHudOverlay {
    private AstaronHudOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post e) {
        if (e.getOverlay() != net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.type()) return; // 1×/frame
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        boolean holds = mc.player.getMainHandItem().is(ModItems.ASTARON_SCALE.get())
                || mc.player.getOffhandItem().is(ModItems.ASTARON_SCALE.get());
        if (!holds) return;

        int rad = AstaronClientState.radiation, count = AstaronClientState.cosmicCount;
        GuiGraphics g = e.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw - 124, y = 6;
        g.fill(x - 2, y - 2, x + 122, y + 32, 0xB0000000);
        g.drawString(mc.font, "§5✦ Astaron", x, y, 0xFFFFFF, false);
        // barra de radiação
        int barW = 116, fill = (int) (barW * (rad / 100.0f));
        int col = rad >= 80 ? 0xFFFF3030 : rad >= 50 ? 0xFFFFAA00 : 0xFF30FF60;
        g.fill(x, y + 12, x + barW, y + 18, 0xFF202020);
        g.fill(x, y + 12, x + fill, y + 18, col);
        g.drawString(mc.font, "§7Rad §f" + rad + "  §7Cósm §d" + count, x, y + 22, 0xFFFFFF, false);
    }
}
