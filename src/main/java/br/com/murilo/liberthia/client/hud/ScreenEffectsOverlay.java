package br.com.murilo.liberthia.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * v0.1.24 r102: <b>Screen Effects Edge</b> — tinted edge da tela quando
 * jogador está em estado especial:
 * <ul>
 *   <li>Burning (fire ticks > 0) — orange tint</li>
 *   <li>Frozen (ticks frozen > 0) — blue tint</li>
 *   <li>Low health (HP &lt; 30%) — red pulse</li>
 *   <li>High cosmic exposure — purple vignette</li>
 * </ul>
 *
 * <p>Desenha rectangles transparentes nas bordas (top/bottom/left/right).
 */
public class ScreenEffectsOverlay implements IGuiOverlay {

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics gfx,
                        float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;

        // (1) Burning
        if (p.getRemainingFireTicks() > 0) {
            drawEdgeTint(gfx, screenWidth, screenHeight, 0x33FF6633);
        }

        // (2) Frozen
        if (p.getTicksFrozen() > 100) {
            int intensity = Math.min(0x66, p.getTicksFrozen() / 5);
            int color = (intensity << 24) | 0x66CCFF;
            drawEdgeTint(gfx, screenWidth, screenHeight, color);
        }

        // (3) Low HP pulse
        if (p.getHealth() < p.getMaxHealth() * 0.3F) {
            long t = mc.level != null ? mc.level.getGameTime() : 0;
            int pulse = (int) (0x44 + Math.sin(t * 0.2) * 0x33);
            pulse = Math.max(0x22, Math.min(0x77, pulse));
            int color = (pulse << 24) | 0xCC0033;
            drawEdgeTint(gfx, screenWidth, screenHeight, color);
        }

        // (4) Cosmic exposure (read horror NBT if available)
        try {
            if (br.com.murilo.liberthia.cosmic.framework.HorrorFramework.activeStates() > 0
                    && p instanceof net.minecraft.client.player.LocalPlayer) {
                // Client doesn't have state — use sanity NBT proxy
                int sanity = br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(p);
                if (sanity < 30) {
                    int intensity = (30 - sanity) * 4;
                    int color = (intensity << 24) | 0x6633CC;
                    drawEdgeTint(gfx, screenWidth, screenHeight, color);
                }
            }
        } catch (Throwable ignored) {}

        // (5) r180: compulsão da Matéria Escura — vinheta VERMELHA pulsante (≥55% dark, sem Âncora Mental)
        try {
            float dark = br.com.murilo.liberthia.matter.ClientMatterProfileCache.dark();
            if (dark >= 55.0F && !hasMindAnchor(p)) {
                long t = mc.level != null ? mc.level.getGameTime() : 0;
                int base = (int) Math.max(20, Math.min(110, (dark - 55) * 3));
                int pulse = (int) (base + Math.sin(t * 0.25) * 30);
                pulse = Math.max(0x18, Math.min(0xBB, pulse));
                int color = (pulse << 24) | 0xAA0000;
                drawEdgeTint(gfx, screenWidth, screenHeight, color);
            }
        } catch (Throwable ignored) {}
    }

    private boolean hasMindAnchor(Player p) {
        try {
            return p.getInventory().contains(new net.minecraft.world.item.ItemStack(
                    br.com.murilo.liberthia.registry.ModItems.MIND_ANCHOR.get()));
        } catch (Throwable t) {
            return false;
        }
    }

    private void drawEdgeTint(GuiGraphics gfx, int w, int h, int color) {
        // Top
        gfx.fillGradient(0, 0, w, 30, color, color & 0x00FFFFFF);
        // Bottom
        gfx.fillGradient(0, h - 30, w, h, color & 0x00FFFFFF, color);
        // Left
        gfx.fillGradient(0, 0, 30, h, color, color & 0x00FFFFFF);
        // Right
        gfx.fillGradient(w - 30, 0, w, h, color & 0x00FFFFFF, color);
    }
}
