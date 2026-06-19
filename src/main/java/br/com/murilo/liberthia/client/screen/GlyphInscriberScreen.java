package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.glyph.SpiritGlyphRecipes;
import br.com.murilo.liberthia.magic.glyph.inscriber.GlyphInscriberMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * v0.1.150 r118: <b>GlyphInscriberScreen</b> — UI bonita pra crafting de glyphs.
 *
 * <h2>Layout 176x166 padrão</h2>
 * <ul>
 *   <li>Background: gradient roxo escuro + parchment central</li>
 *   <li>4 slots input em cruz (cima/baixo/esq/dir)</li>
 *   <li>1 output slot à direita do centro</li>
 *   <li>4 arrow icons apontando do input pro centro (visual feedback)</li>
 *   <li>Recipe Hints: hover sobre slot vazio mostra reagents válidos</li>
 *   <li>Animated runic ring no centro quando recipe completa</li>
 * </ul>
 *
 * <p>Texture: 256x256 PNG (background + ring frames). Standard MC slot
 * tiles renderizadas na posição padrão.
 */
public class GlyphInscriberScreen extends AbstractContainerScreen<GlyphInscriberMenu> {

    private static final ResourceLocation BG = new ResourceLocation(
            LiberthiaMod.MODID, "textures/gui/glyph_inscriber.png");

    public GlyphInscriberScreen(GlyphInscriberMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // ─── Animação radial no centro quando output está pronto ───
        ItemStack output = menu.getSlot(4).getItem();
        if (!output.isEmpty()) {
            long time = System.currentTimeMillis();
            int alpha = (int)(80 + Math.sin(time / 200.0) * 60);
            int color = (alpha << 24) | 0xAA66FF;
            // Glow no slot de output (15px ring around slot at 134,35)
            int cx = leftPos + 142;
            int cy = topPos + 43;
            for (int r = 11; r <= 14; r++) {
                drawRingPixels(g, cx, cy, r, color);
            }
        }

        // ─── Arrows visuais do input pro center (cosméticos) ───
        int arrowColor = 0x88AA66FF;
        drawDirArrows(g);
    }

    /** Desenha pequenas setas direcionais (3 pixels cada) dos inputs pro output. */
    private void drawDirArrows(GuiGraphics g) {
        int color = 0x88AA66FF;
        // Cima → centro
        g.fill(leftPos + 88, topPos + 28, leftPos + 89, topPos + 29, color);
        g.fill(leftPos + 87, topPos + 29, leftPos + 90, topPos + 30, color);
        // Baixo → centro
        g.fill(leftPos + 88, topPos + 47, leftPos + 89, topPos + 48, color);
        g.fill(leftPos + 87, topPos + 46, leftPos + 90, topPos + 47, color);
        // Esq → centro
        g.fill(leftPos + 80, topPos + 39, leftPos + 81, topPos + 40, color);
        g.fill(leftPos + 79, topPos + 38, leftPos + 80, topPos + 41, color);
        // Dir → centro (apontando pra output)
        g.fill(leftPos + 115, topPos + 39, leftPos + 116, topPos + 40, color);
        g.fill(leftPos + 116, topPos + 38, leftPos + 117, topPos + 41, color);
    }

    /** Desenha um anel de pixels (ring outline). */
    private void drawRingPixels(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(radius * radius - dy * dy);
            g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            g.fill(cx - dx, cy + dy, cx - dx + 1, cy + dy + 1, color);
            g.fill(cx + dy, cy + dx, cx + dy + 1, cy + dx + 1, color);
            g.fill(cx + dy, cy - dx, cx + dy + 1, cy - dx + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);

        // ─── Hint panel à esquerda: mostra qual recipe vai resultar dos inputs atuais ───
        renderRecipePreview(g, mouseX, mouseY);
    }

    /** Painel lateral com lista de recipes possíveis baseado nos inputs. */
    private void renderRecipePreview(GuiGraphics g, int mouseX, int mouseY) {
        // Sidebar à ESQUERDA do GUI principal (a direita é ocupada pela lista do JEI)
        int sideW = 100;
        int sideH = 100;
        int sideX = leftPos - sideW - 6;
        int sideY = topPos + 5;

        // Background do panel
        g.fill(sideX, sideY, sideX + sideW, sideY + sideH, 0xDD150528);
        g.fill(sideX, sideY, sideX + sideW, sideY + 1, 0xFFAA66FF);  // borda
        g.fill(sideX, sideY + sideH - 1, sideX + sideW, sideY + sideH, 0xFFAA66FF);
        g.fill(sideX, sideY, sideX + 1, sideY + sideH, 0xFFAA66FF);
        g.fill(sideX + sideW - 1, sideY, sideX + sideW, sideY + sideH, 0xFFAA66FF);

        // Header
        g.drawString(font, Component.literal("§d§lReceitas").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                sideX + 6, sideY + 4, 0xFFFFFFFF, false);

        // Lista até 6 recipes
        int y = sideY + 18;
        int shown = 0;
        for (SpiritGlyphRecipes.Recipe r : SpiritGlyphRecipes.all()) {
            if (shown >= 6) break;
            String label = "§7• §d" + truncate(r.displayName, 14);
            g.drawString(font, label, sideX + 4, y, 0xFFFFFFFF, false);
            y += 11;
            shown++;
        }
        if (SpiritGlyphRecipes.all().size() > 6) {
            g.drawString(font, "§8§o... +" + (SpiritGlyphRecipes.all().size() - 6) + " mais",
                    sideX + 4, y, 0xFFFFFFFF, false);
        }
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}
