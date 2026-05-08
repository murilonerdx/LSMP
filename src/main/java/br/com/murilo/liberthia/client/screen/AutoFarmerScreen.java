package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.AutoFarmerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class AutoFarmerScreen extends AbstractContainerScreen<AutoFarmerMenu> {
    public AutoFarmerScreen(AutoFarmerMenu m, Inventory inv, Component title) {
        super(m, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }
    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF101020);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1A1A30);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3030AA);
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF181830);
        slot(g, x + 56 - 1, y + 35 - 1, 0xFFAA60FF);
        slot(g, x + 116 - 1, y + 35 - 1, 0xFFC080FF);

        // Energia
        int bx = x + 8, by = y + 18, bw = 8, bh = 50;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF3B1A5C);
        g.fill(bx, by, bx + bw, by + bh, 0xFF080012);
        float ef = menu.energyFrac();
        int efill = (int) (bh * ef);
        if (efill > 0) {
            int top = by + bh - efill;
            for (int i = 0; i < efill; i++) {
                float t = i / (float) Math.max(1, efill);
                int r = (int) (90 + 165 * t);
                int gC = (int) (10 + 50 * t);
                int b = (int) (160 + 95 * t);
                g.fill(bx, top + i, bx + bw, top + i + 1, 0xFF000000 | (r << 16) | (gC << 8) | b);
            }
        }

        // Cooldown indicator (vertical)
        int cx = x + 80, cy = y + 35, cw = 30, ch = 18;
        g.fill(cx, cy, cx + cw, cy + ch, 0xFF1A1A30);
        float cf = 1f - menu.cooldownFrac();
        int cfill = (int)(cw * cf);
        for (int i = 0; i < cfill; i++) {
            float t = i / (float) Math.max(1, cw);
            g.fill(cx + i, cy + 4, cx + i + 1, cy + ch - 4,
                    0xFF000000 | ((int)(60 + 100*t) << 16) | ((int)(180 - 80*t) << 8) | 90);
        }
        for (int i = 0; i < 4; i++) g.fill(cx + cw + i, cy + 4 - i, cx + cw + i + 1, cy + ch - 4 + i, 0xFF40C080);

        // player inv
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF181830);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF6060A0);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF8080C0);
    }
    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF20204A);
        g.fill(x, y, x + 1, y + 18, 0xFF20204A);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1A1A30);
    }
    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font,
                Component.translatable("container.liberthia.auto_farmer")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9090CC, false);
    }
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        if (mx >= x + 8 && mx < x + 16 && my >= y + 18 && my < y + 68) {
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal(menu.rawEnergy() + " / " + menu.rawEnergyMax() + " FE")
                            .withStyle(ChatFormatting.LIGHT_PURPLE)
            ), mx, my);
        }
        renderTooltip(g, mx, my);
    }
}
