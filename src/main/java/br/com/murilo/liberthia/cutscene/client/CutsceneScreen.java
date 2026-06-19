package br.com.murilo.liberthia.cutscene.client;

import br.com.murilo.liberthia.cutscene.CutsceneS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * r190 — tela cinematográfica fullscreen exibida durante a cutscene. O jogo continua rodando
 * (isPauseScreen=false) para o pacote de STOP poder chegar.
 */
public class CutsceneScreen extends Screen {
    private final String url;
    private final byte mode;

    public CutsceneScreen(String url, byte mode) {
        super(Component.literal("Cutscene"));
        this.url = url; this.mode = mode;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF000000);
        int bar = (int) (height * 0.12);
        g.fill(0, 0, width, bar, 0xFF000000);
        g.fill(0, height - bar, width, height, 0xFF000000);

        int cy = height / 2;
        String dots = ".".repeat((int) ((System.currentTimeMillis() / 500) % 4));
        if ((mode & CutsceneS2CPacket.MODE_BROWSER) != 0) {
            String shown = url.length() > 55 ? url.substring(0, 55) + "…" : url;
            g.drawCenteredString(this.font, "§f§lAssistindo" + dots, width / 2, cy - 20, 0xFFFFFF);
            g.drawCenteredString(this.font, "§a" + shown, width / 2, cy - 4, 0xFFFFFF);
            g.drawCenteredString(this.font, "§7O vídeo está tocando no seu navegador.", width / 2, cy + 12, 0xFFAAAAAA);
        } else {
            g.drawCenteredString(this.font, "§f§l[ CUTSCENE ]" + dots, width / 2, cy - 6, 0xFFFFFF);
        }
        if (CutsceneManager.getTicksShowing() >= 60)
            g.drawCenteredString(this.font, "§8[ESC] para fechar", width / 2, height - bar + 4, 0xFF888888);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return (mode & CutsceneS2CPacket.MODE_LOCK_ESC) == 0 || CutsceneManager.getTicksShowing() >= 60;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() {
        CutsceneManager.internalStop();
        super.onClose();
    }
}
