package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.spell.mutator.SpellMutatorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * v0.1.162 r138: <b>SpellMutatorScreen</b> — UI do bloco Spell Mutator.
 *
 * <p>Layout:
 * <pre>
 *  [Scroll A]   →    [Output Hibrido]
 *  [Scroll B]
 *
 *  [Stats do hibrido]
 *  [Inventário do player]
 * </pre>
 */
public class SpellMutatorScreen extends AbstractContainerScreen<SpellMutatorMenu> {

    public SpellMutatorScreen(SpellMutatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Background procedural — gradient roxo + slots desenhados via fill
        // (sem PNG, mais leve, mantém consistência visual com SpellWeaver)
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xCC0A0218);

        // Borda dourada
        int borderColor = 0xFFFFEE00;
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, borderColor);
        g.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, borderColor);
        g.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, borderColor);
        g.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, borderColor);

        // Slot A frame
        drawSlotFrame(g, leftPos + 29, topPos + 21);
        drawSlotFrame(g, leftPos + 29, topPos + 49);
        // Output frame (maior, dourado)
        drawOutputFrame(g, leftPos + 127, topPos + 35);

        // Arrow visual entre inputs e output
        ItemStack a = menu.getSlot(0).getItem();
        ItemStack b = menu.getSlot(1).getItem();
        ItemStack out = menu.getSlot(2).getItem();
        boolean ready = !a.isEmpty() && !b.isEmpty() && !out.isEmpty();

        long time = System.currentTimeMillis();
        int arrowColor = ready
                ? (((int)(150 + Math.sin(time / 200.0) * 100) << 24) | 0x66FF66)
                : 0x44888888;
        // Arrow body
        int arrowY = topPos + 41;
        g.fill(leftPos + 60, arrowY, leftPos + 120, arrowY + 4, arrowColor);
        // Arrowhead
        g.fill(leftPos + 118, arrowY - 4, leftPos + 124, arrowY + 8, arrowColor);

        // Inventory area background
        g.fill(leftPos + 7, topPos + 83, leftPos + 169, topPos + 159, 0x88000000);
    }

    private void drawSlotFrame(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF1A0830);
        g.fill(x, y, x + 18, y + 1, 0x66AAAAAA);
        g.fill(x, y, x + 1, y + 18, 0x66AAAAAA);
        g.fill(x + 17, y, x + 18, y + 18, 0x66555555);
        g.fill(x, y + 17, x + 18, y + 18, 0x66555555);
    }

    private void drawOutputFrame(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF2A1230);
        // Dourado
        g.fill(x - 1, y - 1, x + 19, y, 0xFFFFEE00);
        g.fill(x - 1, y + 18, x + 19, y + 19, 0xFFFFEE00);
        g.fill(x - 1, y - 1, x, y + 19, 0xFFFFEE00);
        g.fill(x + 18, y - 1, x + 19, y + 19, 0xFFFFEE00);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFEE00, false);
        g.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFFFFFFF, false);

        // Hint
        ItemStack a = menu.getSlot(0).getItem();
        ItemStack b = menu.getSlot(1).getItem();
        if (a.isEmpty() || b.isEmpty()) {
            g.drawString(font, "§7Coloque 2 scrolls", 6, 70, 0xFFFFFFFF, false);
        } else {
            g.drawString(font, "§a✓ Híbrido pronto", 6, 70, 0xFFFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
