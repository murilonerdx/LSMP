package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.TechMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** r180c — GUI genérica das máquinas tech (slot in→out, barra de energia, progresso). */
public class TechMachineScreen extends AbstractContainerScreen<TechMachineMenu> {

    public TechMachineScreen(TechMachineMenu m, Inventory inv, Component title) {
        super(m, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2, y = (this.height - imageHeight) / 2;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF101418);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1C2228);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3A444E);

        // barra de energia (esquerda)
        int bx = x + 10, by = y + 18, bw = 8, bh = 52;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF0A0C0E);
        g.fill(bx, by, bx + bw, by + bh, 0xFF20140A);
        int fill = (int) (bh * menu.energyFrac());
        for (int i = 0; i < fill; i++) {
            float t = i / (float) Math.max(1, bh);
            int r = (int) (60 + 200 * t), gg = (int) (200 - 40 * t);
            g.fill(bx, by + bh - 1 - i, bx + bw, by + bh - i, 0xFF000000 | (r << 16) | (gg << 8) | 0x18);
        }

        slot(g, x + 56 - 1, y + 35 - 1, 0xFF4A88C8);
        slot(g, x + 116 - 1, y + 35 - 1, 0xFF60C0A0);

        // seta de progresso
        int ax = x + 80, ay = y + 38, aw = 28, ah = 12;
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF0E1216);
        int filled = (int) (aw * menu.progressFrac());
        for (int i = 0; i < filled; i++)
            g.fill(ax + i, ay + 2, ax + i + 1, ay + ah - 2, 0xFF40C0FF);
        for (int i = 0; i < 4; i++) g.fill(ax + aw + i, ay + 4 - i, ax + aw + i + 1, ay + ah - 4 + i, 0xFF40C0FF);

        // player inv
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF2E3640);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF3A444E);
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF0A0C0E);
        g.fill(x, y, x + 1, y + 18, 0xFF0A0C0E);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF12161A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, this.title.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x6688AA, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        int x = (this.width - imageWidth) / 2, y = (this.height - imageHeight) / 2;
        if (mx >= x + 9 && mx < x + 19 && my >= y + 17 && my < y + 71) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("Energia: " + (int) (menu.energyFrac() * 100) + "%").withStyle(ChatFormatting.GREEN)), mx, my);
        }
        renderTooltip(g, mx, my);
    }
}
