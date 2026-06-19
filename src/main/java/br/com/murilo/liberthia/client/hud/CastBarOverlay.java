package br.com.murilo.liberthia.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * v0.1.24 r102: <b>Cast Bar overlay</b> — barra de progresso visível quando o
 * player está castando spell.
 *
 * <p>Estado público (set por código de cast): {@link #castStartTick},
 * {@link #castDurationTicks}, {@link #castSpellName}.
 *
 * <p>Quando o tick atual está entre [start, start+duration], desenha barra
 * gradient + nome do spell. Após terminar, esconde.
 */
public class CastBarOverlay implements IGuiOverlay {

    /** Tick em que cast começou — 0 = inativo. */
    public static long castStartTick = 0;
    public static int castDurationTicks = 0;
    public static String castSpellName = "";
    public static int castColor = 0xFF66CCFF;

    public static void startCast(String spellName, int durationTicks, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        castStartTick = mc.level.getGameTime();
        castDurationTicks = durationTicks;
        castSpellName = spellName;
        castColor = color;
    }

    public static void cancelCast() {
        castStartTick = 0;
    }

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics gfx,
                        float partialTick, int screenWidth, int screenHeight) {
        if (castStartTick == 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = mc.level.getGameTime();
        long elapsed = now - castStartTick;
        if (elapsed >= castDurationTicks) {
            castStartTick = 0;
            return;
        }

        float progress = elapsed / (float) castDurationTicks;
        int barWidth = 160;
        int barHeight = 8;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight - 60;

        // Background
        gfx.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
        gfx.fill(x, y, x + barWidth, y + barHeight, 0xFF222222);
        // Fill
        int filled = (int) (barWidth * progress);
        gfx.fill(x, y, x + filled, y + barHeight, castColor);
        // Spell name
        gfx.drawString(mc.font, castSpellName, x, y - 10, 0xFFFFFFFF);
    }
}
