package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.block.entity.MatterExtractorBlockEntity;
import br.com.murilo.liberthia.menu.MatterExtractorMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI do Matter Extractor — mostra status, energia, último player extraído.
 */
public class MatterExtractorScreen extends AbstractContainerScreen<MatterExtractorMenu> {

    public MatterExtractorScreen(MatterExtractorMenu m, Inventory inv, Component title) {
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

        // Painel principal
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0A0118);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1A0828);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF6020A0);

        // Painel da máquina
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 78, 0xFF050010);

        // Barra de energia vertical à esquerda
        int barX = x + 10, barY = y + 18, barW = 8, barH = 54;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF101020);
        int e = menu.getEnergy(), eMax = menu.getMaxEnergy();
        if (eMax > 0 && e > 0) {
            int filled = (int) ((long) barH * e / eMax);
            for (int i = 0; i < filled; i++) {
                float pct = i / (float) barH;
                int rC = (int) (0x40 + pct * 0x80);
                int gC = (int) (0x80 + pct * 0x40);
                int bC = 0xFF;
                int color = 0xFF000000 | (rC << 16) | (gC << 8) | bC;
                g.fill(barX, barY + barH - 1 - i, barX + barW, barY + barH - i, color);
            }
        }

        // Player inv panel
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF080018);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF080018);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font,
                Component.translatable("container.liberthia.matter_extractor")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // Info de energia
        String eStr = String.format("§b%d§7 / §b%d FE", menu.getEnergy(), menu.getMaxEnergy());
        g.drawString(this.font, Component.literal(eStr), 25, 64, 0xFFFFFF, false);

        // Status
        int s = menu.getStatus();
        String stxt = switch (s) {
            case MatterExtractorBlockEntity.STATUS_NO_ENERGY -> "§cSem energia";
            case MatterExtractorBlockEntity.STATUS_NO_PLAYER -> "§eAguardando player";
            case MatterExtractorBlockEntity.STATUS_EXTRACTING -> "§aExtraindo!";
            case MatterExtractorBlockEntity.STATUS_TANK_FULL -> "§6Tanque cheio";
            default -> "§7Inativo";
        };
        g.drawString(this.font, Component.literal("Status: " + stxt), 50, 22, 0xFFFFFF, false);

        // Última operação
        int amt = menu.getLastAmount();
        int fid = menu.getLastFluidId();
        if (amt > 0 && fid > 0) {
            String fname = switch (fid) {
                case 1 -> "§dDark Matter";
                case 2 -> "§bClear Matter";
                case 3 -> "§6Yellow Matter";
                default -> "§7?";
            };
            g.drawString(this.font, Component.literal("Última: §f" + amt + "§7 → " + fname),
                    50, 36, 0xFFFFFF, false);
        }

        // Tank interno
        int tnk = menu.getTankAmount();
        if (tnk > 0) {
            g.drawString(this.font, Component.literal("§7Buffer: §f" + tnk + " mB"),
                    50, 50, 0xFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
