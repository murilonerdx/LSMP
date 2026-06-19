package br.com.murilo.liberthia.magic.grimoire.client;

import br.com.murilo.liberthia.magic.factory.DynamicSpellItem;
import br.com.murilo.liberthia.magic.grimoire.GrimoireBookItem;
import br.com.murilo.liberthia.magic.grimoire.GrimoireInventory;
import br.com.murilo.liberthia.magic.grimoire.SelectGrimoireSlotC2SPacket;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * r164: <b>GrimoireWheelScreen</b> — radial menu mostrando os 9 slots de scroll
 * do grimoire que o player segura. Clicar (ou soltar X em cima) seleciona o
 * slot ativo. Depois é só right-click no mundo com o grimoire pra castar.
 *
 * <p>Substitui o antigo "bind Z/B/N" — agora é seleção visual única no wheel
 * + cast no right-click. Simples.
 */
@OnlyIn(Dist.CLIENT)
public class GrimoireWheelScreen extends Screen {

    private static final int RING_RADIUS = 90;
    private static final int ICON_RADIUS = 22;

    private final ItemStack grimoire;
    private final boolean isMainHand;
    private final GrimoireInventory inv;

    private int hoveredIndex = -1;

    public GrimoireWheelScreen(ItemStack grimoire, boolean isMainHand) {
        super(Component.literal("Grimoire Wheel"));
        this.grimoire = grimoire;
        this.isMainHand = isMainHand;
        this.inv = GrimoireInventory.from(grimoire);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0xCC000000, 0xEE000000);

        int cx = width / 2;
        int cy = height / 2;

        // Detect hovered slice
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        hoveredIndex = -1;
        if (dist > 28 && dist < RING_RADIUS + ICON_RADIUS + 12) {
            double angle = Math.atan2(dy, dx);
            // Normalize to 0..2π com 0 no topo
            double normalized = (angle + Math.PI * 2.5) % (Math.PI * 2);
            int slice = (int)(normalized / (Math.PI * 2 / GrimoireBookItem.SCROLL_SLOTS));
            if (slice >= 0 && slice < GrimoireBookItem.SCROLL_SLOTS) {
                hoveredIndex = slice;
            }
        }

        // Central glow
        int centerR = 30;
        g.fill(cx - centerR, cy - centerR, cx + centerR, cy + centerR, 0xAA1A0830);
        for (int r = centerR; r > 0; r -= 2) {
            int alpha = (int)(((float) r / centerR) * 40);
            int color = (alpha << 24) | 0x553388;
            g.fill(cx - r, cy - r, cx + r, cy + r, color);
        }

        int activeSlot = GrimoireBookItem.getActiveSlot(grimoire);

        // Draw 9 slot slices
        for (int i = 0; i < GrimoireBookItem.SCROLL_SLOTS; i++) {
            double a = -Math.PI / 2 + (i / (double) GrimoireBookItem.SCROLL_SLOTS) * Math.PI * 2;
            int sx = cx + (int)(Math.cos(a) * RING_RADIUS);
            int sy = cy + (int)(Math.sin(a) * RING_RADIUS);
            drawSlotSlice(g, i, sx, sy, i == hoveredIndex, i == activeSlot);
        }

        // Center info — hovered or active
        int focusIdx = hoveredIndex >= 0 ? hoveredIndex : activeSlot;
        drawCenterInfo(g, cx, cy, focusIdx);

        // Hint
        String hint = "§7Click ou solte X = §6SELECIONAR§7 | §dRight-click do Grimoire§7 = CASTAR | §8ESC fecha";
        int hw = font.width(hint);
        g.drawString(font, hint, cx - hw / 2, height - 20, 0xFFCCCCCC, true);

        // Top header
        String header = "§l§dGrimoire — §rSelecione o feitiço";
        int hh = font.width("Grimoire — Selecione o feitiço");
        g.drawString(font, header, cx - hh / 2, 20, 0xFFFFFFFF, true);
    }

    private void drawSlotSlice(GuiGraphics g, int slotIdx, int cx, int cy,
                               boolean hovered, boolean active) {
        ItemStack scroll = inv.getStackInSlot(slotIdx);
        int r = hovered ? ICON_RADIUS + 6 : ICON_RADIUS;
        boolean empty = scroll.isEmpty();

        // BG circle
        int color;
        if (empty)        color = 0x55555555;
        else if (active)  color = 0xFFFFCC55;   // gold ring for active
        else if (hovered) color = 0xFFAA66FF;
        else              color = 0xFF552288;
        int alpha = hovered ? 0xDD : (active ? 0xCC : 0x99);
        int fillColor = (alpha << 24) | (color & 0x00FFFFFF);
        g.fill(cx - r, cy - r, cx + r, cy + r, fillColor);

        // Border
        int borderColor = active ? 0xFFFFEE66 : (hovered ? 0xFFFFFFFF : 0xFF000000);
        g.fill(cx - r, cy - r, cx + r, cy - r + 1, borderColor);
        g.fill(cx - r, cy + r - 1, cx + r, cy + r, borderColor);
        g.fill(cx - r, cy - r, cx - r + 1, cy + r, borderColor);
        g.fill(cx + r - 1, cy - r, cx + r, cy + r, borderColor);

        // Slot number (top-left of icon)
        String num = String.valueOf(slotIdx + 1);
        g.drawString(font, "§e" + num, cx - r + 3, cy - r + 3, 0xFFFFFFFF, true);

        // Icon (renderiza o item se tiver scroll)
        if (!empty) {
            int iconSz = 16;
            g.renderItem(scroll, cx - iconSz / 2, cy - iconSz / 2);
        } else {
            String x = "§8∅";
            int xw = font.width("∅");
            g.drawString(font, x, cx - xw / 2, cy - 4, 0xFF888888, true);
        }

        // Spell name below
        String name = empty ? "§8(vazio)" : scroll.getHoverName().getString();
        if (font.width(name) > r * 3) {
            name = font.plainSubstrByWidth(name, r * 3 - 4) + "..";
        }
        int nw = font.width(name.replaceAll("§.", ""));
        g.drawString(font, name, cx - nw / 2, cy + r + 4,
                hovered ? 0xFFFFFFFF : 0xFFCCCCCC, true);
    }

    private void drawCenterInfo(GuiGraphics g, int cx, int cy, int slotIdx) {
        if (slotIdx < 0 || slotIdx >= GrimoireBookItem.SCROLL_SLOTS) return;
        ItemStack scroll = inv.getStackInSlot(slotIdx);
        if (scroll.isEmpty()) {
            String t = "§7Slot " + (slotIdx + 1) + ": §8vazio";
            int w = font.width("Slot " + (slotIdx + 1) + ": vazio");
            g.drawString(font, t, cx - w / 2, cy - 4, 0xFFFFFFFF, true);
            return;
        }

        // Resolve SpellDef for stats
        SpellDef d = null;
        if (scroll.getItem() instanceof UniversalSpellScrollItem usi) {
            d = usi.def(scroll);
        } else if (scroll.getItem() instanceof DynamicSpellItem) {
            String id = DynamicSpellItem.spellId(scroll);
            if (id != null) d = SpellLibrary.get(id);
        }

        String name = scroll.getHoverName().getString();
        int nw = font.width(name);
        g.drawString(font, "§l" + name, cx - nw / 2, cy - 18, 0xFFFFFFFF, true);

        if (d != null) {
            String stats = "§b" + d.manaCost + "§7m · §c" + (int) d.damage + "§7dmg · §6cd " + (d.cooldownTicks / 20) + "s";
            String plain = d.manaCost + "m · " + (int) d.damage + "dmg · cd " + (d.cooldownTicks / 20) + "s";
            int sw = font.width(plain);
            g.drawString(font, stats, cx - sw / 2, cy - 4, 0xFFFFFFFF, true);

            String school = "§7" + (d.school != null ? d.school.toString() : "");
            int schw = font.width(school.replaceAll("§.", ""));
            g.drawString(font, school, cx - schw / 2, cy + 8, 0xFFAA66FF, true);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // Solta X = seleciona hovered (mesmo padrão do SpellWheelScreen antigo)
        if (keyCode == GLFW.GLFW_KEY_X) {
            selectHovered();
            this.onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        // Atalhos 1-9 pra selecionar direto
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int idx = keyCode - GLFW.GLFW_KEY_1;
            selectSlot(idx);
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (hoveredIndex >= 0) {
            selectSlot(hoveredIndex);
            this.onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void selectHovered() {
        if (hoveredIndex >= 0) selectSlot(hoveredIndex);
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot >= GrimoireBookItem.SCROLL_SLOTS) return;
        // Update local copy IMEDIATAMENTE pra UX responsivo
        GrimoireBookItem.setActiveSlot(grimoire, slot);
        // Sync server
        ModNetwork.sendToServer(new SelectGrimoireSlotC2SPacket((byte) slot, isMainHand));

        Player p = Minecraft.getInstance().player;
        if (p != null) {
            ItemStack scroll = inv.getStackInSlot(slot);
            String name = scroll.isEmpty() ? "§8vazio" : scroll.getHoverName().getString();
            p.displayClientMessage(
                    Component.literal("§6✓ Slot " + (slot + 1) + " §7→ §r" + name), true);
        }
    }

    /** Open helper — chamado pelo handler de tecla X. */
    public static void openFromHeldGrimoire() {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;

        // Procura grimoire em main → off
        InteractionHand hand = null;
        if (p.getMainHandItem().getItem() instanceof GrimoireBookItem) {
            hand = InteractionHand.MAIN_HAND;
        } else if (p.getOffhandItem().getItem() instanceof GrimoireBookItem) {
            hand = InteractionHand.OFF_HAND;
        }
        if (hand == null) return;

        ItemStack grim = p.getItemInHand(hand);
        mc.setScreen(new GrimoireWheelScreen(grim, hand == InteractionHand.MAIN_HAND));
    }
}
