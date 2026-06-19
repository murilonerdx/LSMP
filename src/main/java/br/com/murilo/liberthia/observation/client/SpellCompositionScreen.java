package br.com.murilo.liberthia.observation.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import br.com.murilo.liberthia.observation.item.ObservationTomeItem;
import br.com.murilo.liberthia.observation.network.SaveCompositionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * v0.1.22 r65: <b>Spell Composition Screen</b> — GUI visual pra escolher
 * preset do Grimório.
 *
 * <h2>Layout (responsive)</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │  ✦ GRIMÓRIO DE OBSERVAÇÃO ✦                            ✕   │
 * │                                                              │
 * │  Preset Atual: §eTendril§r                                  │
 * │  Custo: §c35 Source §7| §c7 Sanity                          │
 * │                                                              │
 * │  Escolha o feitiço:                                          │
 * │                                                              │
 * │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │
 * │  │  Tendril     │ │  Silence     │ │  Mirror      │         │
 * │  │  35 Source   │ │  50 Source   │ │  60 Source   │         │
 * │  └──────────────┘ └──────────────┘ └──────────────┘         │
 * │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │
 * │  │  Decay Echo  │ │ WhisperSprd  │ │ MemTendril   │         │
 * │  │  60 Source   │ │  55 Source   │ │  65 Source   │         │
 * │  └──────────────┘ └──────────────┘ └──────────────┘         │
 * │                                                              │
 * │  [SALVAR NO GRIMÓRIO]  [CANCELAR]                            │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Pattern AN's GuiSpellBook: tema cósmico, glifos animados, drag-style
 * mas simplificado em 6 preset buttons.
 */
@OnlyIn(Dist.CLIENT)
public class SpellCompositionScreen extends Screen {

    private int selectedPreset = 0;
    private int winX, winY, winW, winH;
    private long openedAt;

    /** Cor por preset (mantém em sync com ObservationTomeItem.buildPreset). */
    private static final int[] PRESET_COLORS = {
        0x9D4DD6, 0xCCCCDD, 0x9988CC, 0x3D6010, 0x5544AA, 0x880088
    };

    public SpellCompositionScreen(int currentPreset) {
        super(Component.literal("Spell Composition"));
        this.selectedPreset = Math.max(0, Math.min(5, currentPreset));
    }

    /** Helper chamado pelo OpenCompositionScreenS2CPacket. */
    public static void openNow(int currentPreset) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new SpellCompositionScreen(currentPreset));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        openedAt = System.currentTimeMillis();

        winW = Math.min(440, Math.max(360, width - 60));
        winH = Math.min(340, Math.max(260, height - 60));
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();

        // 6 preset buttons em grid 3x2
        int btnW = 110;
        int btnH = 42;
        int gap = 8;
        int gridW = btnW * 3 + gap * 2;
        int startX = winX + (winW - gridW) / 2;
        int startY = winY + 100;

        for (int i = 0; i < 6; i++) {
            int col = i % 3;
            int row = i / 3;
            int x = startX + col * (btnW + gap);
            int y = startY + row * (btnH + gap);
            final int presetIdx = i;
            ObservationSpell spell = ObservationTomeItem.buildPreset(i);

            String label = "§l" + spell.name() + "\n§r§7" + spell.totalSourceCost() + " S";
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    btn -> { selectedPreset = presetIdx; })
                    .bounds(x, y, btnW, btnH).build());
        }

        // SALVAR button
        int saveY = winY + winH - 32;
        this.addRenderableWidget(Button.builder(
                Component.literal("§a§l✓ SALVAR NO GRIMÓRIO"),
                btn -> save())
                .bounds(winX + 20, saveY, 180, 22).build());

        // CANCELAR button
        this.addRenderableWidget(Button.builder(
                Component.literal("§c§lCANCELAR"),
                btn -> this.onClose())
                .bounds(winX + winW - 100, saveY, 80, 22).build());

        // Close X button top-right
        this.addRenderableWidget(Button.builder(
                Component.literal("§c§l✕"),
                btn -> this.onClose())
                .bounds(winX + winW - 22, winY + 4, 18, 14).build());
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new SaveCompositionC2SPacket(selectedPreset));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Dim background
        g.fillGradient(0, 0, width, height, 0xA0000000, 0xCC0A0518);

        // Cosmic window background (gradient + glow border)
        renderCosmicBg(g, winX, winY, winW, winH);

        // Title bar
        renderTitle(g, winX, winY, winW);

        // Selected preset info
        ObservationSpell selected = ObservationTomeItem.buildPreset(selectedPreset);
        int cx = winX + winW / 2;

        String presetText = "§7Preset: §e§l" + selected.name();
        int tw = font.width(presetText);
        g.drawString(font, presetText, cx - tw / 2, winY + 50, 0xFFFFFFFF, true);

        String costText = "§7Custo: §c" + selected.totalSourceCost()
                + " Source §8| §c" + selected.totalSanityCost() + " Sanity";
        int cw = font.width(costText);
        g.drawString(font, costText, cx - cw / 2, winY + 64, 0xFFFFFFFF, true);

        String hint = "§7Clique num botão pra selecionar — depois §aSALVAR";
        int hw = font.width(hint);
        g.drawString(font, hint, cx - hw / 2, winY + 80, 0xFFAAAAAA, true);

        // Highlight selected preset button (glow border)
        int btnW = 110, btnH = 42, gap = 8;
        int gridW = btnW * 3 + gap * 2;
        int startX = winX + (winW - gridW) / 2;
        int startY = winY + 100;
        int col = selectedPreset % 3;
        int row = selectedPreset / 3;
        int sx = startX + col * (btnW + gap);
        int sy = startY + row * (btnH + gap);
        long t = System.currentTimeMillis() - openedAt;
        int glowAlpha = (int)(150 + 80 * Math.sin(t / 300.0));
        glowAlpha = Math.max(80, Math.min(220, glowAlpha));
        int glowColor = (glowAlpha << 24) | (PRESET_COLORS[selectedPreset] & 0xFFFFFF);
        // 4 borders thick
        for (int b = 0; b < 3; b++) {
            g.fill(sx - b, sy - b, sx + btnW + b, sy - b + 1, glowColor);
            g.fill(sx - b, sy + btnH + b - 1, sx + btnW + b, sy + btnH + b, glowColor);
            g.fill(sx - b, sy - b, sx - b + 1, sy + btnH + b, glowColor);
            g.fill(sx + btnW + b - 1, sy - b, sx + btnW + b, sy + btnH + b, glowColor);
        }

        super.render(g, mouseX, mouseY, partial);
    }

    private void renderCosmicBg(GuiGraphics g, int x, int y, int w, int h) {
        // Outer glow
        for (int i = 0; i < 5; i++) {
            int alpha = 25 + i * 12;
            int color = (alpha << 24) | 0x4D2B7A;
            g.fill(x - (5-i), y - (5-i), x + w + (5-i), y + h + (5-i), color);
        }
        // Main bg
        g.fillGradient(x, y, x + w, y + h, 0xEE150528, 0xEE0A0A1A);
        // Top highlight
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
        // Side glow animated
        long t = System.currentTimeMillis();
        for (int i = 0; i < h; i += 2) {
            int alpha = (int)(60 + 80 * Math.sin(i * 0.05 + t / 800.0));
            int color = (Math.max(20, alpha) << 24) | 0x6B3FBF;
            g.fill(x, y + i, x + 1, y + i + 1, color);
            g.fill(x + w - 1, y + i, x + w, y + i + 1, color);
        }
    }

    private void renderTitle(GuiGraphics g, int x, int y, int w) {
        // Title strip
        g.fillGradient(x + 1, y + 1, x + w - 1, y + 22, 0xCC2A0F50, 0xAA1A0535);
        long t = System.currentTimeMillis();
        // Underline wave
        for (int i = 0; i < w - 4; i++) {
            float wave = (float)Math.sin(i * 0.08 + t / 200.0);
            int alpha = (int)(60 + 80 * wave);
            int color = (Math.max(20, alpha) << 24) | 0x8855FF;
            g.fill(x + 2 + i, y + 22, x + 3 + i, y + 23, color);
        }
        // Title
        String title = "✦ GRIMÓRIO DE OBSERVAÇÃO ✦";
        int tw = font.width(title);
        int titleX = x + (w - tw) / 2;
        // Pulse glow behind title
        for (int i = 1; i <= 2; i++) {
            int a = 40 + (int)(20 * Math.sin(t / 300.0));
            int gc = (Math.max(20, a) << 24) | 0xFF5577;
            g.drawString(font, "§l" + title, titleX + i, y + 7, gc, false);
            g.drawString(font, "§l" + title, titleX - i, y + 7, gc, false);
        }
        g.drawString(font, "§5§l" + title, titleX, y + 7, 0xFFFF99CC, true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        // Number keys 1-6 = quick select preset
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_6) {
            selectedPreset = keyCode - GLFW.GLFW_KEY_1;
            return true;
        }
        // Enter = save
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
