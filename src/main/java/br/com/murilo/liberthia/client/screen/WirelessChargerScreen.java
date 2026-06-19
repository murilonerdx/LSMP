package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.WirelessChargerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * GUI customizada do Wireless Charger — sem texturas, tudo desenhado com
 * primitivas (g.fill, g.drawString). Tema lilás-elétrico com:
 * <ul>
 *   <li>Frame com borda dupla luminosa</li>
 *   <li>Barra de energia GIGANTE central com gradiente animado</li>
 *   <li>Indicador circular de raio com pulso</li>
 *   <li>Contador de jogadores carregando</li>
 *   <li>Linhas-guia mostrando como funciona</li>
 * </ul>
 */
public class WirelessChargerScreen extends AbstractContainerScreen<WirelessChargerMenu> {

    private long openedAt;

    public WirelessChargerScreen(WirelessChargerMenu m, Inventory inv, Component title) {
        super(m, inv, title);
        this.imageWidth = 192;
        this.imageHeight = 192;
        this.titleLabelY = 6;
        this.inventoryLabelY = 98;
    }

    @Override
    protected void init() {
        super.init();
        this.openedAt = System.currentTimeMillis();
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        long elapsed = System.currentTimeMillis() - openedAt;
        float wave = (float) Math.sin(elapsed / 300.0);
        float pulse = 0.5f + 0.5f * wave;

        // ---- Outer frame (camadas de roxo escuro) ----
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF06010C);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF120420);
        // Highlight superior animado
        int hlR = (int) (90 + 60 * pulse);
        int hlG = 30;
        int hlB = (int) (180 + 60 * pulse);
        int hlColor = 0xFF000000 | (hlR << 16) | (hlG << 8) | hlB;
        g.fill(x, y, x + imageWidth, y + 2, hlColor);
        g.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, 0xFF3B1A5C);
        // Bordas laterais
        g.fill(x, y, x + 2, y + imageHeight, 0xFF3B1A5C);
        g.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, 0xFF3B1A5C);

        // ---- Cabeçalho (faixa decorativa) ----
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 18, 0xFF6B2A8C);

        // ---- Anel/circulo de raio (lado esquerdo) ----
        int circleX = x + 36;
        int circleY = y + 56;
        int radius = 22;
        drawAnimatedRing(g, circleX, circleY, radius, pulse);

        // Texto do range no centro do círculo
        String circleStr = menu.isCrossDim() ? "∞" : (menu.range() + "m");
        int crw = font.width(circleStr);
        g.drawString(font, circleStr, circleX - crw / 2, circleY - 4, 0xFFFFFFFF, false);

        // ---- Barra de energia (lado direito, vertical grande) ----
        int barX = x + 90;
        int barY = y + 30;
        int barW = 14;
        int barH = 60;
        // Frame
        g.fill(barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, 0xFF6B2A8C);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF050008);

        // Fill com gradiente animado
        float ef = menu.energyFrac();
        int efill = (int) (barH * ef);
        if (efill > 0) {
            int top = barY + barH - efill;
            for (int i = 0; i < efill; i++) {
                float t = i / (float) Math.max(1, efill);
                int r = (int) (90 + 165 * t + 30 * pulse);
                int gC = (int) (10 + 50 * t);
                int b = (int) (160 + 95 * t);
                if (r > 255) r = 255;
                if (b > 255) b = 255;
                g.fill(barX, top + i, barX + barW, top + i + 1, 0xFF000000 | (r << 16) | (gC << 8) | b);
            }
            // Brilho superior pulsante
            int brightTop = (int) (255 * pulse * 0.6f);
            int hlBar = 0xFF000000 | (brightTop << 16) | (brightTop << 8) | 255;
            g.fill(barX, top, barX + barW, top + 1, hlBar);
        }
        // Marcas de 25% / 50% / 75%
        for (int p = 1; p < 4; p++) {
            int markY = barY + barH - (barH * p / 4);
            g.fill(barX - 1, markY, barX + barW + 1, markY + 1, 0x66FFFFFF);
        }

        // ---- Stats panel (direita) ----
        int sx = x + 116;
        int sy = y + 32;
        // Players carregando
        int pc = menu.chargingPlayers();
        ChatFormatting playerColor = pc > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        g.drawString(font, Component.literal("§lJogadores"), sx, sy, 0xFFFFFFFF, false);
        g.drawString(font, Component.literal("⚡ " + pc).withStyle(playerColor), sx, sy + 10, 0xFFFFFFFF, false);
        // Tier energia
        g.drawString(font, Component.literal("§lFE"), sx, sy + 22, 0xFFFFFFFF, false);
        int pct = (int) (menu.energyFrac() * 100);
        ChatFormatting feColor = pct > 50 ? ChatFormatting.GREEN
                : pct > 20 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        g.drawString(font, Component.literal(pct + "%").withStyle(feColor), sx, sy + 32, 0xFFFFFFFF, false);
        // Range
        String rangeStr;
        ChatFormatting rangeColor;
        if (menu.isCrossDim()) {
            rangeStr = "∞ Cross-Dim";
            rangeColor = ChatFormatting.LIGHT_PURPLE;
        } else {
            rangeStr = "◯ " + menu.range() + " blocos";
            rangeColor = ChatFormatting.AQUA;
        }
        g.drawString(font, Component.literal("§lRaio"), sx, sy + 44, 0xFFFFFFFF, false);
        g.drawString(font, Component.literal(rangeStr).withStyle(rangeColor),
                sx, sy + 54, 0xFFFFFFFF, false);

        // Upgrade slot label
        g.drawString(font, Component.literal("§7§oUpgrade"), x + 142, y + 46, 0xFFFFFFFF, false);
        // Slot frame
        slot(g, x + 152 - 1, y + 56 - 1, 0xFFFFE040);

        // ---- Player inventory panel ----
        g.fill(x + 6, y + 106, x + imageWidth - 6, y + imageHeight - 6, 0xFF0A031A);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 110 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 168 - 1, 0xFFAA40E8);
    }

    /** Desenha um anel circular animado representando o raio do charger. */
    private void drawAnimatedRing(GuiGraphics g, int cx, int cy, int radius, float pulse) {
        // Círculo externo (raio)
        for (int a = 0; a < 360; a += 5) {
            double rad = Math.toRadians(a);
            int px = cx + (int) (Math.cos(rad) * radius);
            int py = cy + (int) (Math.sin(rad) * radius);
            int alpha = (int) (180 + 60 * pulse);
            int color = (alpha << 24) | 0x00CC44FF;
            g.fill(px, py, px + 1, py + 1, color);
        }
        // Pontos pulsando dentro do raio
        int pts = 6;
        for (int i = 0; i < pts; i++) {
            double a = (i / (double) pts) * Math.PI * 2 + pulse * 2;
            double r = radius * 0.6 * pulse;
            int px = cx + (int) (Math.cos(a) * r);
            int py = cy + (int) (Math.sin(a) * r);
            g.fill(px - 1, py - 1, px + 2, py + 2, 0xFFE0A0FF);
        }
        // Centro
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1B0830);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font,
                Component.translatable("container.liberthia.wireless_charger")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFFFF, true);
        g.drawString(font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        // Energy bar tooltip
        if (mx >= x + 88 && mx < x + 106 && my >= y + 28 && my < y + 92) {
            g.renderComponentTooltip(font, List.of(
                    Component.literal(formatFE(menu.rawEnergy()) + " / " + formatFE(menu.rawEnergyMax()) + " FE")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal("Buffer interno").withStyle(ChatFormatting.GRAY),
                    Component.empty(),
                    Component.literal("Carrega cells/itens FE de jogadores próximos")
                            .withStyle(ChatFormatting.DARK_GRAY)
            ), mx, my);
        }
        // Range circle tooltip
        if (mx >= x + 14 && mx < x + 58 && my >= y + 34 && my < y + 78) {
            g.renderComponentTooltip(font, List.of(
                    Component.literal("Raio de carregamento").withStyle(ChatFormatting.AQUA),
                    Component.literal("§7" + menu.range() + " blocos em todas as direções"),
                    Component.empty(),
                    Component.literal("§8Jogadores em creative/spectator não carregam")
            ), mx, my);
        }
        renderTooltip(g, mx, my);
    }

    private String formatFE(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }
}
