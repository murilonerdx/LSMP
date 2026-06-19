package br.com.murilo.liberthia.client.hud;

import br.com.murilo.liberthia.magic.casting.RecastData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * v0.1.24 r102: <b>Recast Counter</b> — mostra quantos recasts disponíveis
 * acima do hotbar.
 *
 * <p>Read-only do {@link RecastData} — server e client compartilham via
 * registry (em memória).
 */
public class RecastCounterOverlay implements IGuiOverlay {

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics gfx,
                        float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int charges = RecastData.getRemainingCharges(mc.player);
        if (charges <= 0) return;

        // Acima do hotbar — desenha pips horizontais
        int x = screenWidth / 2 - 80;
        int y = screenHeight - 50;
        String text = "§b✦ Recasts: " + charges;
        gfx.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFFFF);
    }
}
