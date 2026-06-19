package br.com.murilo.liberthia.magic.spell.hotbar;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.KeyBindings;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.162 r138: <b>SpellHotbarOverlay</b> — HUD client mostrando 3 slots
 * quick-cast no canto direito da tela.
 *
 * <p>Visual:
 * <pre>
 *   ┌──────┐
 *   │ [Z]  │ ← spell name + key hint
 *   │ Fire │
 *   ├──────┤
 *   │ [B]  │
 *   │ Heal │
 *   ├──────┤
 *   │ [N]  │
 *   │ —    │ ← empty
 *   └──────┘
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpellHotbarOverlay {

    private SpellHotbarOverlay() {}

    public static final IGuiOverlay HOTBAR_OVERLAY = (gui, g, partial, sw, sh) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!ClientSpellHotbar.hasAny()) return;

        // Position: canto direito da tela, centralizado verticalmente
        int slotW = 64;
        int slotH = 24;
        int totalH = slotH * 3 + 4; // 4 = padding
        int x = sw - slotW - 4;
        int y = sh / 2 - totalH / 2;

        // Background frame
        g.fill(x - 1, y - 1, x + slotW + 1, y + totalH + 1, 0xFF000000);
        g.fill(x, y, x + slotW, y + totalH, 0xCC0A0218);
        // Gold border
        int gold = 0xFFFFEE00;
        g.fill(x, y, x + slotW, y + 1, gold);
        g.fill(x, y + totalH - 1, x + slotW, y + totalH, gold);
        g.fill(x, y, x + 1, y + totalH, gold);
        g.fill(x + slotW - 1, y, x + slotW, y + totalH, gold);

        String[] keys = {
            keyName(KeyBindings.SPELL_HOTBAR_1),
            keyName(KeyBindings.SPELL_HOTBAR_2),
            keyName(KeyBindings.SPELL_HOTBAR_3)
        };

        for (int i = 0; i < 3; i++) {
            int sy = y + i * slotH + (i > 0 ? 2 : 0);
            // Slot row background
            g.fill(x + 2, sy + 2, x + slotW - 2, sy + slotH - 2, 0xAA1A0830);
            // Key label
            String keyLabel = "[" + keys[i] + "]";
            g.drawString(mc.font, keyLabel, x + 4, sy + 4, 0xFFFFEE00, false);

            // Spell name
            String spellId = ClientSpellHotbar.get(i);
            if (spellId == null) {
                g.drawString(mc.font, "§7—", x + 24, sy + 4, 0xFFAAAAAA, false);
            } else {
                SpellDef d = SpellLibrary.get(spellId);
                String name = d == null ? spellId : d.displayName().getString();
                if (name.length() > 10) name = name.substring(0, 10) + "…";
                int color = d == null ? 0xFFFFFFFF : d.school.colorHex() | 0xFF000000;
                g.drawString(mc.font, name, x + 24, sy + 4, color, false);
                // Cost hint
                if (d != null) {
                    g.drawString(mc.font, "§8" + d.manaCost,
                            x + 24, sy + 14, 0xFFAAAAAA, false);
                }
            }
        }
    };

    private static String keyName(net.minecraft.client.KeyMapping km) {
        try {
            return km.getKey().getDisplayName().getString();
        } catch (Throwable t) {
            return "?";
        }
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // r164: DESATIVADO — o sistema de bind Z/B/N foi removido a pedido do user.
        // O grimoire agora usa o GrimoireWheelScreen (X) pra selecionar slot,
        // e cast vem direto do right-click. Sem hotbar HUD = sem clutter.
        // event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "spell_hotbar", HOTBAR_OVERLAY);
    }
}
