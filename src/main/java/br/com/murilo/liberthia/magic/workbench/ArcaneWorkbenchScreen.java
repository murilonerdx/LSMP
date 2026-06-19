package br.com.murilo.liberthia.magic.workbench;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * r159: GUI do Arcane Workbench com slots tipados.
 *
 * <p>Cada slot tem label visual indicando o tipo aceito (tablet/weave/thread/...).
 * Background procedural sem texture sheet.
 */
public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {

    private static final int BG          = 0xFF1a0824;
    private static final int FRAME       = 0xFF6633aa;
    private static final int FRAME_LIGHT = 0xFFaa66ff;
    private static final int SLOT_BG     = 0xFF2a1244;
    private static final int OUTPUT_FRAME = 0xFFffd700;
    private static final int LABEL_COLOR = 0xFFcc99ff;

    private static final int[] SLOT_X = {16, 40, 64, 88,    16, 40, 64,    16, 40, 64,    132};
    private static final int[] SLOT_Y = {20, 20, 20, 20,    48, 48, 48,    76, 76, 76,     76};
    /** Cor por tipo de slot. */
    private static final int[] SLOT_FRAME = {
            0xFFcc8855,   // TABLET (bronze)
            0xFFaaccee,   // WEAVE (azul claro)
            0xFFddccaa,   // THREAD (palha)
            0xFFffaa44,   // SCROLL (laranja)
            0xFF66ccff,   // ORB (ciano)
            0xFF99ff66,   // FOCUS (verde lima)
            0xFFff66cc,   // SCHOOL (rosa)
            0xFFaa66ff,   // MODIFIER_GLYPH (roxo)
            0xFF9966ff,   // GLYPH (lilás)
            0xFFffeeaa,   // PARCHMENT (creme)
            0xFFffd700,   // OUTPUT (dourado)
    };
    /** Label curto pra cada slot. */
    private static final String[] SLOT_LABEL = {
            "Tab", "Wv", "Thr", "Scr",
            "Orb", "Foc", "Sch",
            "Mod", "Gly", "Par",
            "OUT"
    };

    /** r164: campo de texto pro nome customizado do feitiço. */
    private EditBox nameField;
    /** r164: nome enviado pro server pela última vez (evita spam de packets). */
    private String lastSentName = "";

    public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        // r166: layout mais alto — slots espaçados 28px + rótulos legíveis + campo de nome embaixo
        this.imageHeight = 210;
        this.inventoryLabelY = 104;
    }

    @Override
    protected void init() {
        super.init();
        // r164: text field pro nome customizado, abaixo da hotbar
        this.nameField = new EditBox(this.font,
                this.leftPos + 16, this.topPos + 190,
                144, 12,
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
                    new SetSpellNameC2SPacket(this.menu.getBlockPos(), newName));
            lastSentName = newName;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Background
        g.fill(x, y, x + imageWidth, y + imageHeight, BG);
        // Outer frame
        g.fill(x, y, x + imageWidth, y + 2, FRAME);
        g.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, FRAME);
        g.fill(x, y, x + 2, y + imageHeight, FRAME);
        g.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, FRAME);
        // Title bar
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 14, FRAME);

        // Slot frames colored by type
        for (int i = 0; i < SLOT_X.length; i++) {
            int sx = x + SLOT_X[i];
            int sy = y + SLOT_Y[i];
            int frame = SLOT_FRAME[i];
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, frame);
            g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
            // 2-3 letter label below slot
            g.drawString(this.font, SLOT_LABEL[i], sx + 1, sy + 17, LABEL_COLOR, false);
        }

        // Arrows: row3 → output
        g.drawString(this.font, "→", x + 100, y + 80, 0xFFFFFFFF, false);
        g.drawString(this.font, "→", x + 116, y + 80, 0xFFFFFFFF, false);

        // r166: label do campo "Nome do Feitiço"
        g.drawString(this.font, "§eNome do Feitico:", x + 16, y + 180, 0xFFFFEEAA, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, "§l§dArcane Workbench", 8, 5, 0xFFFFFFFF, false);
        g.drawString(this.font, "§7Inventory", 8, 82, 0xFFAAAAFF, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        // Tooltip pra slot vazio mostrar o que vai ali
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        for (int i = 0; i < SLOT_X.length; i++) {
            if (relX >= SLOT_X[i] && relX < SLOT_X[i] + 16
                    && relY >= SLOT_Y[i] && relY < SLOT_Y[i] + 16
                    && this.menu.getSlot(i).getItem().isEmpty()) {
                ArcaneSlotType type = ArcaneSlotType.ORDER[i];
                g.renderTooltip(this.font,
                        Component.literal("§d" + type.displayName + " §8(slot " + i + ")"),
                        mouseX, mouseY);
                break;
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
