package br.com.murilo.liberthia.observation.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import br.com.murilo.liberthia.observation.item.ObservationTomeItem;
import br.com.murilo.liberthia.observation.network.SaveCompositionC2SPacket;
import br.com.murilo.liberthia.observation.source.SourceData;
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
 * v0.1.22 r66: <b>Spell Book Screen</b> — GUI estilo Ars Nouveau.
 *
 * <h2>Layout</h2>
 * <pre>
 *  ╔════════════════════════════════════════════════════════════╗
 *  ║  ✦ GRIMÓRIO DE OBSERVAÇÃO ✦                         [✕]  ║
 *  ║                                                             ║
 *  ║  [   Source Bar (animated gradient purple→cyan)        ]   ║
 *  ║  Source: 35/100  •  Spell Cost: 50  •  Selected: Mirror   ║
 *  ║                                                             ║
 *  ║  ━━━ Observation Methods ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ║
 *  ║  [G][P][M][S][R]                                            ║
 *  ║                                                             ║
 *  ║  ━━━ Manifestations ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ║
 *  ║  [T][S][M][W][D][G]                                         ║
 *  ║                                                             ║
 *  ║  ━━━ Distortions ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ║
 *  ║  [A][L][E][S]                                               ║
 *  ║                                                             ║
 *  ║  Hover sobre glyph mostra nome + custo.                    ║
 *  ║  Click no glyph SELECIONA o preset associado.              ║
 *  ║                                                             ║
 *  ║  [✓ APRENDER ESTE FEITIÇO]                  [CANCELAR]    ║
 *  ╚════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Glyph icons</h2>
 * Cada um é uma textura 16x16 em {@code textures/observation/glyph/}.
 * Glyphs categorizados em 3 rows, cada glyph mapeia a 1 dos 6 preset spells
 * (clicar atalho seleciona o preset que mais usa esse glyph).
 */
@OnlyIn(Dist.CLIENT)
public class SpellBookScreen extends Screen {

    /** GUI texture pack. */
    private static final ResourceLocation BG_TEX =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/spell_book_bg.png");
    private static final ResourceLocation SLOT_EMPTY =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/glyph_slot_empty.png");
    private static final ResourceLocation SLOT_SELECTED =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/glyph_slot_selected.png");
    private static final ResourceLocation SOURCE_BAR_BG =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/source_bar_bg.png");
    private static final ResourceLocation SOURCE_BAR_FILL =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/source_bar_fill.png");

    /** Each glyph entry: texture path + display name + preset mapping. */
    private static class GlyphEntry {
        final String texPath; final String name; final int preset; final String category;
        GlyphEntry(String tex, String nm, int pre, String cat) {
            this.texPath = tex; this.name = nm; this.preset = pre; this.category = cat;
        }
        ResourceLocation tex() {
            return new ResourceLocation(LiberthiaMod.MODID, "textures/observation/glyph/" + texPath + ".png");
        }
    }

    /** 15 glyphs em 3 categorias. Cada um mapeia a 1 dos 6 spell presets. */
    private static final GlyphEntry[] GLYPHS = {
        // 5 WatchMethods
        new GlyphEntry("watch_direct_gaze", "Olhar Direto", 0, "Observation"),
        new GlyphEntry("watch_peripheral", "Periférico",  3, "Observation"),
        new GlyphEntry("watch_memory",     "Memória",     5, "Observation"),
        new GlyphEntry("watch_silence",    "Silêncio",    1, "Observation"),
        new GlyphEntry("watch_reflection", "Reflexão",    2, "Observation"),
        // 6 Manifestations
        new GlyphEntry("manifest_tendril",  "Tentáculos",   0, "Manifestation"),
        new GlyphEntry("manifest_silence",  "Silenciar",    1, "Manifestation"),
        new GlyphEntry("manifest_mirror",   "Espelhar",     2, "Manifestation"),
        new GlyphEntry("manifest_whisper",  "Sussurrar",    3, "Manifestation"),
        new GlyphEntry("manifest_decay",    "Decaimento",   4, "Manifestation"),
        new GlyphEntry("manifest_glimpse",  "Vislumbre",    0, "Manifestation"),
        // 4 Distortions
        new GlyphEntry("distort_amplify", "Amplificar", 0, "Distortion"),
        new GlyphEntry("distort_linger",  "Persistir",  2, "Distortion"),
        new GlyphEntry("distort_echo",    "Eco",        4, "Distortion"),
        new GlyphEntry("distort_secret",  "Secreto",    5, "Distortion"),
    };

    private int selectedPreset;
    private int hoveredGlyph = -1;
    private int winX, winY, winW, winH;
    private long openedAt;

    public SpellBookScreen(int currentPreset) {
        super(Component.literal("Spell Book"));
        this.selectedPreset = Math.max(0, Math.min(5, currentPreset));
    }

    public static void openNow(int preset) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new SpellBookScreen(preset));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        openedAt = System.currentTimeMillis();
        winW = 304;
        winH = 220;
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        // Save button bottom-left
        this.addRenderableWidget(Button.builder(
                Component.literal("§a§l✓ APRENDER FEITIÇO"),
                btn -> save())
                .bounds(winX + 14, winY + winH - 28, 160, 20).build());

        // Cancel bottom-right
        this.addRenderableWidget(Button.builder(
                Component.literal("§c§lCANCELAR"),
                btn -> this.onClose())
                .bounds(winX + winW - 90, winY + winH - 28, 76, 20).build());

        // Close X
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
        // Background dim
        g.fillGradient(0, 0, width, height, 0xC0000000, 0xE0000000);

        // Bg texture full window
        g.blit(BG_TEX, winX, winY, 0, 0, winW, winH, 256, 256);

        // Outer cosmic glow border (animated)
        long t = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            int alpha = (int)(30 + 40 * Math.sin(t / 500.0 + i));
            int color = (Math.max(20, alpha) << 24) | 0x9D4DD6;
            g.fill(winX - (4-i), winY - (4-i), winX + winW + (4-i), winY - (4-i) + 1, color);
            g.fill(winX - (4-i), winY + winH + (4-i) - 1, winX + winW + (4-i), winY + winH + (4-i), color);
            g.fill(winX - (4-i), winY - (4-i), winX - (4-i) + 1, winY + winH + (4-i), color);
            g.fill(winX + winW + (4-i) - 1, winY - (4-i), winX + winW + (4-i), winY + winH + (4-i), color);
        }

        // Title
        String title = "✦ GRIMÓRIO DE OBSERVAÇÃO ✦";
        int tw = font.width(title);
        // Title glow behind
        for (int i = 1; i <= 2; i++) {
            int a = 40 + (int)(20 * Math.sin(t / 300.0));
            int gc = (Math.max(20, a) << 24) | 0xFF5577;
            g.drawString(font, "§l" + title, winX + (winW - tw)/2 + i, winY + 7, gc, false);
            g.drawString(font, "§l" + title, winX + (winW - tw)/2 - i, winY + 7, gc, false);
        }
        g.drawString(font, "§5§l" + title, winX + (winW - tw)/2, winY + 7, 0xFFFF99CC, true);

        // Source bar
        renderSourceBar(g, winX + 14, winY + 30, winW - 28);

        // Info line
        ObservationSpell selectedSpell = ObservationTomeItem.buildPreset(selectedPreset);
        String info = "§7Custo: §c" + selectedSpell.totalSourceCost()
            + " Source§7 | Selected: §e" + selectedSpell.name();
        g.drawString(font, info, winX + 14, winY + 44, 0xFFFFFFFF, true);

        // Section: Observation Methods (5 glyphs, row at y=64)
        g.drawString(font, "§5§l━━ §rObservation Methods §5§l━━━━━━━━━━━",
            winX + 14, winY + 60, 0xFFCCAAEE, true);
        renderGlyphRow(g, 0, 5, winX + 18, winY + 72, mouseX, mouseY);

        // Section: Manifestations (6 glyphs, row at y=110)
        g.drawString(font, "§5§l━━ §rManifestations §5§l━━━━━━━━━━━━━━━━━",
            winX + 14, winY + 102, 0xFFCCAAEE, true);
        renderGlyphRow(g, 5, 11, winX + 18, winY + 114, mouseX, mouseY);

        // Section: Distortions (4 glyphs, row at y=152)
        g.drawString(font, "§5§l━━ §rDistortions §5§l━━━━━━━━━━━━━━━━━━━━━",
            winX + 14, winY + 144, 0xFFCCAAEE, true);
        renderGlyphRow(g, 11, 15, winX + 18, winY + 156, mouseX, mouseY);

        // Hover tooltip
        if (hoveredGlyph >= 0 && hoveredGlyph < GLYPHS.length) {
            renderTooltip(g, mouseX, mouseY, GLYPHS[hoveredGlyph]);
        }

        super.render(g, mouseX, mouseY, partial);
    }

    /** Render a row of glyphs from index start..end (exclusive). */
    private void renderGlyphRow(GuiGraphics g, int start, int end, int x, int y,
                                  int mouseX, int mouseY) {
        int gap = 28; // slot 24px + 4px gap
        for (int i = start; i < end; i++) {
            int idx = i - start;
            int slotX = x + idx * gap;
            int slotY = y;
            boolean selected = GLYPHS[i].preset == selectedPreset;
            boolean hover = mouseX >= slotX && mouseX < slotX + 24
                          && mouseY >= slotY && mouseY < slotY + 24;
            if (hover) hoveredGlyph = i;

            // Slot bg (empty or selected)
            g.blit(selected ? SLOT_SELECTED : SLOT_EMPTY, slotX, slotY, 0, 0, 24, 24, 24, 24);

            // Glyph icon (16x16) centered in slot (offset +4)
            g.blit(GLYPHS[i].tex(), slotX + 4, slotY + 4, 0, 0, 16, 16, 16, 16);

            // Hover glow
            if (hover) {
                long t = System.currentTimeMillis();
                int alpha = (int)(150 + 80 * Math.sin(t / 200.0));
                int color = (Math.max(80, alpha) << 24) | 0xFFAA55;
                g.fill(slotX - 1, slotY - 1, slotX + 25, slotY, color);
                g.fill(slotX - 1, slotY + 24, slotX + 25, slotY + 25, color);
                g.fill(slotX - 1, slotY - 1, slotX, slotY + 25, color);
                g.fill(slotX + 24, slotY - 1, slotX + 25, slotY + 25, color);
            }
        }
        if (mouseY < y || mouseY > y + 24) {
            // Reset hover if mouse not in this row's Y range
        }
    }

    private void renderSourceBar(GuiGraphics g, int x, int y, int width) {
        int currentSource = SourceData.get(Minecraft.getInstance().player);
        int maxSource = SourceData.getMax(Minecraft.getInstance().player);
        ObservationSpell selectedSpell = ObservationTomeItem.buildPreset(selectedPreset);
        int cost = selectedSpell.totalSourceCost();

        // Bg
        g.blit(SOURCE_BAR_BG, x, y, 0, 0, width, 8, 100, 8);
        // Fill
        int fillWidth = (int)((width - 2) * Math.min(1.0, (double)currentSource / maxSource));
        if (fillWidth > 0) {
            // Scale the source bar fill to fit
            g.blit(SOURCE_BAR_FILL, x + 1, y + 1, 0, 0, fillWidth, 6, 100, 8);
        }
        // Cost indicator (red mark where the cost line is)
        if (cost > 0 && cost <= maxSource) {
            int costX = x + 1 + (int)((width - 2) * ((double)cost / maxSource));
            int red = currentSource >= cost ? 0xFF55FF55 : 0xFFFF5555;
            g.fill(costX, y - 1, costX + 1, y + 9, red);
        }
        // Label
        String label = currentSource + " / " + maxSource;
        int lw = font.width(label);
        g.drawString(font, label, x + width / 2 - lw / 2, y + 10, 0xFFFFFFFF, true);
    }

    private void renderTooltip(GuiGraphics g, int mouseX, int mouseY, GlyphEntry glyph) {
        ObservationSpell linkedSpell = ObservationTomeItem.buildPreset(glyph.preset);
        String[] lines = {
            "§l§d" + glyph.name,
            "§7Categoria: §e" + glyph.category,
            "§7Spell: §a" + linkedSpell.name(),
            "§7Custo: §c" + linkedSpell.totalSourceCost() + " Source",
            "§8Clique pra selecionar"
        };
        int maxW = 0;
        for (String l : lines) maxW = Math.max(maxW, font.width(l));
        int boxW = maxW + 8;
        int boxH = lines.length * 11 + 6;
        int bx = mouseX + 8;
        int by = mouseY - boxH - 4;
        if (bx + boxW > width) bx = mouseX - boxW - 4;
        if (by < 0) by = mouseY + 12;
        // Bg
        g.fill(bx, by, bx + boxW, by + boxH, 0xE0150528);
        g.fill(bx - 1, by, bx, by + boxH, 0xFF9D4DD6);
        g.fill(bx + boxW, by, bx + boxW + 1, by + boxH, 0xFF9D4DD6);
        g.fill(bx, by - 1, bx + boxW, by, 0xFF9D4DD6);
        g.fill(bx, by + boxH, bx + boxW, by + boxH + 1, 0xFF9D4DD6);
        for (int i = 0; i < lines.length; i++) {
            g.drawString(font, lines[i], bx + 4, by + 4 + i * 11, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && hoveredGlyph >= 0 && hoveredGlyph < GLYPHS.length) {
            selectedPreset = GLYPHS[hoveredGlyph].preset;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        hoveredGlyph = -1;
        // Set when the row render detects hover
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_6) {
            selectedPreset = keyCode - GLFW.GLFW_KEY_1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
