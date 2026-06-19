package br.com.murilo.liberthia.client.fog;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Neblina estilo filme de terror: quando a câmera entra numa zona, o ALCANCE
 * DE VISÃO fecha (você não vê nada além de poucos blocos) e o ar fica da cor da
 * zona. Quanto mais perto do núcleo / maior a densidade, mais cego você fica.
 *
 * <p>Esta é a versão "fog de visão" (a primeira) — NÃO usa partículas de fumaça.
 * O emissor de partículas ({@link ClientFogParticles}) está desligado.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class ClientFogEvents {

    private ClientFogEvents() {}

    /** Tinta máxima de cor do ar dentro da névoa (forte quando densa). */
    private static final float MAX_TINT = 0.85f;

    /** Tonaliza o ar com a cor da zona (vermelho, preto, etc). */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String dim = mc.level.dimension().location().toString();
        Vec3 cam = event.getCamera().getPosition();
        ClientFogZones.Result r = combinedFog(cam.x, cam.y, cam.z, dim);
        if (r == null) return;

        float s = Math.max(0f, Math.min(MAX_TINT, r.strength));
        float cr = ((r.color >> 16) & 0xFF) / 255f;
        float cg = ((r.color >> 8) & 0xFF) / 255f;
        float cb = (r.color & 0xFF) / 255f;
        event.setRed(Mth.lerp(s, event.getRed(), cr));
        event.setGreen(Mth.lerp(s, event.getGreen(), cg));
        event.setBlue(Mth.lerp(s, event.getBlue(), cb));
    }

    /** Fecha o alcance de visão dentro da névoa — "não dá pra ver nada". */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        String dim = mc.level.dimension().location().toString();
        Vec3 cam = event.getCamera().getPosition();
        ClientFogZones.Result r = combinedFog(cam.x, cam.y, cam.z, dim);
        if (r == null) return;

        float s = Math.max(0f, Math.min(1f, r.strength));
        if (s <= 0.001f) return;
        // quanto mais forte/denso, MENOR a visibilidade:
        // s baixo  → ~50 blocos (névoa leve)
        // s máximo → ~3 blocos  (praticamente cego)
        float far = Mth.lerp(s, 50f, 3.0f);
        float near = Math.max(0.1f, far * 0.12f);
        event.setNearPlaneDistance(near);
        event.setFarPlaneDistance(far);
        event.setCanceled(true); // aplica os novos planos
    }

    /**
     * Junta a névoa de ZONA (no local) com a névoa PESSOAL (segue o player, só
     * ele vê). Retorna a mais forte das duas, ou null se nenhuma afeta o ponto.
     */
    private static ClientFogZones.Result combinedFog(double x, double y, double z, String dim) {
        ClientFogZones.Result zone = ClientFogZones.compute(x, y, z, dim);
        if (ClientPersonalFog.isActive()) {
            float zStrength = (zone != null) ? zone.strength : 0f;
            float pd = ClientPersonalFog.density();
            if (pd > zStrength) {
                return new ClientFogZones.Result(ClientPersonalFog.color(), pd);
            }
        }
        return zone;
    }
}
