package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.magic.thread.ThreadIngredients;
import br.com.murilo.liberthia.magic.thread.ThreadLoomBlockEntity;
import br.com.murilo.liberthia.magic.thread.ThreadLoomMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * r172: GUI do Tear de Threads — 5 slots de input, slot de output, e um painel
 * que mostra ao vivo as PROPRIEDADES da thread montada (efeitos reais).
 */
@OnlyIn(Dist.CLIENT)
public class ThreadLoomScreen extends AbstractContainerScreen<ThreadLoomMenu> {

    public ThreadLoomScreen(ThreadLoomMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 92;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF161020);
        border(g, x, y, imageWidth, imageHeight, 0xFF7A4ED0, 2);
        g.fill(x, y, x + imageWidth, y + 16, 0xFF241640);

        // 5 input slots (y=34, x=26..98) + output (x=134)
        for (int i = 0; i < ThreadLoomBlockEntity.INPUT_COUNT; i++)
            slot(g, x + 26 + i * 18, y + 34);
        slot(g, x + 134, y + 34);
        // seta
        g.drawString(font, "§d➜", x + 116, y + 38, 0xFFCC99FF, false);

        // Painel de preview (propriedades da thread)
        int px = x + 8, py = y + 54, pw = imageWidth - 16, ph = 34;
        g.fill(px, py, px + pw, py + ph, 0xFF0E0A18);
        border(g, px, py, pw, ph, 0xFF4A3A70, 1);

        List<String> props = previewProps();
        if (props.isEmpty()) {
            g.drawString(font, "§8Coloque itens (açúcar, diamante, pena...)", px + 4, py + 4, 0xFF8877AA, false);
            g.drawString(font, "§8pra montar uma thread customizada.", px + 4, py + 14, 0xFF8877AA, false);
        } else {
            int ly = py + 3;
            for (String s : props) {
                if (ly > py + ph - 9) break;
                g.drawString(font, "§a✦ " + s, px + 4, ly, 0xFFAAFFAA, false);
                ly += 9;
            }
        }
    }

    /** Calcula os efeitos resultantes a partir dos 5 slots de input (client-side). */
    private List<String> previewProps() {
        List<ItemStack> inputs = new ArrayList<>();
        for (int i = 0; i < ThreadLoomBlockEntity.INPUT_COUNT; i++) inputs.add(menu.slots.get(i).getItem());
        var fx = ThreadIngredients.build(inputs);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < fx.size(); i++) {
            String label = fx.getCompound(i).getString("label");
            if (!label.isEmpty()) out.add(label);
        }
        return out;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, "§5§lTear de Threads", titleLabelX, titleLabelY, 0xFFE0C8FF, false);
        g.drawString(font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF9988BB, false);
    }

    private void slot(GuiGraphics g, int sx, int sy) {
        g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF7A4ED0);
        g.fill(sx, sy, sx + 16, sy + 16, 0xFF0E0A18);
    }

    private void border(GuiGraphics g, int x, int y, int w, int h, int c, int t) {
        g.fill(x, y, x + w, y + t, c);
        g.fill(x, y + h - t, x + w, y + h, c);
        g.fill(x, y, x + t, y + h, c);
        g.fill(x + w - t, y, x + w, y + h, c);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
