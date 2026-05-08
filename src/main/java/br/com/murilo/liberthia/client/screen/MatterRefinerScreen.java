package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.MatterRefinerMenu;
import br.com.murilo.liberthia.util.Purity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class MatterRefinerScreen extends AbstractContainerScreen<MatterRefinerMenu> {

    public MatterRefinerScreen(MatterRefinerMenu m, Inventory inv, Component title) {
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
        // Frame com toques verde-roxo (refinamento)
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0E0212);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1B0F30);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF6B2A8C);

        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF120920);

        // Slots
        slot(g, x + 44 - 1, y + 30 - 1, 0xFF553090);   // input (inactive)
        slot(g, x + 44 - 1, y + 52 - 1, 0xFFE0C040);   // catalyst (diamond/star)
        slot(g, x + 122 - 1, y + 40 - 1, 0xFFAA40E8); // output (refined)

        // Progress arrow
        int arrowX = x + 70, arrowY = y + 40, arrowW = 48, arrowH = 18;
        g.fill(arrowX, arrowY, arrowX + arrowW, arrowY + arrowH, 0xFF1A0830);
        g.fill(arrowX, arrowY, arrowX + arrowW, arrowY + 1, 0xFF6B2A8C);
        g.fill(arrowX, arrowY + arrowH - 1, arrowX + arrowW, arrowY + arrowH, 0xFF6B2A8C);
        float pf = menu.progressFrac();
        int filled = (int) (arrowW * pf);
        if (filled > 0) {
            for (int i = 0; i < filled; i++) {
                float t = i / (float) Math.max(1, arrowW);
                int r = (int) (110 + 100 * t);
                int gC = (int) (40 + 80 * t);
                int b = (int) (180 + 50 * t);
                int color = 0xFF000000 | (r << 16) | (gC << 8) | b;
                g.fill(arrowX + i, arrowY + 4, arrowX + i + 1, arrowY + arrowH - 4, color);
            }
        }
        for (int i = 0; i < 6; i++) {
            int xx = arrowX + arrowW + i;
            g.fill(xx, arrowY + 6 - i, xx + 1, arrowY + arrowH - 6 + i, 0xFFAA40E8);
        }

        // Energy bar
        int bx = x + 8, by = y + 18, bw = 8, bh = 50;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF6B2A8C);
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

        // Player inv panel
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF120920);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFFAA40E8);
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
        g.drawString(this.font,
                Component.translatable("container.liberthia.matter_refiner")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Indicador de pureza atual no input → output
        ItemStack input = menu.getBlockEntity().getInventory().getStackInSlot(0);
        if (!input.isEmpty()) {
            int curP = Purity.getPurity(input);
            ItemStack cat = menu.getBlockEntity().getInventory().getStackInSlot(1);
            int target = cat.is(Items.NETHER_STAR) ? Purity.MAX
                    : Math.min(Purity.MAX - 1, curP + 1);
            String s = Purity.colorCode(curP) + Purity.stars(curP) + "§r §7→ "
                    + Purity.colorCode(target) + Purity.stars(target);
            g.drawString(this.font, Component.literal(s), 70, 24, 0xFFFFFF, false);
        }
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
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal("Buffer interno").withStyle(ChatFormatting.GRAY)
            ), mx, my);
        }
        if (mx >= x + 70 && mx < x + 124 && my >= y + 40 && my < y + 58) {
            int pct = (int)(menu.progressFrac() * 100);
            g.renderComponentTooltip(this.font, List.of(
                    Component.literal("Refinando " + pct + "%").withStyle(ChatFormatting.GOLD),
                    Component.literal(menu.feSpent() + " / " + menu.feRequired() + " FE consumido")
                            .withStyle(ChatFormatting.GRAY)
            ), mx, my);
        }
        renderTooltip(g, mx, my);
    }
}
