package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.SpellBindingPedestalMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * r77 / r164: Spell Binding Pedestal GUI — combine Book + Parchment.
 *
 * <p>r164: Layout reescrito do zero — o anterior tinha texto sobreposto
 * (título cortado, "Inventário" + hint na mesma linha). Agora:
 * <ul>
 *   <li>Título em y=6 (não sobrepõe slots)</li>
 *   <li>3 slots em row y=30 com labels acima (y=20)</li>
 *   <li>Hint dinâmico em y=58 (separado do inventário em y=72)</li>
 *   <li>Inventário em y=82, hotbar em y=140</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class SpellBindingPedestalScreen extends AbstractContainerScreen<SpellBindingPedestalMenu> {

    public SpellBindingPedestalScreen(SpellBindingPedestalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        // Inventário centralizado abaixo (default position OK)
        this.inventoryLabelY = 72;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Background — gradient azul-roxo
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1a0f3a);
        // Border dourada
        g.fill(x, y, x + imageWidth, y + 1, 0xFFc9a657);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFFc9a657);
        g.fill(x, y, x + 1, y + imageHeight, 0xFFc9a657);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFFc9a657);

        // r164: slots realinhados — book(40,40) parch(80,40) out(130,40) era denso demais.
        // Espaçamento agora simétrico: 30 / 80 / 130 com gap visual largo.
        int slotBookX = 30, slotParchX = 80, slotOutX = 130, slotY = 36;
        int[] colors = {0xFF5544AA, 0xFFc9a657, 0xFFD670D6};
        int[][] slotsXY = {{slotBookX, slotY}, {slotParchX, slotY}, {slotOutX, slotY}};
        for (int i = 0; i < 3; i++) {
            int sx = x + slotsXY[i][0], sy = y + slotsXY[i][1];
            drawSlotBg(g, sx, sy, 0xFF2a1845, colors[i]);
        }
        // Seta 1: Book → Parchment
        int arrowY = y + slotY + 6;
        for (int ax = slotBookX + 18; ax < slotParchX - 1; ax += 3) {
            g.fill(x + ax, arrowY, x + ax + 2, arrowY + 3, 0xFFAAAAFF);
        }
        // Seta 2: Parchment → Output
        for (int ax = slotParchX + 18; ax < slotOutX - 1; ax += 3) {
            g.fill(x + ax, arrowY, x + ax + 2, arrowY + 3, 0xFFFFEE66);
        }

        // r164: labels ACIMA dos slots em y=22 (não em y=28 que ficava perto demais)
        drawCenteredString(g, "§eLivro", x + slotBookX + 8, y + 22);
        drawCenteredString(g, "§ePergaminho", x + slotParchX + 8, y + 22);
        drawCenteredString(g, "§6Resultado", x + slotOutX + 8, y + 22);

        // Player inventory frames (default position)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBg(g, x + 8 + col * 18, y + 84 + row * 18, 0xFF1a0d2e, 0xFF4a3d5c);
        for (int col = 0; col < 9; col++)
            drawSlotBg(g, x + 8 + col * 18, y + 142, 0xFF1a0d2e, 0xFFc9a657);
    }

    private void drawCenteredString(GuiGraphics g, String text, int centerX, int y) {
        int plainW = font.width(text.replaceAll("§.", ""));
        g.drawString(font, text, centerX - plainW / 2, y, 0xFFFFFFFF, false);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y, int bg, int border) {
        g.fill(x - 1, y - 1, x + 17, y + 17, border);
        g.fill(x, y, x + 16, y + 16, bg);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Título — limita largura pra não cortar (16px de padding nas bordas)
        String titleText = this.title.getString();
        int maxW = imageWidth - 16;
        if (font.width(titleText) > maxW) {
            titleText = font.plainSubstrByWidth(titleText, maxW - font.width("...")) + "...";
        }
        g.drawString(this.font, titleText, this.titleLabelX, this.titleLabelY, 0xFFc9a657, false);

        // r164: hint em y=58 (ABAIXO do row de slots, ACIMA do label de inventário)
        // Antes ia em y=70 que sobrepunha com inventoryLabelY=72.
        boolean hasOutput = menu.getSlot(38).hasItem();
        String hint = hasOutput
                ? "§a✓ Pronto — pegue o livro"
                : "§7Coloque §eLivro§7 + §6Pergaminho";
        g.drawString(this.font, Component.literal(hint), 8, 58, 0xFFFFFFFF, false);

        // Inventário label — usa posição default (72) sem hint sobreposto
        g.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0xFFaaaaaa, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
    }
}
