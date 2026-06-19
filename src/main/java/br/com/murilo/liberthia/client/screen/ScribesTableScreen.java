package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.ScribesTableMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * v0.1.22 r69: Tela da Scribes Table.
 *
 * <p>Background renderizado proceduralmente (fillRect) com tema místico
 * roxo-azul. Sem necessidade de PNG separado. Slots visíveis com bordas
 * stylized.
 *
 * <h2>Layout</h2>
 * <pre>
 *  ┌───────────────────────────────┐  176×176
 *  │  ╳ Escriba                    │
 *  │ ┌──┬──┬──┬──┬──┬──┬──┬──┐    │  glyph row y=20
 *  │ │  │  │  │  │  │  │  │  │    │
 *  │ └──┴──┴──┴──┴──┴──┴──┴──┘    │
 *  │      [P]     →     [O]        │  parchment y=46
 *  │ ┌───────────────────────────┐ │
 *  │ │   inventário do player    │ │
 *  │ └───────────────────────────┘ │
 *  │ ┌───────────────────────────┐ │
 *  │ │         hotbar            │ │
 *  │ └───────────────────────────┘ │
 *  └───────────────────────────────┘
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class ScribesTableScreen extends AbstractContainerScreen<ScribesTableMenu> {

    /** r164: campo de texto pro nome customizado do feitiço. */
    private EditBox nameField;
    private String lastSentName = "";

    public ScribesTableScreen(ScribesTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        // r164: aumenta altura em 16px (label + EditBox) pra encaixar campo de nome
        this.imageHeight = 182;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // r164: text field embaixo da hotbar
        this.nameField = new EditBox(this.font,
                this.leftPos + 8, this.topPos + 164,
                160, 12,
                Component.literal("Nome do Feitiço"));
        this.nameField.setMaxLength(40);
        this.nameField.setHint(Component.literal("§7Nome do Feitiço..."));
        this.nameField.setResponder(this::onNameChanged);
        this.nameField.setValue(this.menu.getCustomName());
        this.lastSentName = this.menu.getCustomName();
        this.addRenderableWidget(this.nameField);
    }

    private void onNameChanged(String newName) {
        if (!newName.equals(lastSentName)) {
            br.com.murilo.liberthia.network.ModNetwork.sendToServer(
                    new br.com.murilo.liberthia.magic.workbench.SetSpellNameC2SPacket(
                            this.menu.getBlockPos(), newName));
            lastSentName = newName;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // ── Background panel (gradient roxo escuro) ──
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1a0d2e);
        // borda dourada
        g.fill(x, y, x + imageWidth, y + 1, 0xFFc9a657);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFFc9a657);
        g.fill(x, y, x + 1, y + imageHeight, 0xFFc9a657);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFFc9a657);

        // ── Glyph row slots (8 slots de 18x18) ──
        for (int i = 0; i < 8; i++) {
            int sx = x + 8 + i * 18;
            int sy = y + 20;
            drawSlotBg(g, sx, sy, 0xFF3d2a5c, 0xFF6b4d99);
        }

        // ── Parchment in slot ──
        int pX = x + 44, pY = y + 46;
        drawSlotBg(g, pX, pY, 0xFF3d2a5c, 0xFFc9a657);

        // ── Output slot ──
        int oX = x + 116, oY = y + 46;
        drawSlotBg(g, oX, oY, 0xFF2a3d5c, 0xFFc9a657);

        // ── Arrow entre parchment in e output ──
        int ax = x + 66, ay = y + 50;
        g.fill(ax, ay, ax + 48, ay + 6, 0xFF8a6d3a);
        // Setinha (3 pixels triangulares)
        g.fill(ax + 46, ay - 2, ax + 50, ay + 1, 0xFF8a6d3a);
        g.fill(ax + 46, ay + 5, ax + 50, ay + 8, 0xFF8a6d3a);

        // ── Texto label sobre o arrow ──
        g.drawString(font, "→ feitiço",
            x + 62, y + 38, 0xFFc9a657, false);

        // ── Player inventory background (3 linhas de 9 slots em y=84) ──
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 8 + col * 18;
                int sy = y + 84 + row * 18;
                drawSlotBg(g, sx, sy, 0xFF1a0d2e, 0xFF4a3d5c);
            }
        }
        // Hotbar (y=142)
        for (int col = 0; col < 9; col++) {
            int sx = x + 8 + col * 18;
            int sy = y + 142;
            drawSlotBg(g, sx, sy, 0xFF1a0d2e, 0xFFc9a657);
        }
        // r164: label do nome do feitiço (acima do EditBox em y=164)
        g.drawString(font, "§e✎ Nome do Feitiço:", x + 8, y + 155, 0xFFc9a657, false);
    }

    /** Draws a 18x18 slot background with border. */
    private void drawSlotBg(GuiGraphics g, int x, int y, int bgColor, int borderColor) {
        g.fill(x - 1, y - 1, x + 17, y + 17, borderColor);
        g.fill(x, y, x + 16, y + 16, bgColor);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Título centralizado em cima
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFc9a657, false);
        // "Inventário" no topo do player inv
        g.drawString(this.font, this.playerInventoryTitle,
            this.inventoryLabelX, this.inventoryLabelY, 0xFFaaaaaa, false);

        // Hint dinâmico embaixo do label
        boolean hasOutput = menu.getSlot(45).hasItem();
        String hint = hasOutput
            ? "§a✓ Pergaminho pronto — clique pra pegar"
            : "§7§oColoque pergaminho vazio + glifos";
        g.drawString(this.font, Component.literal(hint),
            8, 70, 0xFFFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
