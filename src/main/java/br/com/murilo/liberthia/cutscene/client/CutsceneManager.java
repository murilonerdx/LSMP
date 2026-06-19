package br.com.murilo.liberthia.cutscene.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cutscene.CutsceneS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.net.URI;

/**
 * r190 — gerencia a cutscene no cliente: estado, fade, letterbox e abertura do vídeo no navegador.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CutsceneManager {
    private CutsceneManager() {}

    private static String activeUrl = "";
    private static boolean active = false;
    private static byte activeMode = 0;
    private static int ticksShowing = 0;
    private static int fadeInTicks = 0;     // 0..10
    private static int fadeOutTicks = -1;   // -1 = não saindo; 0..20

    public static int getTicksShowing() { return ticksShowing; }
    public static byte getActiveMode() { return activeMode; }

    public static void receive(CutsceneS2CPacket pkt) {
        switch (pkt.action) {
            case PLAY -> play(pkt.url, pkt.mode);
            case STOP -> { if (active && fadeOutTicks < 0) fadeOutTicks = 0; }
            case RESTART -> { if (!activeUrl.isEmpty()) play(activeUrl, activeMode == 0 ? (byte) 0x07 : activeMode); }
            case LINK -> activeUrl = pkt.url;
            case RESET -> { internalStop(); activeUrl = ""; activeMode = 0; }
        }
    }

    private static void play(String url, byte mode) {
        activeUrl = url; activeMode = mode; active = true;
        ticksShowing = 0; fadeInTicks = 0; fadeOutTicks = -1;
        if ((mode & CutsceneS2CPacket.MODE_BROWSER) != 0 && !url.isEmpty()) {
            try { Util.getPlatform().openUri(URI.create(url)); }
            catch (Exception e) { LiberthiaMod.LOGGER.warn("[Cutscene] openUri falhou: {}", e.toString()); }
        }
        if ((mode & CutsceneS2CPacket.MODE_CINEMATIC) != 0) {
            Minecraft.getInstance().setScreen(new CutsceneScreen(url, mode));
        }
    }

    public static void internalStop() {
        active = false; fadeOutTicks = -1; ticksShowing = 0; fadeInTicks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CutsceneScreen) mc.setScreen(null);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !active) return;
        ticksShowing++;
        if (fadeInTicks < 10) fadeInTicks++;
        if (fadeOutTicks >= 0) { fadeOutTicks++; if (fadeOutTicks >= 20) internalStop(); }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post e) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = e.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        // fade alpha
        float fade = fadeInTicks / 10.0f;
        if (fadeOutTicks >= 0) fade = 1.0f - (fadeOutTicks / 20.0f);
        int a = Mth.clamp((int) (fade * 255), 0, 255);
        if (a > 4) g.fill(0, 0, w, h, (a / 3) << 24); // leve escurecida geral
        // letterbox (sempre)
        int bar = h / 10;
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        if (ticksShowing >= 60)
            g.drawString(mc.font, "§7[ESC] fechar", w - 70, h - bar + (bar - 8) / 2, 0xFFFFFF, true);
    }
}
