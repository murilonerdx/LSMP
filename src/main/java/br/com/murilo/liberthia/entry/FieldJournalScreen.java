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

/**
 * Simplified journal editor:
 * - One EditBox for title.
 * - Multiple EditBox rows (9 lines) acting as a manual multi-line area.
 * - Each line has its own limit; total content = lines joined with \n.
 * Avoids MultiLineEditBox crashes that happen when the inner string grows too large.
 */
public class FieldJournalScreen extends Screen {
    private static final int PANEL_W = 360, PANEL_H = 260;
    private static final int LINES_PER_PAGE = 9;
    private static final int LINE_MAX = 60;

    private final InteractionHand hand;
    private final List<String> pages = new ArrayList<>();
    private String title = "";
    private int currentPage = 0;

    private EditBox titleBox;
    private final EditBox[] lineBoxes = new EditBox[LINES_PER_PAGE];

    public FieldJournalScreen(InteractionHand hand) {
        super(Component.literal("Diário de Campo"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(hand);
        CompoundTag tag = stack.getOrCreateTag();
        List<String> existing = FieldJournalItem.getPages(tag);
        if (pages.isEmpty()) {
            pages.addAll(existing);
            if (pages.isEmpty()) pages.add("");
            title = tag.getString("journal_title");
        }

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        // Title row
        titleBox = new EditBox(this.font, left + 10, top + 20, PANEL_W - 20, 16, Component.literal("Título"));
        titleBox.setMaxLength(80);
        titleBox.setValue(title);
        titleBox.setHint(Component.literal("Título..."));
        this.addRenderableWidget(titleBox);

        // Multiple single-line boxes -> stable & never crashes.
        String[] lines = splitPage(currentPage);
        for (int i = 0; i < LINES_PER_PAGE; i++) {
            EditBox line = new EditBox(this.font, left + 10, top + 46 + i * 16, PANEL_W - 20, 14, Component.literal(""));
            line.setMaxLength(LINE_MAX);
            line.setBordered(false);
            line.setValue(lines[i]);
            lineBoxes[i] = line;
            this.addRenderableWidget(line);
        }

        // Page nav
        int btnY = top + PANEL_H - 28;
        int bx = left + 10;
        int bw = 50;
        int gap = 4;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> prevPage())
                .bounds(bx, btnY, 22, 20).build());
        bx += 22 + gap;
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> nextPage())
                .bounds(bx, btnY, 22, 20).build());
        bx += 22 + gap;
        this.addRenderableWidget(Button.builder(Component.literal("+ Pág"), b -> addPage())
                .bounds(bx, btnY, bw, 20).build());
        bx += bw + gap;
        this.addRenderableWidget(Button.builder(Component.literal("- Pág"), b -> removePage())
                .bounds(bx, btnY, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> save())
                .bounds(left + PANEL_W - 60, btnY, 50, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("X"), b -> this.onClose())
                .bounds(left + PANEL_W - 85, btnY, 22, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (titleBox != null) titleBox.tick();
        for (EditBox b : lineBoxes) if (b != null) b.tick();
    }

    private String[] splitPage(int index) {
        String[] out = new String[LINES_PER_PAGE];
        for (int i = 0; i < LINES_PER_PAGE; i++) out[i] = "";
        if (index < 0 || index >= pages.size()) return out;
        String raw = pages.get(index);
        if (raw == null || raw.isEmpty()) return out;
        String[] split = raw.split("\n", -1);
        for (int i = 0; i < LINES_PER_PAGE && i < split.length; i++) {
            String ln = split[i];
            if (ln.length() > LINE_MAX) ln = ln.substring(0, LINE_MAX);
            out[i] = ln;
        }
        return out;
    }

    private String joinLines() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LINES_PER_PAGE; i++) {
            if (i > 0) sb.append('\n');
            String v = lineBoxes[i] == null ? "" : lineBoxes[i].getValue();
            sb.append(v);
        }
        return sb.toString();
    }

    private void saveCurrent() {
        if (titleBox != null) title = titleBox.getValue();
        while (pages.size() <= currentPage) pages.add("");
        pages.set(currentPage, joinLines());
    }

    private void reloadLines() {
        String[] lines = splitPage(currentPage);
        for (int i = 0; i < LINES_PER_PAGE; i++) {
            if (lineBoxes[i] != null) lineBoxes[i].setValue(lines[i]);
        }
    }

    private void prevPage() {
        saveCurrent();
        if (currentPage > 0) {
            currentPage--;
            reloadLines();
        }
    }

    private void nextPage() {
        saveCurrent();
        if (currentPage < pages.size() - 1) {
            currentPage++;
            reloadLines();
        } else if (pages.size() < FieldJournalItem.MAX_PAGES) {
            addPage();
        }
    }

    private void addPage() {
        saveCurrent();
        if (pages.size() < FieldJournalItem.MAX_PAGES) {
            pages.add(currentPage + 1, "");
            currentPage++;
            reloadLines();
        }
    }

    private void removePage() {
        if (pages.size() > 1) {
            saveCurrent();
            pages.remove(currentPage);
            if (currentPage >= pages.size()) currentPage = pages.size() - 1;
            reloadLines();
        }
    }

    private void save() {
        saveCurrent();
        while (pages.size() > 1 && pages.get(pages.size() - 1).trim().isEmpty()) {
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

        gfx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE1A0A2E);
        // Purple border
        gfx.fill(left, top, left + PANEL_W, top + 1, 0xFF8A2BE2);
        gfx.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFF8A2BE2);
        gfx.fill(left, top, left + 1, top + PANEL_H, 0xFF8A2BE2);
        gfx.fill(left + PANEL_W - 1, top, left + PANEL_W, top + PANEL_H, 0xFF8A2BE2);

        // Content area background
        gfx.fill(left + 8, top + 44, left + PANEL_W - 8, top + 44 + LINES_PER_PAGE * 16 + 2, 0xFF0A0418);

        gfx.drawCenteredString(this.font, "§d§l✦ Diário de Campo ✦", left + PANEL_W / 2, top + 6, 0xFFFFFFFF);
        gfx.drawString(this.font, "§7Título:", left + 10, top + 10, 0xFFAAAAAA, false);

        super.render(gfx, mouseX, mouseY, delta);

        String pg = "§dPágina §f" + (currentPage + 1) + "§7/§f" + pages.size() + " §8(máx " + FieldJournalItem.MAX_PAGES + ")";
        gfx.drawCenteredString(this.font, pg, left + PANEL_W / 2, top + PANEL_H - 38, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
