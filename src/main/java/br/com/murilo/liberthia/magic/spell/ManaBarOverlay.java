package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.source.MagicLevelData;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.145 r112 / r164 redesign: <b>Mana Bar Overlay</b> — barra horizontal
 * estilo Iron's Spells / WoW, exibida acima da hotbar.
 *
 * <p><b>r164:</b> SEMPRE visível (não depende de segurar item mágico). Mostra:
 * <ul>
 *   <li>Barra de Mana com gradient (ciano→roxo→vermelho)</li>
 *   <li>Texto §6cur§7/§emax§r centralizado</li>
 *   <li>Badge §eLv N§r à esquerda</li>
 *   <li>Indicador de gasto/ganho: pop animado §c-X§r ou §a+X§r ao lado quando
 *       o mana muda (fade em ~1.5s)</li>
 *   <li>Pulse glow quando player está parado (regen ativo)</li>
 * </ul>
 *
 * <p>O painel detalhado (SourceHud em BOTTOM_LEFT) continua aparecendo só
 * com items mágicos pra evitar clutter — esta barra é compacta e onipresente.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ManaBarOverlay {

    /** Último valor de mana conhecido (client-side) — pra detectar delta. */
    private static int lastKnownMana = -1;
    /** Delta acumulado (positivo = ganhou, negativo = gastou) sendo exibido. */
    private static int deltaShown = 0;
    /** Tick (ms) em que o delta começou a animar — pra fade. */
    private static long deltaStartMs = 0L;
    /** Duração do pop animation em ms. */
    private static final long DELTA_DURATION_MS = 1500L;

    private ManaBarOverlay() {}

    public static final IGuiOverlay MANA_BAR = (gui, g, partial, sw, sh) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;

        int cur = SourceData.get(p);
        int max = SourceData.getMax(p);
        if (max <= 0) return;

        // ── Track delta (mana spending / gaining) ──
        if (lastKnownMana == -1) {
            lastKnownMana = cur;
        } else if (cur != lastKnownMana) {
            int diff = cur - lastKnownMana;
            // Acumula se já tinha um delta ativo da mesma direção, senão substitui
            if (deltaShown != 0 && Integer.signum(diff) == Integer.signum(deltaShown)
                    && System.currentTimeMillis() - deltaStartMs < 400L) {
                deltaShown += diff;
            } else {
                deltaShown = diff;
            }
            deltaStartMs = System.currentTimeMillis();
            lastKnownMana = cur;
        }

        // ── Layout ──
        // r164: posição via Unified HUD system (drag-and-drop persistente)
        int barW = 120;
        int barH = 6;
        int badgeW = 30;
        int gap = 4;
        var hudId = br.com.murilo.liberthia.client.hud.unified.HudId.MANA_BAR;
        int x = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.x(hudId, sw);
        int y = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.y(hudId, sh);
        int barX = x + badgeW + gap;
        int barY = y;

        // ── Level badge (left) ──
        int level = MagicLevelData.getLevel(p);
        String lvText = "Lv " + level;
        int lvTw = mc.font.width(lvText);
        // background do badge
        g.fill(x, y - 2, x + badgeW, y + barH + 2, 0xCC2A1A4A);
        g.fill(x, y - 2, x + badgeW, y - 1, 0xFFAA66FF);
        g.fill(x, y + barH + 1, x + badgeW, y + barH + 2, 0xFFAA66FF);
        g.fill(x, y - 2, x + 1, y + barH + 2, 0xFFAA66FF);
        g.fill(x + badgeW - 1, y - 2, x + badgeW, y + barH + 2, 0xFFAA66FF);
        g.drawString(mc.font, "§e" + lvText, x + (badgeW - lvTw) / 2, y - 1,
                0xFFFFEE66, true);

        // ── Mana bar background ──
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A0A2A);

        // ── Mana bar fill ──
        int fillW = (int)((double)barW * cur / max);
        int color;
        if (cur >= max * 0.7) color = 0xFF5DC1E0;       // ciano (alto)
        else if (cur >= max * 0.3) color = 0xFF9D4DD6;  // roxo (médio)
        else color = 0xFFFF5577;                          // vermelho (baixo)
        g.fill(barX, barY, barX + fillW, barY + barH, color);

        // Pixel highlight no topo (brilho)
        g.fill(barX, barY, barX + fillW, barY + 1,
                (color & 0x00FFFFFF) | 0x80FFFFFF);

        // Pulse glow quando player parado (observação ativa = regen)
        if (p.getDeltaMovement().lengthSqr() < 0.0005) {
            float pulse = (float)(Math.sin(System.currentTimeMillis() / 250.0) * 0.5 + 0.5);
            int glowAlpha = (int)(110 * pulse);
            g.fill(barX, barY, barX + fillW, barY + 1,
                    (glowAlpha << 24) | 0xFFFFFF);
        }

        // ── Mana number cur/max (centralizado na barra) ──
        String label = "§b" + cur + "§7/§e" + max;
        String plain = cur + "/" + max;
        int tw = mc.font.width(plain);
        g.drawString(mc.font, label,
                barX + (barW - tw) / 2, barY - 9,
                0xFFFFFFFF, true);

        // ── Delta indicator (spend/gain pop) ──
        if (deltaShown != 0) {
            long elapsed = System.currentTimeMillis() - deltaStartMs;
            if (elapsed >= DELTA_DURATION_MS) {
                deltaShown = 0;
            } else {
                float progress = elapsed / (float) DELTA_DURATION_MS;
                int alpha = (int)((1.0F - progress) * 255);
                if (alpha < 0) alpha = 0;
                if (alpha > 255) alpha = 255;
                // Sobe um pouco enquanto fade
                int yOffset = (int)(progress * -10);

                String deltaText = (deltaShown > 0 ? "§a+" : "§c") + deltaShown;
                String deltaPlain = (deltaShown > 0 ? "+" : "") + deltaShown;
                int dw = mc.font.width(deltaPlain);
                int dx = barX + barW + 6;
                int dy = barY - 2 + yOffset;
                int textColor = (alpha << 24) | (deltaShown > 0 ? 0x55FF55 : 0xFF5555);
                g.drawString(mc.font, deltaText, dx, dy, textColor, true);
            }
        }
    };

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // r164: REATIVADO — barra sempre visível pra mostrar mana e gasto.
        // Conflito antigo (r136) com SourceHud foi resolvido: SourceHud só
        // aparece com magic items, esta barra é compacta acima da hotbar.
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "mana_bar", MANA_BAR);
    }
}
