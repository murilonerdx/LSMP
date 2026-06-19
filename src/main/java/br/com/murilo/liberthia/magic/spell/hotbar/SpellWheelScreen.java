package br.com.murilo.liberthia.magic.spell.hotbar;

import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * r155: <b>Radial Spell Wheel</b> — abre ao segurar R, mostra os 8 slots da
 * spell hotbar dispostos em círculo. Mover o mouse seleciona o slot, soltar
 * R casta o slot selecionado.
 *
 * <h2>Visual</h2>
 * <pre>
 *       [slot 0]
 *  [slot 7] [slot 1]
 * [slot 6]  ●  [slot 2]
 *  [slot 5] [slot 3]
 *       [slot 4]
 * </pre>
 *
 * <p>Cada slot mostra:
 * <ul>
 *   <li>Background colorido (school color do spell ou cinza se vazio)</li>
 *   <li>Texto do nome do spell (truncado)</li>
 *   <li>Mana cost no canto</li>
 *   <li>Hotkey hint (Z/X/C/...) pra slots 0-2 (mapeados nas keybinds)</li>
 * </ul>
 */
public class SpellWheelScreen extends Screen {

    private static final int WHEEL_RADIUS = 100;
    private static final int SLOT_SIZE = 56;

    /** Slot atualmente sob o cursor (0-7) ou -1 se nenhum. */
    private int hoveredSlot = -1;
    private boolean casted = false;

    public SpellWheelScreen() {
        super(Component.literal("Spell Wheel"));
    }

    /** Centro X da tela. */
    private int cx() { return this.width / 2; }
    /** Centro Y da tela. */
    private int cy() { return this.height / 2; }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        RenderSystem.enableBlend();

        int centerX = cx();
        int centerY = cy();

        // Detect hovered slot
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        hoveredSlot = -1;
        if (dist > 30 && dist < WHEEL_RADIUS + 40) {
            // angle = atan2 — 0=right, π/2=down. Convert to 0=top, clockwise.
            double angle = Math.atan2(dy, dx) + Math.PI / 2;
            if (angle < 0) angle += Math.PI * 2;
            // Discretize em 8 slots — cada slot = π/4 radians (45 deg)
            hoveredSlot = (int) Math.round(angle / (Math.PI * 2 / 8)) % 8;
        }

        // Cursor central
        g.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFFFFFFFF);

        // Render os 8 slots
        Player p = Minecraft.getInstance().player;
        if (p == null) return;

        for (int slot = 0; slot < SpellHotbarData.SLOTS; slot++) {
            // Posição: começa no topo (slot 0), sentido horário
            double angle = (slot * Math.PI * 2 / SpellHotbarData.SLOTS) - Math.PI / 2;
            int sx = centerX + (int) (Math.cos(angle) * WHEEL_RADIUS);
            int sy = centerY + (int) (Math.sin(angle) * WHEEL_RADIUS);
            renderSlot(g, slot, sx, sy, p);
        }

        // Hint texto no topo
        String hint = hoveredSlot == -1
                ? "Mova o mouse pra selecionar"
                : "Solte R pra castar slot " + (hoveredSlot + 1);
        int hintW = this.font.width(hint);
        g.drawString(this.font, hint, centerX - hintW / 2, 20, 0xFFFFFFFF, true);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderSlot(GuiGraphics g, int slot, int sx, int sy, Player p) {
        int x0 = sx - SLOT_SIZE / 2;
        int y0 = sy - SLOT_SIZE / 2;
        int x1 = sx + SLOT_SIZE / 2;
        int y1 = sy + SLOT_SIZE / 2;

        // Background — destacado se hovered
        int bgColor = slot == hoveredSlot ? 0xFF4a2a8a : 0xCC1a0a3a;
        int borderColor = slot == hoveredSlot ? 0xFFDDAAFF : 0xFF552288;
        g.fill(x0, y0, x1, y1, bgColor);
        // Border
        g.fill(x0, y0, x1, y0 + 2, borderColor);
        g.fill(x0, y1 - 2, x1, y1, borderColor);
        g.fill(x0, y0, x0 + 2, y1, borderColor);
        g.fill(x1 - 2, y0, x1, y1, borderColor);

        // Spell info
        String spellId = SpellHotbarData.getSpellId(p, slot);
        if (spellId == null) {
            // Empty slot
            String empty = "vazio";
            int w = this.font.width(empty);
            g.drawString(this.font, empty, sx - w / 2, sy - 4, 0xFF888888, false);
        } else {
            SpellDef d = SpellLibrary.get(spellId);
            if (d != null) {
                // School color bar at top
                g.fill(x0 + 4, y0 + 4, x1 - 4, y0 + 8, 0xFF000000 | d.school.colorHex());
                // Spell name (centered, truncated)
                String name = d.name;
                if (this.font.width(name) > SLOT_SIZE - 6) {
                    while (this.font.width(name + "...") > SLOT_SIZE - 6 && name.length() > 3) {
                        name = name.substring(0, name.length() - 1);
                    }
                    name = name + "...";
                }
                int nameW = this.font.width(name);
                g.drawString(this.font, name, sx - nameW / 2, sy - 4, 0xFFFFFFFF, false);
                // Mana cost (bottom-right)
                String mana = "§b" + d.manaCost;
                int manaW = this.font.width(mana);
                g.drawString(this.font, mana, x1 - manaW - 4, y1 - 10, 0xFFFFFFFF, false);
            }
        }
        // Slot number badge (top-left)
        g.drawString(this.font, String.valueOf(slot + 1), x0 + 4, y0 + 12,
                slot == hoveredSlot ? 0xFFFFFFFF : 0xFFAAAAFF, false);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Se soltou R, casta o slot selecionado
        if (br.com.murilo.liberthia.client.KeyBindings.SPELL_WHEEL_RADIAL.matches(keyCode, scanCode)) {
            if (hoveredSlot != -1 && !casted) {
                casted = true;
                // Envia packet pra castar slot (action=CAST, slot=hovered)
                ModNetwork.sendToServer(new SpellHotbarActionC2SPacket(
                        SpellHotbarActionC2SPacket.ACTION_CAST, (byte) hoveredSlot));
            }
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** Não usa Minecraft normal close (Esc) — fecha pra continuar gameplay. */
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
