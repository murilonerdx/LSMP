package br.com.murilo.liberthia.client.fog;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.fog.FogZone;
import br.com.murilo.liberthia.fog.particle.FogMistData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renderiza a neblina como uma NUVEM DE PARTÍCULAS visível no ar — fumaça/névoa
 * de verdade, SUAVE e TRANSLÚCIDA (não redução de alcance de visão, não bolinhas
 * sólidas). A cada poucos ticks spawna puffs {@link FogMistData} coloridos (cor
 * da zona) espalhados dentro do raio. Cada puff faz fade-in, expande devagar e
 * faz fade-out — formando um volume de névoa que você vê de fora e atravessa.
 *
 * <p>Só roda no cliente, lendo as zonas sincronizadas ({@link ClientFogZones}).
 * Limitado a zonas perto do player, com teto global de partículas/emissão pra
 * não pesar.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class ClientFogParticles {

    private ClientFogParticles() {}

    /** Teto de partículas spawnadas por emissão (somando todas as zonas). */
    private static final int BUDGET_PER_EMIT = 140;

    /**
     * DESLIGADO: o usuário preferiu a névoa de VISÃO ({@link ClientFogEvents}),
     * que fecha o alcance de visão em vez de spawnar partículas de fumaça (que
     * ficavam parecendo fumacinha esparramada). Mantido aqui sem efeito caso
     * queira reativar/combinar no futuro — basta pôr {@code true}.
     */
    private static final boolean PARTICLES_ENABLED = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!PARTICLES_ENABLED) return;
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;
        // emite a cada 2 ticks pra aliviar (puffs vivem ~2-4s, então cobre bem)
        if ((level.getGameTime() & 1L) != 0L) return;

        String dim = level.dimension().location().toString();
        RandomSource rnd = level.random;
        int budget = BUDGET_PER_EMIT;

        for (FogZone z : ClientFogZones.all()) {
            if (budget <= 0) break;
            if (!z.dim.equals(dim)) continue;
            // distância de render configurável por zona
            double view = z.viewDistance;
            // longe demais? pula
            double ddx = z.x - player.getX();
            double ddy = z.y - player.getY();
            double ddz = z.z - player.getZ();
            double centerDist = Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
            if (centerDist > z.radius + view) continue;

            // intensidade = tamanho dos puffs; opacidade = transparência
            float size = Mth.clamp(z.thickness, FogZone.MIN_THICKNESS, FogZone.MAX_THICKNESS);
            float alpha = Mth.clamp(z.opacity, FogZone.MIN_OPACITY, FogZone.MAX_OPACITY);

            // NÉVOA RASTEIRA: antes espalhava numa ESFERA (jogava puffs lá no céu,
            // parecendo bolas de algodão flutuando). Agora espalha num DISCO FINO
            // colado no chão — fog de filme de terror que rasteja pela superfície.
            double slabHalf = Math.min(3.5, Math.max(1.5, z.radius * 0.22)); // altura do "lençol"
            double baseY = z.y - 0.5;                                        // levemente abaixo do centro
            int count = (int) Math.ceil(z.density * Math.min(22.0, z.radius * 1.2));
            if (count < 1) count = 1;
            double viewSq = view * view;
            for (int i = 0; i < count && budget > 0; i++) {
                // disco horizontal uniforme (sqrt evita amontoar tudo no centro)
                double ang = rnd.nextDouble() * Math.PI * 2.0;
                double rad = Math.sqrt(rnd.nextDouble()) * z.radius;
                double px = z.x + Math.cos(ang) * rad;
                double pz = z.z + Math.sin(ang) * rad;
                double py = baseY + (rnd.nextDouble() * 2.0 - 1.0) * slabHalf; // banda fina perto do chão
                if (player.distanceToSqr(px, py, pz) > viewSq) continue;       // só perto do player

                int life = 55 + rnd.nextInt(35); // 55-90 ticks
                FogMistData data = new FogMistData(z.color, size, alpha, life);
                // velocidade ~0 = névoa parada (o puff adiciona drift lento e horizontal)
                level.addParticle(data, px, py, pz, 0.0, 0.0, 0.0);
                budget--;
            }
        }
    }
}
