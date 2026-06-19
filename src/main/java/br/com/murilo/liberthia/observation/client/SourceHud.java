package br.com.murilo.liberthia.observation.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem;
import br.com.murilo.liberthia.observation.source.MagicLevelData;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r71: <b>Source HUD upgrade</b> — agora exibe:
 * <ul>
 *   <li>Nome do spell ativo (com cor)</li>
 *   <li>Barra de Source com gradient + glow</li>
 *   <li>Level + XP bar (mini)</li>
 *   <li>Luck %</li>
 * </ul>
 *
 * <p>Posição configurável via keybind {@code H} → cycle entre 4 cantos + center.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class SourceHud {

    /** Position presets. Player escolhe via comando ou keybind. */
    public enum HudPosition {
        BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT, CENTER_TOP
    }

    public static HudPosition position = HudPosition.BOTTOM_LEFT;

    private SourceHud() {}

    /**
     * r142: helper — verifica se o item é "mágico" (HUD só aparece com magic items).
     * Cobre: Grimoire (todos os tiers), Spell Scrolls (Universal), Caster Wand,
     * Spell Parchment, Glyph items, Staffs, SpellBow, Imbued Sword, Source items.
     */
    private static boolean isMagicItem(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var item = stack.getItem();
        return item instanceof GrimoireOfObservationItem
            || item instanceof br.com.murilo.liberthia.observation.item.GrimoireTierItem
            || item instanceof br.com.murilo.liberthia.observation.item.PrebuiltTomeItem
            || item instanceof br.com.murilo.liberthia.observation.item.ObservationTomeItem
            || item instanceof br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem
            || item instanceof br.com.murilo.liberthia.magic.spell.wand.CasterWandItem
            || item instanceof br.com.murilo.liberthia.observation.item.SpellParchmentItem
            || item instanceof br.com.murilo.liberthia.observation.item.GlyphItem
            || item instanceof br.com.murilo.liberthia.magic.weapon.StaffItem
            || item instanceof br.com.murilo.liberthia.observation.item.SourceUpgradeItems.SourceCrystalItem
            || item instanceof br.com.murilo.liberthia.observation.item.SourceUpgradeItems.SourceCatalystItem
            || item instanceof br.com.murilo.liberthia.observation.item.SourceUpgradeItems.SourceLensItem;
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // r142: ESCONDE HUD se player não está com item mágico nas mãos.
        // Mostra só quando player segura: Grimoire, Spell Scroll, Caster Wand,
        // Staff, Spell Bow, Spell Parchment, Glyph item — qualquer item mágico.
        var mainHand = mc.player.getMainHandItem();
        var offHand = mc.player.getOffhandItem();
        if (!isMagicItem(mainHand) && !isMagicItem(offHand)) return;

        int current = SourceData.get(mc.player);
        int max = SourceData.getMax(mc.player);

        GuiGraphics g = event.getGuiGraphics();
        int W = g.guiWidth();
        int H = g.guiHeight();

        // r164: posição via Unified HUD system (drag-and-drop persistente)
        int panelW = 140;
        int panelH = 44;
        var hudId = br.com.murilo.liberthia.client.hud.unified.HudId.SOURCE_HUD;
        int x = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.x(hudId, W);
        int y = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.y(hudId, H);

        // Panel background (gradient roxo escuro)
        g.fill(x, y, x + panelW, y + panelH, 0xCC0F0820);
        g.fill(x, y, x + panelW, y + 1, 0xFFAA66FF);  // top border
        g.fill(x, y + panelH - 1, x + panelW, y + panelH, 0xFFAA66FF);  // bottom border
        g.fill(x, y, x + 1, y + panelH, 0xFFAA66FF);  // left
        g.fill(x + panelW - 1, y, x + panelW, y + panelH, 0xFFAA66FF);  // right

        // ── Spell name (top) ──
        // r142: mainHand já foi declarado acima — reusa a referência
        String spellName = "Sem feitiço";
        int spellColor = 0xFFFFFFFF;
        if (mainHand.getItem() instanceof GrimoireOfObservationItem) {
            var spell = GrimoireOfObservationItem.loadActiveSpell(mainHand);
            spellName = spell.name();
            spellColor = 0xFF000000 | spell.color();
        }
        g.drawString(mc.font, "§l" + spellName, x + 6, y + 5, spellColor, true);

        // ── Level badge (top right) ──
        int level = MagicLevelData.getLevel(mc.player);
        String lvText = "Lv " + level;
        int lvWidth = mc.font.width(lvText);
        g.fill(x + panelW - lvWidth - 12, y + 4, x + panelW - 4, y + 14, 0xFF5544AA);
        g.drawString(mc.font, "§e" + lvText, x + panelW - lvWidth - 8, y + 5, 0xFFFFEE66, true);

        // ── Source bar (r136: compactado) ──
        int barX = x + 6;
        int barY = y + 16;
        int barW = panelW - 12;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + 9, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + 8, 0xFF1A0A2A);
        int fillW = (int)((double)barW * current / Math.max(1, max));
        int fillColor;
        if (current >= max * 0.7) fillColor = 0xFF5DC1E0;
        else if (current >= max * 0.3) fillColor = 0xFF9D4DD6;
        else fillColor = 0xFFFF5577;
        g.fill(barX, barY, barX + fillW, barY + 8, fillColor);
        // Glow pulse na borda
        if (mc.player.getDeltaMovement().lengthSqr() < 0.0005) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.5 + 0.5);
            int glowAlpha = (int)(120 * pulse);
            g.fill(barX, barY, barX + fillW, barY + 1, (glowAlpha << 24) | 0xFFFFFF);
        }
        // Source text
        String srcText = current + " / " + max;
        int srcTextW = mc.font.width(srcText);
        g.drawString(mc.font, "§b" + current + "§7/§e" + max,
            barX + (barW - srcTextW) / 2, barY + 1, 0xFFFFFFFF, true);

        // ── XP bar (mini, r136 compactado) ──
        int xpBarY = y + 27;
        int xpFillW = (int)(barW * MagicLevelData.getProgress(mc.player));
        g.fill(barX - 1, xpBarY - 1, barX + barW + 1, xpBarY + 5, 0xFF000000);
        g.fill(barX, xpBarY, barX + barW, xpBarY + 4, 0xFF221133);
        g.fill(barX, xpBarY, barX + xpFillW, xpBarY + 4, 0xFFFFCC44);

        // ── Bottom row: XP raw + luck ──
        int xp = MagicLevelData.getXp(mc.player);
        int xpMax = MagicLevelData.getXpThreshold(level);
        float luck = MagicLevelData.getLuck(mc.player);
        String botRow = "§eXP §f" + xp + "§7/§f" + xpMax + "  §6Sorte §e" + (int)luck + "%";
        g.drawString(mc.font, botRow, x + 6, y + 34, 0xFFCCCCCC, true);
    }
}
