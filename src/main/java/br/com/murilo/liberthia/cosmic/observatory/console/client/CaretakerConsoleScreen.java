package br.com.murilo.liberthia.cosmic.observatory.console.client;

import br.com.murilo.liberthia.cosmic.observatory.console.MessageTemplates;
import br.com.murilo.liberthia.cosmic.observatory.console.StartCaretakerSessionC2SPacket;
import br.com.murilo.liberthia.cosmic.observatory.console.StopCaretakerSessionC2SPacket;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * v0.1.22 r56: <b>Caretaker Console v3</b> — UI completamente reimaginada.
 *
 * <h2>Características novas (r56)</h2>
 * <ul>
 *   <li><b>Draggable window</b> — clique e arraste a barra de título</li>
 *   <li><b>Responsivo</b> — adapta a ultrawide, 720p, 4k</li>
 *   <li><b>Tema cósmico</b> — gradiente background, glifos animados, glow</li>
 *   <li><b>Tabs animadas</b> — transitions suaves entre categorias</li>
 *   <li><b>Glifos vivos</b> — particles ASCII rotacionando no header</li>
 *   <li><b>Scroll smooth</b> — animado, com inércia</li>
 *   <li><b>Sem texto cortado</b> — list usa altura adaptativa</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class CaretakerConsoleScreen extends Screen {

    // Window state
    private int winX, winY, winW, winH;
    private boolean dragging = false;
    private int dragOffX, dragOffY;

    // Input/state
    private EditBox targetBox;
    private EditBox customMsgBox;
    private int intervalSec = 30;
    private int durationSec = 300;
    private String filterCategory = "ALL";

    private final Set<String> selectedTemplates = new HashSet<>();
    private final List<String> customMessages = new ArrayList<>();

    private int scrollOffset = 0;
    private float scrollSmooth = 0F;
    private static final int ROW_HEIGHT = 14;

    // Animation timers
    private long openedAt = 0;
    private float tabAnim = 0F;
    private int prevTabIndex = 0;

    public CaretakerConsoleScreen() {
        super(Component.literal("Caretaker Console"));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        openedAt = System.currentTimeMillis();

        // Window dimensions — adaptive
        winW = Math.min(560, Math.max(400, width - 40));
        winH = Math.min(380, Math.max(260, height - 40));
        // Center initial position
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        buildLayout();
    }

    /**
     * Reconstrói widgets baseado em winX/winY (chamado quando arrasta).
     */
    private void buildLayout() {
        this.clearWidgets();

        int padLeft = winX + 14;
        int padRight = winX + winW - 14;
        int contentW = winW - 28;

        int yTitle = winY + 8;       // barra de drag/título
        int yRow1 = yTitle + 18;     // target + buttons
        int yRow2 = yRow1 + 18;      // interval + duration
        int yTabs = yRow2 + 18;      // category tabs
        int yListTop = yTabs + 22;   // templates list
        int yListBottom = winY + winH - 52;  // 52px for custom+count+esc
        int yCustom = winY + winH - 36;
        int yCount = winY + winH - 18;

        // Target input
        targetBox = new EditBox(font, padLeft + 60, yRow1, 130, 14,
                Component.literal("target"));
        targetBox.setMaxLength(16);
        targetBox.setSuggestion("Nome do player...");
        this.addRenderableWidget(targetBox);

        // ACTIVATE
        this.addRenderableWidget(Button.builder(
                Component.literal("§a§l✦ ATIVAR"), btn -> activate())
                .bounds(padLeft + 196, yRow1, 80, 14).build());

        // STOP
        this.addRenderableWidget(Button.builder(
                Component.literal("§c§l■ PARAR"), btn -> stop())
                .bounds(padLeft + 280, yRow1, 70, 14).build());

        // Interval -/+
        this.addRenderableWidget(Button.builder(
                Component.literal("§e-"), btn -> { intervalSec = Math.max(3, intervalSec - 5); })
                .bounds(padLeft + 60, yRow2, 14, 14).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("§e+"), btn -> { intervalSec = Math.min(300, intervalSec + 5); })
                .bounds(padLeft + 116, yRow2, 14, 14).build());

        // Duration -/+
        this.addRenderableWidget(Button.builder(
                Component.literal("§e-"), btn -> {
                    if (durationSec == -1) durationSec = 1800;
                    else durationSec = Math.max(60, durationSec - 60);
                })
                .bounds(padLeft + 196, yRow2, 14, 14).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("§e+"), btn -> {
                    if (durationSec >= 3600) durationSec = -1;
                    else durationSec = Math.min(3600, durationSec + 60);
                })
                .bounds(padLeft + 270, yRow2, 14, 14).build());

        // Category TABS — animated
        String[] cats = {"ALL", "JOIN_LEAVE", "DEATH", "SYSTEM", "COMMAND_FAIL", "WHISPER", "OBSERVATION"};
        String[] catLabels = {"All", "Join", "Death", "Sys", "Cmd", "Whisp", "Obs"};
        int totalTabW = contentW;
        int tabW = totalTabW / cats.length;
        for (int i = 0; i < cats.length; i++) {
            final String catId = cats[i];
            final int idx = i;
            this.addRenderableWidget(Button.builder(
                    Component.literal(catLabels[i]),
                    btn -> {
                        prevTabIndex = currentTabIndex();
                        filterCategory = catId;
                        scrollOffset = 0;
                        scrollSmooth = 0F;
                        tabAnim = 1F;
                    })
                    .bounds(padLeft + i * tabW, yTabs, tabW - 2, 14).build());
        }

        // Custom msg input
        customMsgBox = new EditBox(font, padLeft + 70, yCustom, contentW - 110, 14,
                Component.literal("custom"));
        customMsgBox.setMaxLength(200);
        customMsgBox.setSuggestion("Custom message...");
        this.addRenderableWidget(customMsgBox);

        // Add custom
        this.addRenderableWidget(Button.builder(
                Component.literal("§a+"), btn -> {
                    String c = customMsgBox.getValue().trim();
                    if (!c.isEmpty()) {
                        customMessages.add(c);
                        customMsgBox.setValue("");
                    }
                })
                .bounds(padRight - 36, yCustom, 36, 14).build());

        // Scroll buttons (lateral right)
        this.addRenderableWidget(Button.builder(
                Component.literal("▲"), btn -> {
                    scrollOffset = Math.max(0, scrollOffset - 1);
                })
                .bounds(padRight - 14, yListTop, 14, 14).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("▼"), btn -> {
                    List<MessageTemplates.Template> visible = getFilteredTemplates();
                    int rows = Math.max(1, (yListBottom - yListTop) / ROW_HEIGHT);
                    int max = Math.max(0, visible.size() - rows);
                    scrollOffset = Math.min(max, scrollOffset + 1);
                })
                .bounds(padRight - 14, yListBottom - 14, 14, 14).build());

        // Close button (X) no canto superior direito
        this.addRenderableWidget(Button.builder(
                Component.literal("§c§l✕"), btn -> this.onClose())
                .bounds(winX + winW - 22, winY + 4, 18, 14).build());
    }

    private int currentTabIndex() {
        String[] cats = {"ALL", "JOIN_LEAVE", "DEATH", "SYSTEM", "COMMAND_FAIL", "WHISPER", "OBSERVATION"};
        for (int i = 0; i < cats.length; i++) {
            if (cats[i].equals(filterCategory)) return i;
        }
        return 0;
    }

    private List<MessageTemplates.Template> getFilteredTemplates() {
        if ("ALL".equals(filterCategory)) return MessageTemplates.ALL;
        return MessageTemplates.getByCategory(filterCategory);
    }

    private void activate() {
        String target = targetBox.getValue().trim();
        if (target.isEmpty()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§c⚠ Digite o nome do target"), true);
            }
            return;
        }

        List<String> resolved = new ArrayList<>();
        String[] randomNames = {"OldSteve", "_Shadow_", "user_404", "[deleted]",
                "guest_a3f", "Marlon", "Pedro_22", "EmptyName"};
        for (String tid : selectedTemplates) {
            MessageTemplates.Template t = MessageTemplates.byId(tid);
            if (t == null) continue;
            String randomName = randomNames[(int)(Math.random() * randomNames.length)];
            resolved.add(MessageTemplates.resolve(t, target, randomName));
        }
        resolved.addAll(customMessages);

        if (resolved.isEmpty()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§c⚠ Selecione pelo menos uma mensagem"), true);
            }
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new StartCaretakerSessionC2SPacket(
                target, intervalSec, durationSec, resolved));
        this.onClose();
    }

    private void stop() {
        String target = targetBox.getValue().trim();
        if (target.isEmpty()) return;
        ModNetwork.CHANNEL.sendToServer(new StopCaretakerSessionC2SPacket(target));
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal("§c■ Stop request enviado pra " + target), true);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Dim entire bg (behind window)
        g.fillGradient(0, 0, width, height, 0xA0000000, 0xCC0A0518);

        // Decay tabAnim
        tabAnim = Math.max(0F, tabAnim - partial * 0.06F);

        // Smooth scroll lerp
        scrollSmooth += (scrollOffset - scrollSmooth) * 0.15F;

        // Compute layout
        int padLeft = winX + 14;
        int padRight = winX + winW - 14;
        int contentW = winW - 28;
        int yTitle = winY + 8;
        int yRow1 = yTitle + 18;
        int yRow2 = yRow1 + 18;
        int yTabs = yRow2 + 18;
        int yListTop = yTabs + 22;
        int yListBottom = winY + winH - 52;
        int yCustom = winY + winH - 36;
        int yCount = winY + winH - 18;

        // ─── COSMIC WINDOW BACKGROUND ───
        renderCosmicBg(g, winX, winY, winW, winH);

        // ─── TITLE BAR (drag handle) ───
        renderTitleBar(g, winX, winY, winW, yTitle);

        // ─── ROW LABELS ───
        g.drawString(font, "§7§oTarget:", padLeft, yRow1 + 3, 0xFFBFA8FF, true);
        g.drawString(font, "§7Interv: §e" + intervalSec + "s", padLeft + 80, yRow2 + 3, 0xFFFFFFFF, true);
        String durStr = durationSec == -1 ? "§l∞" : (durationSec / 60) + "min";
        g.drawString(font, "§7Dur: §e" + durStr, padLeft + 216, yRow2 + 3, 0xFFFFFFFF, true);

        // ─── TAB HIGHLIGHT (animated under current tab) ───
        int tabIdx = currentTabIndex();
        int tabW = contentW / 7;
        int tabHighlightX = padLeft + tabIdx * tabW;
        // Glow under active tab
        int glowAlpha = (int)(150 + 50 * Math.sin(System.currentTimeMillis() / 300.0));
        glowAlpha = Math.max(80, Math.min(200, glowAlpha));
        int glowColor = (glowAlpha << 24) | 0x5D2BB4;
        g.fill(tabHighlightX, yTabs + 14, tabHighlightX + tabW - 2, yTabs + 15, glowColor);
        // Animated slide (if recently changed tab)
        if (tabAnim > 0.05F) {
            int slideX = padLeft + (int)(prevTabIndex * tabW + (tabIdx - prevTabIndex) * tabW * (1F - tabAnim));
            g.fill(slideX, yTabs + 13, slideX + tabW - 2, yTabs + 14, 0xCCFFFFFF);
        }

        // ─── FILTER LABEL ───
        g.drawString(font, "§dFiltro: §7" + filterCategory,
                padLeft, yTabs - 10, 0xFFFFFFFF, true);
        g.drawString(font, "§dTemplates §7(click pra toggle):",
                padLeft, yListTop - 10, 0xFFFFFFFF, true);

        // ─── TEMPLATES LIST (smooth scroll) ───
        renderTemplatesList(g, mouseX, mouseY, padLeft, yListTop, padRight - 18 - padLeft, yListBottom);

        // ─── SELECTED COUNT ───
        int totalSel = selectedTemplates.size() + customMessages.size();
        g.drawString(font, "§dSel: §a" + totalSel + " §7("
                        + selectedTemplates.size() + "t§7+§a" + customMessages.size() + "c§7)",
                padLeft, yCount, 0xFFFFFFFF, true);

        // ─── CUSTOM LABEL ───
        g.drawString(font, "§dCustom:", padLeft, yCustom + 3, 0xFFFFFFFF, true);

        // ─── ESC HINT (bottom-right) ───
        String esc = "§7[ESC] fecha §8| §7arraste o topo pra mover";
        int escW = font.width(esc);
        g.drawString(font, esc, padRight - escW, yCount, 0xFFAAAAAA, true);

        super.render(g, mouseX, mouseY, partial);
    }

    /** Cosmic background com gradient roxo + glifos sutis. */
    private void renderCosmicBg(GuiGraphics g, int x, int y, int w, int h) {
        // Outer glow border (multiple layers)
        for (int i = 0; i < 4; i++) {
            int alpha = 30 + i * 10;
            int color = (alpha << 24) | 0x4D2B7A;
            g.fill(x - (4-i), y - (4-i), x + w + (4-i), y + h + (4-i), color);
        }
        // Main bg gradient
        g.fillGradient(x, y, x + w, y + h, 0xEE150528, 0xEE0A0A1A);
        // Inner highlight (top)
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
        // Inner shadow (bottom)
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x80000000);
        // Side glow lines (left + right)
        for (int i = 0; i < h; i += 2) {
            int alpha = (int)(80 + 80 * Math.sin(i * 0.05 + System.currentTimeMillis() / 800.0));
            int color = (Math.max(20, alpha) << 24) | 0x6B3FBF;
            g.fill(x, y + i, x + 1, y + i + 1, color);
            g.fill(x + w - 1, y + i, x + w, y + i + 1, color);
        }
        // Moving glyph particles (animated)
        long t = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            float phase = (t / 100F + i * 60F) % 360F;
            int gx = x + 20 + (int)((Math.sin(phase * 0.0174) + 1) * (w - 40) / 2);
            int gy = y + 20 + (int)((Math.cos(phase * 0.0174 * 0.7) + 1) * (h - 40) / 2);
            char[] glyphs = {'✦', '✧', '⚝', '⚹', '⟁', '⧫', '◈', '◇'};
            char glyph = glyphs[i];
            int alpha = 40 + (int)(40 * Math.sin(t / 400.0 + i));
            int color = (Math.max(20, alpha) << 24) | 0xAA88FF;
            g.drawString(font, String.valueOf(glyph), gx, gy, color, false);
        }
    }

    /** Title bar com gradient + título estilizado. */
    private void renderTitleBar(GuiGraphics g, int x, int y, int w, int yTitle) {
        // Title strip darker than bg
        g.fillGradient(x + 1, y + 1, x + w - 1, yTitle + 14, 0xCC2A0F50, 0xAA1A0535);
        // Animated underline
        long t = System.currentTimeMillis();
        for (int i = 0; i < w - 4; i++) {
            float wave = (float)Math.sin(i * 0.08 + t / 200.0);
            int alpha = (int)(60 + 80 * wave);
            int color = (Math.max(20, alpha) << 24) | 0x8855FF;
            g.fill(x + 2 + i, yTitle + 14, x + 3 + i, yTitle + 15, color);
        }
        // Title text com animação pulsante
        String title = "✦ CARETAKER CONSOLE ✦";
        int tw = font.width(title);
        int titleX = x + (w - tw) / 2;
        // Glow behind
        for (int i = 1; i <= 2; i++) {
            int a = 40 + (int)(20 * Math.sin(t / 300.0));
            int gc = (Math.max(20, a) << 24) | 0xFF5577;
            g.drawString(font, "§l" + title, titleX + i, yTitle, gc, false);
            g.drawString(font, "§l" + title, titleX - i, yTitle, gc, false);
        }
        // Main title
        g.drawString(font, "§4§l" + title, titleX, yTitle, 0xFFFF99CC, true);
    }

    /** Lista de templates com smooth scroll + hover glow. */
    private void renderTemplatesList(GuiGraphics g, int mouseX, int mouseY,
                                      int x, int y, int w, int yBottom) {
        int rows = Math.max(1, (yBottom - y) / ROW_HEIGHT);
        // List bg
        g.fill(x - 2, y - 2, x + w + 2, y + rows * ROW_HEIGHT + 2, 0xCC0A0518);
        // Inner shadow
        g.fill(x, y, x + w, y + 1, 0xAA000000);

        List<MessageTemplates.Template> visible = getFilteredTemplates();
        // Smooth scroll offset (fractional)
        int displayOffset = (int)scrollSmooth;
        float fractional = scrollSmooth - displayOffset;
        int offsetPx = -(int)(fractional * ROW_HEIGHT);

        // Clip rendering (we don't have GuiGraphics.enableScissor here, use overlap fill)
        for (int i = 0; i < rows + 1 && i + displayOffset < visible.size(); i++) {
            MessageTemplates.Template t = visible.get(i + displayOffset);
            int rowY = y + i * ROW_HEIGHT + offsetPx;
            if (rowY < y - ROW_HEIGHT || rowY > yBottom) continue;
            boolean selected = selectedTemplates.contains(t.id);
            boolean hover = mouseX >= x && mouseX < x + w
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            int bg;
            if (selected) bg = 0xDD5D2BB4;
            else if (hover) bg = 0x88AA77FF;
            else bg = (i % 2 == 0) ? 0x44190B30 : 0x44120822;
            g.fill(x, rowY, x + w, rowY + ROW_HEIGHT - 1, bg);
            // Checkbox
            String check = selected ? "§a[✓]" : "§7[ ]";
            g.drawString(font, check, x + 4, rowY + 3, 0xFFFFFFFF, true);
            // Label
            String label = t.label;
            // Trim if too wide
            int maxLabelW = w - 100;
            while (font.width(label) > maxLabelW && label.length() > 4) {
                label = label.substring(0, label.length() - 4) + "..";
            }
            g.drawString(font, "§f" + label, x + 28, rowY + 3, 0xFFFFFFFF, true);
            // Category badge
            g.drawString(font, "§8" + t.category, x + w - 70, rowY + 3, 0xFFAAAAAA, true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Title bar drag check
        int yTitle = winY + 8;
        if (button == 0 && mx >= winX && mx < winX + winW - 24
                && my >= winY && my < yTitle + 14) {
            dragging = true;
            dragOffX = (int)(mx - winX);
            dragOffY = (int)(my - winY);
            return true;
        }

        // Click numa row da lista pra toggle
        int padLeft = winX + 14;
        int padRight = winX + winW - 14;
        int yTabs = winY + 8 + 18 + 18 + 18;
        int yListTop = yTabs + 22;
        int yListBottom = winY + winH - 52;
        int listW = padRight - 18 - padLeft;
        int rows = Math.max(1, (yListBottom - yListTop) / ROW_HEIGHT);

        if (mx >= padLeft && mx < padLeft + listW && my >= yListTop
                && my < yListTop + rows * ROW_HEIGHT) {
            int row = (int)((my - yListTop) / ROW_HEIGHT);
            List<MessageTemplates.Template> visible = getFilteredTemplates();
            if (row + scrollOffset < visible.size()) {
                MessageTemplates.Template t = visible.get(row + scrollOffset);
                if (selectedTemplates.contains(t.id)) selectedTemplates.remove(t.id);
                else selectedTemplates.add(t.id);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            int newX = (int)(mx - dragOffX);
            int newY = (int)(my - dragOffY);
            // Clamp dentro da tela
            newX = Math.max(0, Math.min(width - winW, newX));
            newY = Math.max(0, Math.min(height - winH, newY));
            if (newX != winX || newY != winY) {
                winX = newX;
                winY = newY;
                buildLayout();
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int padLeft = winX + 14;
        int padRight = winX + winW - 14;
        int yTabs = winY + 8 + 18 + 18 + 18;
        int yListTop = yTabs + 22;
        int yListBottom = winY + winH - 52;
        int listW = padRight - 18 - padLeft;
        int rows = Math.max(1, (yListBottom - yListTop) / ROW_HEIGHT);

        if (mx >= padLeft && mx < padLeft + listW && my >= yListTop
                && my < yListTop + rows * ROW_HEIGHT) {
            List<MessageTemplates.Template> visible = getFilteredTemplates();
            int max = Math.max(0, visible.size() - rows);
            if (delta < 0) scrollOffset = Math.min(max, scrollOffset + 1);
            else scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if ((targetBox != null && targetBox.isFocused())
                || (customMsgBox != null && customMsgBox.isFocused())) {
            if (targetBox != null && targetBox.isFocused()
                    && targetBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (customMsgBox != null && customMsgBox.isFocused()
                    && customMsgBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (targetBox != null && targetBox.isFocused())
            return targetBox.charTyped(codePoint, modifiers);
        if (customMsgBox != null && customMsgBox.isFocused())
            return customMsgBox.charTyped(codePoint, modifiers);
        return super.charTyped(codePoint, modifiers);
    }
}
