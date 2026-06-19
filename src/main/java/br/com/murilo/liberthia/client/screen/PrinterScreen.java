package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.menu.PrinterMenu;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.PrinterPrintC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * GUI da Impressora — lista de "pendências" (relatórios do Computador vizinho),
 * slot de papel e botões Imprimir / Tudo. Selecione um relatório e clique
 * Imprimir → sai o livro (consome 1 papel do slot).
 */
@OnlyIn(Dist.CLIENT)
public class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {

    private static final int VISIBLE = 6;
    private static final int ROW_H = 12;
    private int selected = -1;
    private int scroll = 0;

    public PrinterScreen(PrinterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        if (!menu.getFiles().isEmpty()) selected = 0;
        addRenderableWidget(Button.builder(Component.literal("Imprimir"), b -> print(false))
                .bounds(leftPos + 44, topPos + 100, 60, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Tudo"), b -> print(true))
                .bounds(leftPos + 108, topPos + 100, 44, 20).build());
    }

    private void print(boolean all) {
        ModNetwork.sendToServer(new PrinterPrintC2SPacket(menu.getBlockPos(), all ? -1 : selected));
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF161018);
        drawBorder(g, x, y, imageWidth, imageHeight, 0xFF8A7050, 2);
        g.fill(x, y, x + imageWidth, y + 16, 0xFF241A12);

        // Lista de pendências
        int lx = x + 8, ly = y + 30, lw = imageWidth - 16;
        g.fill(lx, ly - 1, lx + lw, ly + VISIBLE * ROW_H + 1, 0xFF0C0A12);
        drawBorder(g, lx, ly - 1, lw, VISIBLE * ROW_H + 2, 0xFF5A4A30, 1);

        List<ComputerData.Entry> files = menu.getFiles();
        if (files.isEmpty()) {
            g.drawString(font, "§8Sem relatórios no Computador.", lx + 4, ly + 4, 0xFF887766, false);
        } else {
            for (int i = 0; i < VISIBLE; i++) {
                int idx = scroll + i;
                if (idx >= files.size()) break;
                int ry = ly + i * ROW_H;
                boolean sel = idx == selected;
                if (sel) g.fill(lx + 1, ry, lx + lw - 1, ry + ROW_H, 0xFF4A3A18);
                String nm = truncate(files.get(idx).name, 26);
                g.drawString(font, (sel ? "§e> " : "§7") + nm, lx + 3, ry + 2, sel ? 0xFFFFEE88 : 0xFFCCBBAA, false);
            }
        }

        // Slot de papel
        drawSlotBg(g, x + 16, y + 104, 0xFF241A12, 0xFFE0D8C0);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, "§6§lImpressora", titleLabelX, titleLabelY, 0xFFFFE8B0, false);
        g.drawString(font, "§7Pendências:", 8, 20, 0xFFCCBBAA, false);
        g.drawString(font, "§7Papel", 36, 96, 0xFFCCCCCC, false);
        g.drawString(font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9988AA, false);

        // Energia (FE) no canto direito da barra de título.
        String estr = menu.getEnergyStored() + "/" + menu.getMaxEnergy() + " FE";
        int ecol = menu.getEnergyStored() < br.com.murilo.liberthia.storage.PrinterBlockEntity.PRINT_COST
                ? 0xFFFF7755 : 0xFF66DDFF;
        g.drawString(font, estr, imageWidth - font.width(estr) - 8, 5, ecol, false);
    }

    @Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        int lx = leftPos + 8, ly = topPos + 30, lw = imageWidth - 16;
        if (mxd >= lx && mxd < lx + lw && myd >= ly && myd < ly + VISIBLE * ROW_H) {
            int idx = scroll + (int) ((myd - ly) / ROW_H);
            if (idx >= 0 && idx < menu.getFiles().size()) {
                selected = idx;
                return true;
            }
        }
        return super.mouseClicked(mxd, myd, button);
    }

    @Override
    public boolean mouseScrolled(double mxd, double myd, double delta) {
        int n = menu.getFiles().size();
        if (n > VISIBLE) {
            scroll = Math.max(0, Math.min(n - VISIBLE, scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mxd, myd, delta);
    }

    private void drawSlotBg(GuiGraphics g, int sx, int sy, int fill, int border) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, border);
        g.fill(sx, sy, sx + 16, sy + 16, fill);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color, int t) {
        g.fill(x, y, x + w, y + t, color);
        g.fill(x, y + h - t, x + w, y + h, color);
        g.fill(x, y, x + t, y + h, color);
        g.fill(x + w - t, y, x + w, y + h, color);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
