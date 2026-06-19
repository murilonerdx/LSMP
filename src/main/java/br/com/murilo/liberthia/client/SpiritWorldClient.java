package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r24: client-side do Spirit World:
 *
 * <ul>
 *   <li>HUD bar de sanidade (canto inferior direito quando &lt; 100)</li>
 *   <li>Tint roxo na tela quando em spirit world (overlay sutil)</li>
 *   <li>Vignette aumenta com sanidade baixa</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class SpiritWorldClient {

    private static int currentSanity = SpiritDimension.MAX_SANITY;

    private SpiritWorldClient() {}

    public static void onSanitySync(int sanity) {
        currentSanity = sanity;
    }

    public static int getCurrentSanity() {
        return currentSanity;
    }

    /**
     * v0.1.22 r26 BUGFIX: reset sanity ao logout/login. Antes valor antigo
     * de partida anterior podia persistir entre worlds → HUD bar mostrava
     * valor errado até primeiro sync do server.
     */
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        currentSanity = SpiritDimension.MAX_SANITY;
    }

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        currentSanity = SpiritDimension.MAX_SANITY;
        // server vai enviar sync inicial via onPlayerLogin no SpiritWorldEvents
    }

    /**
     * Renderiza overlay quando em Spirit World OU quando sanidade está baixa.
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();

        boolean inSpirit = mc.player.level().dimension().equals(SpiritDimension.SPIRIT_WORLD);

        // (1) TINT roxo se em spirit world — overlay translúcido em toda tela
        if (inSpirit) {
            // Cor base + intensidade baseada na sanidade
            int alpha = 60 - (currentSanity * 30 / SpiritDimension.MAX_SANITY);
            alpha = Math.max(20, alpha);
            int color = (alpha << 24) | 0x4B0082; // indigo
            g.fill(0, 0, w, h, color);
        }

        // (2) VIGNETTE baseado em sanidade baixa — escurece bordas
        if (currentSanity < SpiritDimension.LOW_SANITY_THRESHOLD) {
            float intensity = (SpiritDimension.LOW_SANITY_THRESHOLD - currentSanity)
                    / (float) SpiritDimension.LOW_SANITY_THRESHOLD;
            int vignetteAlpha = (int) (150 * intensity);
            int vColor = (vignetteAlpha << 24) | 0x000000;
            // Borda superior + inferior + laterais (vignette simplificado)
            int borderSize = (int) (h * 0.15 * intensity);
            g.fill(0, 0, w, borderSize, vColor);
            g.fill(0, h - borderSize, w, h, vColor);
            g.fill(0, 0, borderSize, h, vColor);
            g.fill(w - borderSize, 0, w, h, vColor);
        }

        // (3) HUD BAR de sanidade — r165: SEMPRE visível (independente de estar no spirit
        // world ou ter sanidade plena). Config toggle ainda funciona.
        if (br.com.murilo.liberthia.config.LiberthiaConfig.CLIENT.sanityHudVisible.get()) {
            var hudId = br.com.murilo.liberthia.client.hud.unified.HudId.SANITY_HUD;
            // r164: compacto — barra 80×4 + label inline pequena
            int barW = 80;
            int barH = 4;
            int barX = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.x(hudId, w);
            int barY = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.y(hudId, h);
            // r166 FIX: clamp on-screen — garante que a barra NUNCA fique fora da tela
            // (corrige "HUD de sanidade sumiu" por posição salva ruim no editor de HUD).
            barX = Math.max(2, Math.min(barX, w - barW - 2));
            barY = Math.max(12, Math.min(barY, h - barH - 2));

            // Background
            g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xCC000000);
            g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A0A2A);

            // Fill — cor depende da sanidade
            int fillW = (barW * currentSanity) / SpiritDimension.MAX_SANITY;
            int fillColor;
            if (currentSanity >= 70) fillColor = 0xFF55AAFF;
            else if (currentSanity >= 30) fillColor = 0xFFAA55FF;
            else if (currentSanity >= 10) fillColor = 0xFFFF5555;
            else fillColor = 0xFFFF0000;
            if (currentSanity < 10 && (mc.player.tickCount / 5) % 2 == 0) {
                fillColor = 0xFFFFFFFF;
            }
            g.fill(barX, barY, barX + fillW, barY + barH, fillColor);

            // Label compacto inline acima
            String lbl = "§5Sanidade §f" + currentSanity + "§8/§7" + SpiritDimension.MAX_SANITY;
            g.drawString(mc.font, lbl, barX, barY - 10, 0xFFAA88FF, true);
        }
    }
}
