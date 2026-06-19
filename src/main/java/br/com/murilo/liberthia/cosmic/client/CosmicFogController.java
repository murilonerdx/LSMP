package br.com.murilo.liberthia.cosmic.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicClientState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r35: <b>Fog Controller</b> — overrides fog density / color baseado
 * em {@link CosmicClientState#currentFogIntensity}.
 *
 * <p>Hooks em {@link ViewportEvent.RenderFog} + {@link ViewportEvent.ComputeFogColor}.
 *
 * <ul>
 *   <li>Phase 1: fog 10% mais denso, cor levemente roxa</li>
 *   <li>Phase 2: fog 40% mais denso, cor roxa óbvia</li>
 *   <li>Phase 3: fog 60% mais denso, cor vermelho-roxo pulsando</li>
 *   <li>Phase 4: fog DENSO 80%, cor predominante roxa</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class CosmicFogController {

    private CosmicFogController() {}

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float intensity = CosmicClientState.currentFogIntensity;
        if (intensity < 0.01F) return;
        // Diminui far distance pra dar sensação de fog denso
        float currentFar = event.getFarPlaneDistance();
        float newFar = currentFar * (1.0F - intensity * 0.6F);
        float currentNear = event.getNearPlaneDistance();
        float newNear = currentNear * (1.0F - intensity * 0.3F);
        event.setFarPlaneDistance(Math.max(8.0F, newFar));
        event.setNearPlaneDistance(Math.max(0.0F, newNear));
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float intensity = CosmicClientState.currentFogIntensity;
        if (intensity < 0.01F) return;
        // Mistura cor original com roxo cósmico
        float pulse = (float) (0.6 + Math.sin(CosmicClientState.animTime * 0.5) * 0.4);
        float r = event.getRed() * (1.0F - intensity) + 0.30F * intensity * pulse;
        float g = event.getGreen() * (1.0F - intensity) + 0.05F * intensity;
        float b = event.getBlue() * (1.0F - intensity) + 0.55F * intensity * pulse;
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }
}
