package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.block.entity.DarkMatterGeneratorBlockEntity;
import br.com.murilo.liberthia.menu.DarkMatterGeneratorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI do Gerador de Matéria Escura — totalmente desenhada via {@code fill}.
 *
 * <p>Layout (176×186):
 * <pre>
 *   ┌──────────────────────────┐  17px frame escuro
 *   │  Gerador de Matéria       │  título dourado
 *   ├───────────────────┬──────┤
 *   │ [chama] [fuel]   │       │  fuel (44,36) + chama animada
 *   │           [▲S]   │ █████ │  upgrades (102, 18/36/54)
 *   │           [♦E]   │ █████ │  energy bar (152,18,14×64)
 *   │           [+C]   │ █████ │
 *   ├──────────────────┴───────┤
 *   │  inventário do jogador    │
 *   └──────────────────────────┘
 * </pre>
 */
public class DarkMatterGeneratorScreen extends AbstractContainerScreen<DarkMatterGeneratorMenu> {

    // Tema
    private static final int FRAME_OUTER  = 0xFF0E0212;
    private static final int FRAME_INNER  = 0xFF1B0830;
    private static final int FRAME_HILITE = 0xFF3B1A5C;
    private static final int PANEL_BG     = 0xFF120420;
    private static final int PANEL_GRID   = 0xFF1F0A36;
    private static final int LABEL        = 0xFFB48EE6;
    private static final int LABEL_ACCENT = 0xFFFF80FF;

    public DarkMatterGeneratorScreen(DarkMatterGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        RenderSystem.enableBlend();

        // 1) Painel principal
        drawPanel(g, x, y, imageWidth, imageHeight, FRAME_OUTER, FRAME_INNER, FRAME_HILITE);

        // 2) Faixa de título
        drawGradientV(g, x + 6, y + 4, imageWidth - 12, 12, 0xFF2B0F4D, 0xFF120420);

        // 3) Área superior da máquina (y=16..72) — espaço pra texto inv abaixo
        int areaX = x + 6, areaY = y + 16, areaW = imageWidth - 12, areaH = 56;
        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, PANEL_BG);
        for (int gy = areaY + 4; gy < areaY + areaH; gy += 8)
            g.fill(areaX + 2, gy, areaX + areaW - 2, gy + 1, PANEL_GRID);
        outline(g, areaX, areaY, areaW, areaH, FRAME_HILITE);

        // 4) Slot de combustível (44, 35) — bate com o menu
        drawSlotFrame(g, x + 44 - 1, y + 35 - 1, 0xFF3D1860, 0xFF7F40C8);

        // 5) Chama animada à esquerda
        int flameX = x + 22, flameY = y + 31, flameW = 14, flameH = 18;
        g.fill(flameX, flameY, flameX + flameW, flameY + flameH, 0xFF1A0822);
        outline(g, flameX, flameY, flameW, flameH, 0xFF44195F);
        if (menu.isBurning()) {
            float burn = menu.burnFrac();
            int fh = (int) ((flameH - 2) * burn);
            for (int i = 0; i < fh; i++) {
                int yy = flameY + flameH - 1 - i;
                float t = 1f - (i / (float) Math.max(1, fh));
                int r = (int) (140 + 115 * t);
                int gC = (int) (40 + 100 * t);
                int b = (int) (180 + 60 * t);
                g.fill(flameX + 1, yy, flameX + flameW - 1, yy + 1, 0xFF000000 | (r << 16) | (gC << 8) | b);
            }
        }

        // 6) Slots de upgrade (102,18 / 36 / 54) — bate com o menu
        drawUpgradeFrame(g, x + 102 - 1, y + 18 - 1, 0xFFAA7820);
        drawUpgradeFrame(g, x + 102 - 1, y + 36 - 1, 0xFF40A0E0);
        drawUpgradeFrame(g, x + 102 - 1, y + 54 - 1, 0xFFC060FF);

        // 7) Barra de energia (x=152, y=18, 14×52)
        int barX = x + 152, barY = y + 18, barW = 14, barH = 52;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF3B1A5C);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF080012);
        for (int t = 1; t < 4; t++) {
            int ty = barY + (barH * t) / 4;
            g.fill(barX, ty, barX + 2, ty + 1, 0xFF6A2C9E);
            g.fill(barX + barW - 2, ty, barX + barW, ty + 1, 0xFF6A2C9E);
        }
        float frac = menu.energyFrac();
        int filled = (int) (barH * frac);
        if (filled > 0) {
            int top = barY + barH - filled;
            for (int i = 0; i < filled; i++) {
                float t = i / (float) Math.max(1, filled);
                int r = (int) (90 + 165 * t);
                int gC = (int) (10 + 50 * t);
                int b = (int) (160 + 95 * t);
                g.fill(barX, top + i, barX + barW, top + i + 1,
                        0xFF000000 | (r << 16) | (gC << 8) | b);
            }
            int topGlow = barY + barH - filled;
            g.fill(barX, topGlow, barX + barW, topGlow + 1, 0xFFFFCCFF);
        }

        // 8) Painel do inventário do jogador (frames batendo com slots: y=84/102/120 e y=142)
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF120420);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotFrame(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF2A0D44, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            drawSlotFrame(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF44195F, 0xFF7F40C8);
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h, int outer, int inner, int hilite) {
        g.fill(x, y, x + w, y + h, outer);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, inner);
        // 1px hilite top + left
        g.fill(x + 1, y + 1, x + w - 1, y + 2, hilite);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, hilite);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y, int dark, int hilite) {
        g.fill(x, y, x + 18, y + 1, dark);
        g.fill(x, y, x + 1, y + 18, dark);
        g.fill(x, y + 17, x + 18, y + 18, hilite);
        g.fill(x + 17, y, x + 18, y + 18, hilite);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1B0830);
    }

    private void drawUpgradeFrame(GuiGraphics g, int x, int y, int accent) {
        // moldura colorida + canto luminoso
        g.fill(x - 1, y - 1, x + 19, y + 19, accent);
        g.fill(x, y, x + 18, y + 18, 0xFF1B0830);
        // diagonal hilite
        g.fill(x, y, x + 18, y + 1, mix(accent, 0xFFFFFFFF, 0.3f));
        g.fill(x, y, x + 1, y + 18, mix(accent, 0xFFFFFFFF, 0.3f));
    }

    private void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawGradientV(GuiGraphics g, int x, int y, int w, int h, int top, int bot) {
        for (int i = 0; i < h; i++) {
            float t = i / (float) Math.max(1, h - 1);
            g.fill(x, y + i, x + w, y + i + 1, mix(top, bot, t));
        }
    }

    private static int mix(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int oa = (int) (aa + (ba - aa) * t);
        int or = (int) (ar + (br - ar) * t);
        int og = (int) (ag + (bg - ag) * t);
        int ob = (int) (ab + (bb - ab) * t);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Título
        g.drawString(this.font,
                Component.translatable("container.liberthia.dark_matter_generator")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                this.titleLabelX, this.titleLabelY, 0xFFFFFF, true);
        // Inventory label
        g.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x9966CC, false);

        // Status em duas linhas abaixo do slot de combustível.
        int rate = menu.getBlockEntity() != null
                ? menu.getBlockEntity().currentFePerTick()
                : DarkMatterGeneratorBlockEntity.FE_PER_TICK;
        int e = menu.rawEnergy(), max = menu.rawEnergyMax();
        g.drawString(this.font, Component.literal("Rate: " + formatFE(rate) + " FE/t")
                .withStyle(ChatFormatting.GOLD), 8, 55, 0xFFFFFF, false);
        g.drawString(this.font, Component.literal(formatFE(e) + " / " + formatFE(max) + " FE")
                .withStyle(ChatFormatting.LIGHT_PURPLE), 8, 64, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        // Tooltips
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Energy bar tooltip
        int barX = x + 152, barY = y + 18, barW = 14, barH = 64;
        if (mouseX >= barX && mouseX < barX + barW && mouseY >= barY && mouseY < barY + barH) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(menu.rawEnergy() + " / " + menu.rawEnergyMax() + " FE")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            lines.add(Component.literal("Buffer de energia").withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }

        // Tooltips dos slots de upgrade
        upgradeTooltip(g, mouseX, mouseY, x + 102, y + 18, "Velocidade",
                "+" + (menu.getBlockEntity() == null ? 0 : menu.getBlockEntity().speedUpgradeCount() * 100) + "% FE/tick",
                "Acelera a queima e a saída de energia.", ChatFormatting.GOLD);
        upgradeTooltip(g, mouseX, mouseY, x + 102, y + 36, "Eficiência",
                "+" + (menu.getBlockEntity() == null ? 0 : menu.getBlockEntity().efficiencyUpgradeCount() * 50) + "% FE/bloco",
                "Cada bloco rende mais energia.", ChatFormatting.AQUA);
        upgradeTooltip(g, mouseX, mouseY, x + 102, y + 54, "Capacidade",
                "+" + (menu.getBlockEntity() == null ? 0 : menu.getBlockEntity().capacityUpgradeCount() * 100) + "% buffer",
                "Aumenta o buffer interno.", ChatFormatting.LIGHT_PURPLE);

        // Chama tooltip
        int flameX = x + 22, flameY = y + 32, flameW = 14, flameH = 22;
        if (mouseX >= flameX && mouseX < flameX + flameW && mouseY >= flameY && mouseY < flameY + flameH) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("Combustão").withStyle(ChatFormatting.GOLD));
            if (menu.isBurning()) {
                lines.add(Component.literal((int)(menu.burnFrac() * 100) + "% restante")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                lines.add(Component.literal("Inativo").withStyle(ChatFormatting.GRAY));
            }
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }

        renderTooltip(g, mouseX, mouseY);
    }

    private void upgradeTooltip(GuiGraphics g, int mx, int my, int sx, int sy,
                                String name, String value, String desc, ChatFormatting accent) {
        if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
            // Só mostra se o slot estiver vazio (caso contrário o tooltip do item já cobre).
            int idx = -1;
            if (sy == this.topPos + 18) idx = 37;
            else if (sy == this.topPos + 36) idx = 38;
            else if (sy == this.topPos + 54) idx = 39;
            if (idx >= 0 && idx < this.menu.slots.size() && !this.menu.slots.get(idx).hasItem()) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(name).withStyle(accent, ChatFormatting.BOLD));
                lines.add(Component.literal(value).withStyle(ChatFormatting.WHITE));
                lines.add(Component.literal(desc).withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(this.font, lines, mx, my);
            }
        }
    }

    /** Formata FE compacto: 12345 → 12.3k, 1234567 → 1.23M. */
    private static String formatFE(int v) {
        if (v < 1000) return Integer.toString(v);
        if (v < 1_000_000) return String.format("%.1fk", v / 1000.0);
        if (v < 1_000_000_000) return String.format("%.2fM", v / 1_000_000.0);
        return String.format("%.2fB", v / 1_000_000_000.0);
    }
}
