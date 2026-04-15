package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.item.FieldJournalItem;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FieldJournalScreen extends Screen {
    private static final int PANEL_W = 300, PANEL_H = 220;
    private final InteractionHand hand;
    private final List<String> pages = new ArrayList<>();
    private String title = "";
    private int currentPage = 0;

    private EditBox titleBox;
    private EditBox contentBox;

    public FieldJournalScreen(InteractionHand hand) {
        super(Component.literal("Diário de Campo"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        // Load existing data
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();
        List<String> existing = FieldJournalItem.getPages(tag);
        pages.clear();
        pages.addAll(existing);
        if (pages.isEmpty()) pages.add("");
        title = tag.getString("journal_title");

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        // Title
        titleBox = new EditBox(this.font, left + 5, top + 5, PANEL_W - 10, 16, Component.literal("Título"));
        titleBox.setMaxLength(60);
        titleBox.setValue(title);
        this.addRenderableWidget(titleBox);

        // Content area (multi-line simulated with single EditBox — max 256 chars per page)
        contentBox = new EditBox(this.font, left + 5, top + 28, PANEL_W - 10, 16, Component.literal("Conteúdo"));
        contentBox.setMaxLength(512);
        if (currentPage < pages.size()) {
            contentBox.setValue(pages.get(currentPage));
        }
        this.addRenderableWidget(contentBox);

        // Page nav
        int btnY = top + PANEL_H - 24;
        this.addRenderableWidget(Button.builder(Component.literal("< Pág"), b -> prevPage())
                .bounds(left + 5, btnY, 45, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Pág >"), b -> nextPage())
                .bounds(left + 55, btnY, 45, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("+Pág"), b -> addPage())
                .bounds(left + 105, btnY, 40, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("-Pág"), b -> removePage())
                .bounds(left + 150, btnY, 40, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> save())
                .bounds(left + PANEL_W - 55, btnY, 50, 20).build());
    }

    private void saveCurrent() {
        title = titleBox.getValue();
        while (pages.size() <= currentPage) pages.add("");
        pages.set(currentPage, contentBox.getValue());
    }

    private void prevPage() {
        saveCurrent();
        if (currentPage > 0) {
            currentPage--;
            contentBox.setValue(pages.get(currentPage));
        }
    }

    private void nextPage() {
        saveCurrent();
        if (currentPage < pages.size() - 1) {
            currentPage++;
            contentBox.setValue(pages.get(currentPage));
        }
    }

    private void addPage() {
        saveCurrent();
        if (pages.size() < FieldJournalItem.MAX_PAGES) {
            pages.add(currentPage + 1, "");
            currentPage++;
            contentBox.setValue("");
        }
    }

    private void removePage() {
        if (pages.size() > 1) {
            saveCurrent();
            pages.remove(currentPage);
            if (currentPage >= pages.size()) currentPage = pages.size() - 1;
            contentBox.setValue(pages.get(currentPage));
        }
    }

    private void save() {
        saveCurrent();
        // Remove trailing empty pages
        while (pages.size() > 1 && pages.get(pages.size() - 1).isEmpty()) {
            pages.remove(pages.size() - 1);
        }
        ModNetwork.CHANNEL.sendToServer(new FieldJournalSaveC2SPacket(hand, title, pages));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        this.renderBackground(gfx);
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        // Background
        gfx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xDD1A0A2E);
        gfx.fill(left, top, left + PANEL_W, top + 1, 0xFF6A0DAD);
        gfx.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF6A0DAD);
        gfx.fill(left, top, left + 1, top + PANEL_H, 0xFF6A0DAD);
        gfx.fill(left + PANEL_W - 1, top, left + PANEL_W, top + PANEL_H, 0xFF6A0DAD);

        super.render(gfx, mouseX, mouseY, delta);

        // Page content preview (lines below EditBox)
        String text = contentBox.getValue();
        String[] lines = text.split("\\\\n");
        int lineY = top + 50;
        for (int i = 0; i < lines.length && i < 10; i++) {
            gfx.drawString(this.font, lines[i], left + 8, lineY, 0xFFBBBBBB, false);
            lineY += 10;
        }

        // Page indicator
        gfx.drawString(this.font, "Página " + (currentPage + 1) + "/" + pages.size(),
                left + PANEL_W / 2 - 20, top + PANEL_H - 22, 0xFFAAAAAA, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
