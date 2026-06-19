package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.idol.PhotoStore;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r186 — client-only. No FIM de cada frame, se há uma foto pendente (pedida pela câmera com a
 * HUD escondida), lê o framebuffer já SEM HUD/mãos (estilo F1) e salva em alta resolução.
 * Ver {@link PhotoStore#capture} / {@link PhotoStore#onRenderTickEnd}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PhotoCaptureClientHandler {
    private PhotoCaptureClientHandler() {}

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase == TickEvent.Phase.END) PhotoStore.onRenderTickEnd();
    }
}
