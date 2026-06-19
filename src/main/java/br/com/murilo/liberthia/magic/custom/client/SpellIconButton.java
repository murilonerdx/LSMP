package br.com.murilo.liberthia.magic.custom.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * v0.1.22 r43: Botão custom que renderiza um SPRITE PNG como ícone
 * (não apenas texto). Usado pela GUI de spell crafting pra mostrar VFX
 * de verdade em vez de "FIRE" / "ICE" / "VOID" texto.
 *
 * <h2>Visual</h2>
 * <ul>
 *   <li>Border: 1px — gold se selecionado, branco se hover, cinza normal</li>
 *   <li>BG: roxo escuro semi-transparente</li>
 *   <li>Icon: sprite 24×24 centralizado</li>
 *   <li>Label: 1-2 chars abaixo (opcional)</li>
 * </ul>
 */
public class SpellIconButton extends AbstractButton {

    private final ResourceLocation iconTexture;
    private final int iconColor;
    private final Runnable onClick;
    private boolean selected;

    public SpellIconButton(int x, int y, int size, ResourceLocation icon, int color,
                            Component tooltip, Runnable onClick) {
        super(x, y, size, size, tooltip);
        this.iconTexture = icon;
        this.iconColor = color;
        this.onClick = onClick;
        this.selected = false;
        this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
    }

    public void setSelected(boolean sel) { this.selected = sel; }
    public boolean isSelected() { return selected; }

    @Override
    public void onPress() {
        if (onClick != null) onClick.run();
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        boolean hover = isHovered();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        // Background: tonalidade do color do sprite com 0x55 alpha
        int bgAlpha = selected ? 0xCC : (hover ? 0x99 : 0x55);
        int bg = (bgAlpha << 24) | (iconColor & 0xFFFFFF);
        g.fill(x, y, x + w, y + h, bg);

        // Border
        int borderColor;
        if (selected) borderColor = 0xFFFFCC00; // gold
        else if (hover) borderColor = 0xFFFFFFFF;
        else borderColor = 0xFF555555;
        // Draw 1px border
        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);
        // Double border when selected (mais visível)
        if (selected) {
            int innerColor = 0xFFFF8800;
            g.fill(x + 1, y + 1, x + w - 1, y + 2, innerColor);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, innerColor);
            g.fill(x + 1, y + 1, x + 2, y + h - 1, innerColor);
            g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, innerColor);
        }

        // Render sprite icon
        if (iconTexture != null) {
            try {
                RenderSystem.enableBlend();
                int iconSize = Math.max(8, w - 8);
                int ix = x + (w - iconSize) / 2;
                int iy = y + (h - iconSize) / 2;
                g.blit(iconTexture, ix, iy, 0, 0, iconSize, iconSize, iconSize, iconSize);
                RenderSystem.disableBlend();
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        this.defaultButtonNarrationText(out);
    }
}
