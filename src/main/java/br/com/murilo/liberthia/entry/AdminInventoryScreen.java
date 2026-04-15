package br.com.murilo.liberthia.entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class AdminInventoryScreen extends Screen {

    private final Screen parent;
    private final AdminInventorySnapshot snapshot;

    public AdminInventoryScreen(Screen parent, AdminInventorySnapshot snapshot) {
        super(Component.literal("Inventário: " + snapshot.targetName()));
        this.parent = parent;
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int left = (this.width - 250) / 2;
        int top = (this.height - 180) / 2;

        this.addRenderableWidget(
                Button.builder(Component.literal("Voltar"), b -> onClose())
                        .bounds(left + 170, top + 150, 70, 20)
                        .build()
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int left = (this.width - 250) / 2;
        int top = (this.height - 180) / 2;

        guiGraphics.fill(left, top, left + 250, top + 180, 0xCC101010);
        guiGraphics.drawString(this.font, "Inventário de " + snapshot.targetName(), left + 8, top + 8, 0xFFFFFF, false);

        guiGraphics.drawString(this.font, "Main", left + 8, top + 20, 0xA0A0A0, false);

        // Main inventory: 27 slots (9..35)
        int startX = left + 8;
        int startY = top + 32;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                int x = startX + col * 18;
                int y = startY + row * 18;
                drawSlot(guiGraphics, x, y, snapshot.inventory().get(slotIndex));
            }
        }

        guiGraphics.drawString(this.font, "Hotbar", left + 8, top + 88, 0xA0A0A0, false);

        // Hotbar: 0..8
        int hotbarY = top + 100;
        for (int col = 0; col < 9; col++) {
            int x = startX + col * 18;
            drawSlot(guiGraphics, x, hotbarY, snapshot.inventory().get(col));
        }

        guiGraphics.drawString(this.font, "Armor", left + 180, top + 20, 0xA0A0A0, false);

        // Armor: helmet, chest, legs, boots
        String[] labels = {"Helmet", "Chest", "Legs", "Boots"};
        int[] armorMap = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            int x = left + 180;
            int y = top + 32 + i * 18;
            guiGraphics.drawString(this.font, labels[i], x - 2, y - 10, 0x808080, false);
            drawSlot(guiGraphics, x, y, snapshot.armor().size() > armorMap[i] ? snapshot.armor().get(armorMap[i]) : ItemStack.EMPTY);
        }

        guiGraphics.drawString(this.font, "Offhand", left + 180, top + 110, 0xA0A0A0, false);
        drawSlot(guiGraphics, left + 180, top + 122, snapshot.offhand());

        ItemStack hovered = getHoveredStack(mouseX, mouseY, left, top);
        if (!hovered.isEmpty()) {
            guiGraphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y, ItemStack stack) {
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF2B2B2B);
        guiGraphics.fill(x - 1, y - 1, x + 17, y, 0xFF555555);
        guiGraphics.fill(x - 1, y + 16, x + 17, y + 17, 0xFF111111);
        guiGraphics.fill(x - 1, y, x, y + 16, 0xFF555555);
        guiGraphics.fill(x + 16, y, x + 17, y + 16, 0xFF111111);

        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(this.font, stack, x, y);
        }
    }

    private ItemStack getHoveredStack(int mouseX, int mouseY, int left, int top) {
        int startX = left + 8;
        int startY = top + 32;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + row * 9 + col;
                int x = startX + col * 18;
                int y = startY + row * 18;
                if (inside(mouseX, mouseY, x, y, 16, 16)) {
                    return snapshot.inventory().get(slotIndex);
                }
            }
        }

        int hotbarY = top + 100;
        for (int col = 0; col < 9; col++) {
            int x = startX + col * 18;
            if (inside(mouseX, mouseY, x, hotbarY, 16, 16)) {
                return snapshot.inventory().get(col);
            }
        }

        int[] armorMap = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            int x = left + 180;
            int y = top + 32 + i * 18;
            if (inside(mouseX, mouseY, x, y, 16, 16)) {
                return snapshot.armor().size() > armorMap[i] ? snapshot.armor().get(armorMap[i]) : ItemStack.EMPTY;
            }
        }

        if (inside(mouseX, mouseY, left + 180, top + 122, 16, 16)) {
            return snapshot.offhand();
        }

        return ItemStack.EMPTY;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
