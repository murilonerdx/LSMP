package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.spell.composition.SpellComposition;
import br.com.murilo.liberthia.magic.spell.weaver.SpellWeaverMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * v0.1.151 r119: <b>SpellWeaverScreen</b> — UI do bloco Spell Weaver.
 *
 * <p>Renderiza:
 * <ul>
 *   <li>Background com gradiente roxo/azul + slot tiles</li>
 *   <li>Slot base à esquerda, slot output à direita, 7 modifier slots no meio</li>
 *   <li>Painel à direita mostrando STATS finais do output (mana, dano, CD)</li>
 *   <li>Painel à esquerda com hint sobre o spell base</li>
 *   <li>Animated arc connecting base → modifiers → output quando craft válido</li>
 * </ul>
 */
public class SpellWeaverScreen extends AbstractContainerScreen<SpellWeaverMenu> {

    private static final ResourceLocation BG = new ResourceLocation(
            LiberthiaMod.MODID, "textures/gui/spell_weaver.png");

    public SpellWeaverScreen(SpellWeaverMenu menu, Inventory inv, Component title) {
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

        // Linha animada de partículas conectando base ao output quando há output
        ItemStack output = menu.getSlot(8).getItem();
        if (!output.isEmpty()) {
            long time = System.currentTimeMillis();
            // Pulsing line from base (x=28, y=28) to output (x=160, y=28)
            int alpha = (int)(150 + Math.sin(time / 200.0) * 100);
            int color = (alpha << 24) | 0xAA66FF;
            int startX = leftPos + 36;
            int endX = leftPos + 160;
            int y = topPos + 28;
            g.fill(startX, y, endX, y + 1, color);
            // Pulsing nodes
            double phase = (time / 100.0) % (Math.PI * 2);
            for (int i = 0; i < 7; i++) {
                int nodeX = leftPos + 34 + i * 18;
                int sz = (int)(2 + Math.sin(phase + i * 0.5) * 1.5);
                g.fill(nodeX - sz, y - sz, nodeX + sz, y + sz, 0xFFFFEE00);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderStatsPanel(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    /** Renderiza painel lateral com stats finais do output. */
    private void renderStatsPanel(GuiGraphics g, int mouseX, int mouseY) {
        ItemStack output = menu.getSlot(8).getItem();

        // Painel à ESQUERDA do GUI principal (a direita é ocupada pela lista do JEI)
        int sideW = 120;
        int sideH = 140;
        int sideX = leftPos - sideW - 6;
        int sideY = topPos + 5;

        // Background
        g.fill(sideX, sideY, sideX + sideW, sideY + sideH, 0xDD0A0218);
        g.fill(sideX, sideY, sideX + sideW, sideY + 1, 0xFFFFEE00);
        g.fill(sideX, sideY + sideH - 1, sideX + sideW, sideY + sideH, 0xFFFFEE00);
        g.fill(sideX, sideY, sideX + 1, sideY + sideH, 0xFFFFEE00);
        g.fill(sideX + sideW - 1, sideY, sideX + sideW, sideY + sideH, 0xFFFFEE00);

        // Title
        g.drawString(font, Component.literal("§6§lFeitiço Composto"),
                sideX + 6, sideY + 4, 0xFFFFFFFF, false);

        // Stats
        int y = sideY + 18;
        if (output.isEmpty()) {
            g.drawString(font, "§7Coloque o feitiço",
                    sideX + 6, y, 0xFFFFFFFF, false);
            g.drawString(font, "§7base no slot esq.",
                    sideX + 6, y + 11, 0xFFFFFFFF, false);
            return;
        }

        SpellComposition comp = SpellComposition.fromStack(output);
        if (comp == null) return;

        for (Component line : comp.tooltip()) {
            g.drawString(font, line, sideX + 4, y, 0xFFFFFFFF, false);
            y += 11;
            if (y > sideY + sideH - 15) break;
        }
    }
}
