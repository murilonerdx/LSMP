package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.ArcaneMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** r184 — GUI procedural (g.fill) das máquinas Arcanas, tematizada pela cor de destaque do tipo. */
public class ArcaneMachineScreen extends AbstractContainerScreen<ArcaneMachineMenu> {
    private int ex, ey, ew, eh;

    public ArcaneMachineScreen(ArcaneMachineMenu m, Inventory inv, Component title) {
        super(m, inv, title);
        this.imageWidth = 176; this.imageHeight = 166;
        this.titleLabelY = 6; this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2, y = (this.height - imageHeight) / 2;
        int accent = menu.type.accentColor;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0C1014);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1A2026);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, accent);
        g.fill(x + 4, y + 16, x + imageWidth - 4, y + 70, 0xFF12161A);

        // barra de energia (esquerda)
        ex = x + 9; ey = y + 18; ew = 10; eh = 52;
        g.fill(ex - 1, ey - 1, ex + ew + 1, ey + eh + 1, 0xFF050608);
        g.fill(ex, ey, ex + ew, ey + eh, 0xFF0E2218);
        int fill = (int) (eh * menu.energyFrac());
        for (int i = 0; i < fill; i++) {
            float t = i / (float) Math.max(1, eh);
            int r = (int) (40 + 60 * t), gg = (int) (200 - 40 * t), b = (int) (110 + 120 * t);
            g.fill(ex, ey + eh - 1 - i, ex + ew, ey + eh - i, 0xFF000000 | (r << 16) | (gg << 8) | b);
        }

        // slots de entrada e saída
        for (int i = 0; i < menu.type.inputSlots; i++) slot(g, x + ArcaneMachineMenu.slotX(i, false) - 1, y + 34, accent);
        for (int j = 0; j < menu.type.outputSlots; j++) slot(g, x + ArcaneMachineMenu.slotX(j, true) - 1, y + 34, 0xFF60C0A0);

        // seta de progresso
        int ax = x + 88, ay = y + 38, aw = 24, ah = 12;
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF0E1216);
        int filled = (int) (aw * menu.progressFrac());
        for (int i = 0; i < filled; i++) g.fill(ax + i, ay + 2, ax + i + 1, ay + ah - 2, accent);
        for (int i = 0; i < 4; i++) g.fill(ax + aw + i, ay + 4 - i, ax + aw + i + 1, ay + ah - 4 + i, accent);

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF2E3640);
        for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF3A444E);
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF050608);
        g.fill(x, y, x + 1, y + 18, 0xFF050608);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF12161A);
    }

    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, this.title.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x6688AA, false);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        if (mx >= ex && mx < ex + ew && my >= ey && my < ey + eh)
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("⚡ " + String.format("%,d", menu.energy()) + " / " + String.format("%,d", menu.maxEnergy()) + " FE")
                            .withStyle(ChatFormatting.GREEN)), mx, my);
        renderTooltip(g, mx, my);
    }
}
