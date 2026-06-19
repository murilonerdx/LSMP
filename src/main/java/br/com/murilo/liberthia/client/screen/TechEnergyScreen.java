package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.block.entity.ITechEnergyUI;
import br.com.murilo.liberthia.menu.TechEnergyMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * r182 — GUI de energia unificada e bonita (procedural g.fill, sem PNG): barra de
 * energia com gradiente + número exato, slot de combustível/carga e barra de
 * atividade (queima nos geradores / geração nos passivos). Adapta-se ao layout.
 */
public class TechEnergyScreen extends AbstractContainerScreen<TechEnergyMenu> {

    private int ex, ey, ew, eh;   // bounds da barra de energia (p/ tooltip)
    private int ax, ay, aw, ah;   // bounds da barra de atividade

    public TechEnergyScreen(TechEnergyMenu m, Inventory inv, Component title) {
        super(m, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2, y = (this.height - imageHeight) / 2;
        // painel
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0C1014);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1A2026);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3A444E);
        g.fill(x + 4, y + 16, x + imageWidth - 4, y + 70, 0xFF12161A);

        // ── barra de energia (esquerda) ──
        ex = x + 10; ey = y + 18; ew = 12; eh = 52;
        g.fill(ex - 1, ey - 1, ex + ew + 1, ey + eh + 1, 0xFF050608);
        g.fill(ex, ey, ex + ew, ey + eh, 0xFF0E2218);
        int fill = (int) (eh * menu.energyFrac());
        for (int i = 0; i < fill; i++) {
            float t = i / (float) Math.max(1, eh);
            int r = (int) (40 + 60 * t), gg = (int) (200 - 40 * t), b = (int) (110 + 130 * t);
            g.fill(ex, ey + eh - 1 - i, ex + ew, ey + eh - i, 0xFF000000 | (r << 16) | (gg << 8) | b);
        }
        // brilho no topo do nível
        if (fill > 0) g.fill(ex, ey + eh - 1 - fill, ex + ew, ey + eh - fill, 0xFFB0FFE0);

        // ── slot central (combustível/carga) — só geradores e células/charger ──
        if (menu.layout() == ITechEnergyUI.LAYOUT_GENERATOR || menu.layout() == ITechEnergyUI.LAYOUT_CHARGER) {
            int hi = menu.layout() == ITechEnergyUI.LAYOUT_GENERATOR ? 0xFFC86040 : 0xFF60C0A0;
            slot(g, x + 80 - 1, y + 35 - 1, hi);
        }

        // ── barra de atividade ──
        ax = x + 71; ay = y + 56; aw = 36; ah = 5;
        if (menu.layout() == ITechEnergyUI.LAYOUT_GENERATOR) {
            drawActivity(g, menu.activityFrac(), 0xFFFF7A20, 0xFFFFD060);  // queima (laranja→amarelo)
        } else if (menu.layout() == ITechEnergyUI.LAYOUT_PASSIVE) {
            ax = x + 60; ay = y + 40; aw = 56; ah = 8;
            drawActivity(g, menu.activityFrac(), 0xFF30B0FF, 0xFFB0E8FF);  // geração (azul→ciano)
        } else if (menu.layout() == ITechEnergyUI.LAYOUT_CONSUMER) {
            ax = x + 60; ay = y + 40; aw = 56; ah = 8;
            drawActivity(g, menu.activityFrac(), 0xFF8A40D0, 0xFFD0A0FF);  // ativo (roxo→lilás)
        } else {
            aw = 0; ah = 0; // charger/cell: sem barra de atividade
        }

        // player inv
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF2E3640);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF3A444E);
    }

    private void drawActivity(GuiGraphics g, float frac, int c1, int c2) {
        g.fill(ax - 1, ay - 1, ax + aw + 1, ay + ah + 1, 0xFF050608);
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF20160C);
        int f = (int) (aw * frac);
        for (int i = 0; i < f; i++) {
            float t = i / (float) Math.max(1, aw);
            int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
            int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
            int r = (int) (r1 + (r2 - r1) * t), gg = (int) (g1 + (g2 - g1) * t), b = (int) (b1 + (b2 - b1) * t);
            g.fill(ax + i, ay, ax + i + 1, ay + ah, 0xFF000000 | (r << 16) | (gg << 8) | b);
        }
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF050608);
        g.fill(x, y, x + 1, y + 18, 0xFF050608);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF12161A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, this.title.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x6688AA, false);
        // % de energia ao lado da barra
        String pct = (int) (menu.energyFrac() * 100) + "%";
        g.drawString(this.font, pct, 26, 22, 0x80FFC0, false);
        if (menu.layout() == ITechEnergyUI.LAYOUT_PASSIVE)
            g.drawString(this.font, Component.translatable("gui.liberthia.generating"), 60, 30, 0x60C0FF, false);
        else if (menu.layout() == ITechEnergyUI.LAYOUT_CHARGER)
            g.drawString(this.font, Component.translatable("gui.liberthia.charge_slot"), 50, 60, 0x60C0A0, false);
        else if (menu.layout() == ITechEnergyUI.LAYOUT_CONSUMER)
            g.drawString(this.font, Component.translatable(menu.activity() > 0 ? "gui.liberthia.field_active" : "gui.liberthia.field_idle"), 60, 30,
                    menu.activity() > 0 ? 0xC080FF : 0x806080, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        if (within(mx, my, ex, ey, ew, eh)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.literal("⚡ " + String.format("%,d", menu.energy()) + " / " + String.format("%,d", menu.maxEnergy()) + " FE")
                    .withStyle(ChatFormatting.GREEN));
            tip.add(Component.literal((int) (menu.energyFrac() * 100) + "%").withStyle(ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, tip, mx, my);
        } else if (aw > 0 && menu.layout() != ITechEnergyUI.LAYOUT_CONSUMER && within(mx, my, ax, ay, aw, ah)) {
            Component label = menu.layout() == ITechEnergyUI.LAYOUT_GENERATOR
                    ? Component.translatable("gui.liberthia.burning", menu.activity())
                    : Component.translatable("gui.liberthia.gen_rate", menu.activity());
            g.renderComponentTooltip(this.font, List.of(label.copy().withStyle(ChatFormatting.GOLD)), mx, my);
        }
        renderTooltip(g, mx, my);
    }

    private boolean within(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }
}
