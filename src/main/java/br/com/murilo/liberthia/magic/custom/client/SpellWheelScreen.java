package br.com.murilo.liberthia.magic.custom.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.custom.CustomSpell;
import br.com.murilo.liberthia.magic.custom.CustomSpellClientCache;
import br.com.murilo.liberthia.magic.custom.SelectSpellC2SPacket;
import br.com.murilo.liberthia.magic.custom.CastCustomSpellC2SPacket;
import br.com.murilo.liberthia.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * v0.1.22 r42: <b>Spell Wheel</b> — UI radial pra selecionar/castar custom spells.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li><b>Abre</b> ao segurar a keybind (default X)</li>
 *   <li><b>Movimenta mouse</b> pra hovering num spell — anel central destaca</li>
 *   <li><b>Solta keybind</b>:
 *     <ul>
 *       <li>Se mouse sobre algum spell → CASTA imediato</li>
 *       <li>Se centro → SELECIONA o spell mas não casta (atalho)</li>
 *     </ul>
 *   </li>
 *   <li><b>Right-click hover</b> = seleciona o spell como ativo (sem castar)</li>
 *   <li><b>Q hover</b> = deleta o spell (com confirm)</li>
 * </ul>
 *
 * <h2>Layout</h2>
 * Spells distribuídos em círculo. Anel interno mostra ícones (sprite color).
 * Centro mostra nome + stats do spell hovered.
 */
@OnlyIn(Dist.CLIENT)
public class SpellWheelScreen extends Screen {

    private static final int RING_RADIUS = 90;
    private static final int ICON_RADIUS = 24;

    private int hoveredIndex = -1;
    private boolean castOnRelease = true;

    public SpellWheelScreen() {
        super(Component.literal("Spell Wheel"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        // Center mouse
        if (minecraft != null && minecraft.mouseHandler != null) {
            // Already grabbed in main game
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Dark vignette background
        g.fillGradient(0, 0, width, height, 0xCC000000, 0xEE000000);

        int cx = width / 2;
        int cy = height / 2;

        List<CustomSpell> spells = CustomSpellClientCache.getSpells();

        if (spells.isEmpty()) {
            String msg = "§7Nenhum feitiço criado.";
            String hint = "§8Use a §dMesa de Feitiços§8 pra criar.";
            int w1 = font.width(msg), w2 = font.width(hint);
            g.drawString(font, msg, cx - w1 / 2, cy - 8, 0xFFFFFFFF, true);
            g.drawString(font, hint, cx - w2 / 2, cy + 4, 0xFFAAAAAA, true);
            // ESC hint
            String esc = "§8[ESC pra fechar]";
            int ew = font.width(esc);
            g.drawString(font, esc, cx - ew / 2, height - 20, 0xFFAAAAAA, true);
            return;
        }

        // Determine hovered slice based on mouse angle
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        hoveredIndex = -1;
        if (dist > 30 && dist < RING_RADIUS + ICON_RADIUS + 10) {
            double angle = Math.atan2(dy, dx);
            // Normalize 0..2π starting at top (-PI/2)
            double normalized = (angle + Math.PI * 2.5) % (Math.PI * 2);
            int slice = (int) (normalized / (Math.PI * 2 / spells.size()));
            if (slice >= 0 && slice < spells.size()) {
                hoveredIndex = slice;
            }
        }

        // Draw central circle (info area)
        int centerR = 28;
        g.fill(cx - centerR, cy - centerR, cx + centerR, cy + centerR, 0xAA1A0830);
        // Inner glow
        for (int r = centerR; r > 0; r -= 2) {
            int alpha = (int)(((float)r / centerR) * 40);
            int color = (alpha << 24) | 0x553388;
            g.fill(cx - r, cy - r, cx + r, cy + r, color);
        }

        // Draw spell icons in ring
        int n = spells.size();
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + (i / (double) n) * Math.PI * 2;
            int sx = cx + (int) (Math.cos(angle) * RING_RADIUS);
            int sy = cy + (int) (Math.sin(angle) * RING_RADIUS);
            drawSpellSlice(g, spells.get(i), sx, sy, i == hoveredIndex);
        }

        // Center info — show hovered or selected
        CustomSpell focus = hoveredIndex >= 0 ? spells.get(hoveredIndex)
                : CustomSpellClientCache.getSelected();
        if (focus != null) {
            drawCenterInfo(g, cx, cy, focus);
        } else {
            String txt = "§dSpell Wheel";
            int w = font.width(txt);
            g.drawString(font, txt, cx - w / 2, cy - 4, 0xFFFFFFFF, true);
        }

        // Hint at bottom (r58: clarifica que só seleciona, não casta)
        String hint = "§7Click/Solte X = §6SELECIONAR§7 | §cQ§7 = deletar | §7ESC fecha | §dRclick Grimoire§7 = CASTAR";
        int hw = font.width(hint);
        g.drawString(font, hint, cx - hw / 2, height - 20, 0xFFCCCCCC, true);
    }

    private void drawSpellSlice(GuiGraphics g, CustomSpell s, int cx, int cy, boolean hovered) {
        int r = hovered ? ICON_RADIUS + 6 : ICON_RADIUS;
        // BG circle (sprite color)
        int color = s.effectiveColor();
        int alpha = hovered ? 0xDD : 0x99;
        int fillColor = (alpha << 24) | color;
        // Square approximation (cheap circle)
        g.fill(cx - r, cy - r, cx + r, cy + r, fillColor);
        // Border
        int borderColor = hovered ? 0xFFFFFFFF : 0xFF000000;
        g.fill(cx - r, cy - r, cx + r, cy - r + 1, borderColor);
        g.fill(cx - r, cy + r - 1, cx + r, cy + r, borderColor);
        g.fill(cx - r, cy - r, cx - r + 1, cy + r, borderColor);
        g.fill(cx + r - 1, cy - r, cx + r, cy + r, borderColor);

        // Sprite icon — render texture
        try {
            ResourceLocation tex = new ResourceLocation("liberthia",
                    "textures/particle/spell_" + s.sprite.id + "_0.png");
            int sz = r * 2 - 6;
            g.blit(tex, cx - sz / 2, cy - sz / 2, 0, 0, sz, sz, sz, sz);
        } catch (Throwable ignored) {}

        // Name below
        String name = s.name;
        if (font.width(name) > r * 3) {
            name = font.plainSubstrByWidth(name, r * 3 - 4) + "..";
        }
        int nw = font.width(name);
        g.drawString(font, name, cx - nw / 2, cy + r + 4,
                hovered ? 0xFFFFFFFF : 0xFFCCCCCC, true);
    }

    private void drawCenterInfo(GuiGraphics g, int cx, int cy, CustomSpell s) {
        String name = s.name;
        Component shape = s.shape.displayComponent();
        Component element = s.element.displayComponent();
        String stats = "§7P" + s.power + " · §b" + s.manaCost() + "§7m · §c" + (int) s.damage() + "§7dmg";

        int nw = font.width(name);
        g.drawString(font, "§l" + name, cx - nw / 2, cy - 18, s.effectiveColor() | 0xFF000000, true);

        int shw = font.width(shape);
        g.drawString(font, shape, cx - shw / 2, cy - 6, 0xFFAACCFF, true);

        int ew = font.width(element);
        g.drawString(font, element, cx - ew / 2, cy + 4, s.element.color | 0xFF000000, true);

        int sw = font.width(stats);
        g.drawString(font, stats, cx - sw / 2, cy + 16, 0xFFFFFFFF, true);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // r58 FIX: Release of X = SELECT hovered (NOT cast). User cast via right-click.
        if (keyCode == GLFW.GLFW_KEY_X) {
            selectHovered();
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** r58: Apenas SELECIONA o spell hovered no servidor (não casta). */
    private void selectHovered() {
        List<CustomSpell> spells = CustomSpellClientCache.getSpells();
        if (hoveredIndex >= 0 && hoveredIndex < spells.size()) {
            CustomSpell s = spells.get(hoveredIndex);
            ModNetwork.CHANNEL.sendToServer(new SelectSpellC2SPacket(s.id));
            CustomSpellClientCache.update(spells, s.id);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§6✓ Selecionado: §r§e" + s.name
                                + " §8(use right-click do Grimoire pra castar)"), true);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_Q) {
            // Delete hovered
            List<CustomSpell> spells = CustomSpellClientCache.getSpells();
            if (hoveredIndex >= 0 && hoveredIndex < spells.size()) {
                CustomSpell s = spells.get(hoveredIndex);
                ModNetwork.CHANNEL.sendToServer(
                        new br.com.murilo.liberthia.magic.custom.DeleteSpellC2SPacket(s.id));
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.displayClientMessage(
                            Component.literal("§c✗ Deletado: " + s.name), true);
                }
                this.onClose();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        List<CustomSpell> spells = CustomSpellClientCache.getSpells();
        if (hoveredIndex < 0 || hoveredIndex >= spells.size()) return false;
        CustomSpell s = spells.get(hoveredIndex);

        // r58 FIX: AMBOS left e right click = SELECIONAR (não castar)
        // Cast só via right-click do Grimoire no mundo.
        if (button == 0 || button == 1) {
            ModNetwork.CHANNEL.sendToServer(new SelectSpellC2SPacket(s.id));
            CustomSpellClientCache.update(spells, s.id);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§6✓ Selecionado: §r§e" + s.name
                                + " §8(use right-click do Grimoire pra castar)"), true);
            }
            this.onClose();
            return true;
        }
        return false;
    }

    private void castHovered() {
        List<CustomSpell> spells = CustomSpellClientCache.getSpells();
        if (hoveredIndex >= 0 && hoveredIndex < spells.size()) {
            CustomSpell s = spells.get(hoveredIndex);
            castSpell(s);
        }
    }

    private void castSpell(CustomSpell s) {
        ModNetwork.CHANNEL.sendToServer(new CastCustomSpellC2SPacket(s.id));
    }
}
