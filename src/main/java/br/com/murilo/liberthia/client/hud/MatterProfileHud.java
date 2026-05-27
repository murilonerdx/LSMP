package br.com.murilo.liberthia.client.hud;

import br.com.murilo.liberthia.matter.ClientMatterProfileCache;
import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * HUD canto superior direito — 3 barrinhas verticais finas (DM/WM/YM) +
 * label do perfil ativo embaixo.
 */
public class MatterProfileHud implements IGuiOverlay {

    public static final MatterProfileHud INSTANCE = new MatterProfileHud();

    private static final int BAR_W = 4;
    private static final int BAR_H = 60;
    private static final int GAP = 2;
    private static final int MARGIN = 6;
    private static final int VERTICAL_OFFSET = 30; // pra não colidir com hot bar/effect bar

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui,
                       GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        float dm = ClientMatterProfileCache.dark();
        float wm = ClientMatterProfileCache.white();
        float ym = ClientMatterProfileCache.yellow();
        // Não desenha se tudo zerado (não polui a tela)
        if (dm <= 0.5f && wm <= 0.5f && ym <= 0.5f) return;

        // r164: posição via Unified HUD system (drag-and-drop persistente)
        var hudId = br.com.murilo.liberthia.client.hud.unified.HudId.MATTER_PROFILE;
        int totalW = BAR_W * 3 + GAP * 2;
        int x0 = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.x(hudId, screenWidth);
        int y0 = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.y(hudId, screenHeight);

        drawBar(g, x0,                          y0, dm, 0xFF8B40D8, 0xFF1A0830);
        drawBar(g, x0 + (BAR_W + GAP),          y0, wm, 0xFFE6E6FF, 0xFF202030);
        drawBar(g, x0 + (BAR_W + GAP) * 2,      y0, ym, 0xFFFFD23F, 0xFF302000);

        // Label do perfil ativo (abaixo das barras), alinhado à esquerda das barras
        MatterProfileType type = ClientMatterProfileCache.activeType();
        if (type != MatterProfileType.NONE) {
            String label = labelFor(type);
            int color = colorFor(type);
            g.drawString(mc.font, Component.literal(label),
                    x0, y0 + BAR_H + 2, color, true);
        }
    }

    private static void drawBar(GuiGraphics g, int x, int y, float value, int color, int bgColor) {
        // Frame
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xFF000000);
        // Background
        g.fill(x, y, x + BAR_W, y + BAR_H, bgColor);
        // Fill (de baixo pra cima)
        int fill = (int) (BAR_H * Math.min(1f, value / MatterProfile.MAX));
        if (fill > 0) {
            g.fill(x, y + BAR_H - fill, x + BAR_W, y + BAR_H, color);
            // glow no topo
            g.fill(x, y + BAR_H - fill, x + BAR_W, y + BAR_H - fill + 1, 0xFFFFFFFF);
        }
        // Marcações de threshold
        for (int t : new int[]{(int)(BAR_H * 0.15f), (int)(BAR_H * 0.20f), (int)(BAR_H * 0.30f)}) {
            g.fill(x - 2, y + BAR_H - t, x, y + BAR_H - t + 1, 0xFF606060);
        }
    }

    private static String labelFor(MatterProfileType t) {
        return switch (t) {
            case DARK         -> "Selvagem";
            case DARK_WHITE   -> "Contido";
            case WHITE        -> "Esquecido";
            case YELLOW       -> "Caótico";
            case YELLOW_WHITE -> "Estrategista";
            default           -> "";
        };
    }

    private static int colorFor(MatterProfileType t) {
        return switch (t) {
            case DARK         -> 0xFF8B40D8;
            case DARK_WHITE   -> 0xFFB088E0;
            case WHITE        -> 0xFFE6E6FF;
            case YELLOW       -> 0xFFFFD23F;
            case YELLOW_WHITE -> 0xFFFFEC8B;
            default           -> 0xFFAAAAAA;
        };
    }
}
