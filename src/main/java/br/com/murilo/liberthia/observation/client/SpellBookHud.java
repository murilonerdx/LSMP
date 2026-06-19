package br.com.murilo.liberthia.observation.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem;
import br.com.murilo.liberthia.observation.item.ObservationTomeItem;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r66: <b>Spell Book HUD</b> — overlay no canto inferior esquerdo.
 *
 * <p>Mostra: selected glyph icon + spell name + source bar (cost indicator).
 * Só aparece quando o player está segurando um Grimório.
 *
 * <h2>Layout</h2>
 * <pre>
 *  [glyph 24x24]  Spell Name
 *                 ━━━━━━━━━━━━  Source: 35/100
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class SpellBookHud {

    private static final ResourceLocation SLOT =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/glyph_slot_selected.png");
    private static final ResourceLocation BAR_BG =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/source_bar_bg.png");
    private static final ResourceLocation BAR_FILL =
        new ResourceLocation(LiberthiaMod.MODID, "textures/gui/source_bar_fill.png");

    /** Mapeia preset → glyph texture path (1ª manifestation do preset). */
    private static final String[] PRESET_GLYPHS = {
        "manifest_tendril",   // 0 = Tendril
        "manifest_silence",   // 1 = Silence
        "manifest_mirror",    // 2 = Mirror
        "manifest_decay",     // 3 = Decay/Whisper variant
        "manifest_whisper",   // 4 = Whisper Spread
        "manifest_tendril",   // 5 = Memory Tendril
    };

    private SpellBookHud() {}

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        // r110 FIX: HUD desabilitado — redundante com SourceHud (r71) que já mostra
        // spell name + source bar + Lv + XP + Sorte no mesmo canto. User reportou
        // overlap visual ("alguns huds estão um em cima do outro").
        // Pra reativar: descomentar abaixo + mover position pra TOP_LEFT ou
        // configurar position diferente do SourceHud.
        if (true) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;

        // Only show if holding Grimoire
        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        ItemStack grimoire = null;
        if (main.getItem() instanceof GrimoireOfObservationItem) grimoire = main;
        else if (off.getItem() instanceof GrimoireOfObservationItem) grimoire = off;
        if (grimoire == null) return;

        int preset = grimoire.getOrCreateTag().getInt(GrimoireOfObservationItem.NBT_PRESET);
        ObservationSpell spell = ObservationTomeItem.buildPreset(preset);
        int sourceCur = SourceData.get(p);
        int sourceMax = SourceData.getMax(p);
        int cost = spell.totalSourceCost();

        GuiGraphics g = event.getGuiGraphics();
        int h = g.guiHeight();
        // Position: bottom-left, above hotbar
        int x = 10;
        int y = h - 70;

        // Cosmic border around HUD box
        long t = System.currentTimeMillis();
        int box_w = 130, box_h = 32;
        // Box bg gradient
        g.fillGradient(x, y, x + box_w, y + box_h, 0xDD150528, 0xDD0A0A1A);
        // Border animated
        int alpha = (int)(100 + 80 * Math.sin(t / 400.0));
        int borderColor = (Math.max(60, alpha) << 24) | 0x9D4DD6;
        g.fill(x - 1, y - 1, x + box_w + 1, y, borderColor);
        g.fill(x - 1, y + box_h, x + box_w + 1, y + box_h + 1, borderColor);
        g.fill(x - 1, y - 1, x, y + box_h + 1, borderColor);
        g.fill(x + box_w, y - 1, x + box_w + 1, y + box_h + 1, borderColor);

        // Glyph slot (24x24)
        g.blit(SLOT, x + 3, y + 3, 0, 0, 24, 24, 24, 24);
        // Glyph icon
        ResourceLocation glyphTex = new ResourceLocation(LiberthiaMod.MODID,
            "textures/observation/glyph/" + PRESET_GLYPHS[Math.min(5, Math.max(0, preset))] + ".png");
        g.blit(glyphTex, x + 7, y + 7, 0, 0, 16, 16, 16, 16);

        // Spell name
        g.drawString(mc.font, "§l" + spell.name(), x + 32, y + 4, 0xFFFFFFFF, true);

        // Source bar
        int barW = 90;
        int barX = x + 32;
        int barY = y + 16;
        g.blit(BAR_BG, barX, barY, 0, 0, barW, 8, 100, 8);
        int fillW = (int)((barW - 2) * Math.min(1.0, (double) sourceCur / sourceMax));
        if (fillW > 0) {
            g.blit(BAR_FILL, barX + 1, barY + 1, 0, 0, fillW, 6, 100, 8);
        }
        // Cost mark
        if (cost > 0 && cost <= sourceMax) {
            int cm = barX + 1 + (int)((barW - 2) * ((double) cost / sourceMax));
            int markColor = sourceCur >= cost ? 0xFF55FF55 : 0xFFFF5555;
            g.fill(cm, barY - 1, cm + 1, barY + 9, markColor);
        }

        // Cost label below
        String costLabel = "§c" + cost + " §7/ §e" + sourceCur;
        g.drawString(mc.font, costLabel, x + 32, y + 26, 0xFFFFFFFF, true);
    }
}
