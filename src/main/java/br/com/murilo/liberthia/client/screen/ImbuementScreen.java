package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.ImbuementMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * v0.1.22 r73: Imbuement Table GUI — procedural rendering.
 */
@OnlyIn(Dist.CLIENT)
public class ImbuementScreen extends AbstractContainerScreen<ImbuementMenu> {

    public ImbuementScreen(ImbuementMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // Background
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1f0d3a);
        // Gold border
        g.fill(x, y, x + imageWidth, y + 1, 0xFFc9a657);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFFc9a657);
        g.fill(x, y, x + 1, y + imageHeight, 0xFFc9a657);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFFc9a657);

        // Slot frames (sword, parchment, fragment, output)
        int[][] slotsXY = {{30, 40}, {60, 40}, {90, 40}, {132, 40}};
        int[] colors = {0xFF8a3d3d, 0xFF8a6d3a, 0xFF6b3d8a, 0xFFc9a657};  // sword=red, parch=gold, frag=purple, out=gold
        for (int i = 0; i < 4; i++) {
            int sx = x + slotsXY[i][0], sy = y + slotsXY[i][1];
            drawSlotBg(g, sx, sy, 0xFF2a1845, colors[i]);
        }

        // Arrow from fragment → output
        g.fill(x + 108, y + 46, x + 130, y + 50, 0xFFc9a657);
        g.fill(x + 128, y + 44, x + 130, y + 52, 0xFFc9a657);
        g.fill(x + 126, y + 42, x + 128, y + 54, 0xFFc9a657);

        // Labels
        g.drawString(font, "§eEspada", x + 22, y + 28, 0xFFFFFFFF, false);
        g.drawString(font, "§ePergaminho", x + 47, y + 28, 0xFFFFFFFF, false);
        g.drawString(font, "§eAlma", x + 84, y + 28, 0xFFFFFFFF, false);
        g.drawString(font, "§eResultado", x + 122, y + 28, 0xFFFFFFFF, false);

        // Player inventory frames
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBg(g, x + 8 + col * 18, y + 84 + row * 18, 0xFF1a0d2e, 0xFF4a3d5c);
        for (int col = 0; col < 9; col++)
            drawSlotBg(g, x + 8 + col * 18, y + 142, 0xFF1a0d2e, 0xFFc9a657);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y, int bg, int border) {
        g.fill(x - 1, y - 1, x + 17, y + 17, border);
        g.fill(x, y, x + 16, y + 16, bg);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFc9a657, false);
        g.drawString(this.font, this.playerInventoryTitle,
            this.inventoryLabelX, this.inventoryLabelY, 0xFFaaaaaa, false);

        boolean hasOutput = menu.getSlot(39).hasItem();
        String hint = hasOutput
            ? "§a✓ Espada pronta — clique pra retirar"
            : "§7§oColoque Espada + Pergaminho + Fragmento";
        g.drawString(this.font, Component.literal(hint), 8, 72, 0xFFFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
    }
}
