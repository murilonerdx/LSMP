package br.com.murilo.liberthia.observation.client;

import br.com.murilo.liberthia.observation.particle.GlowData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * v0.1.22 r65: Client helper pra cast animation.
 *
 * <p>Quando o {@link br.com.murilo.liberthia.observation.network.CastAnimationS2CPacket}
 * chega, faz swing de braço + spawn 20 GlowParticle em volta do player local.
 */
@OnlyIn(Dist.CLIENT)
public final class CastAnimationClient {

    private CastAnimationClient() {}

    public static void play(int color) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;

        // Swing main hand
        p.swing(InteractionHand.MAIN_HAND);

        // Glow burst em volta do player
        var glow = new GlowData(color, 0.6F, 1.0F, 18);
        for (int i = 0; i < 24; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 0.8 + Math.random() * 1.5;
            double dy = Math.random() * 2.0;
            double vx = -Math.cos(a) * 0.05;
            double vz = -Math.sin(a) * 0.05;
            p.level().addParticle(glow,
                p.getX() + Math.cos(a) * r,
                p.getY() + dy,
                p.getZ() + Math.sin(a) * r,
                vx, 0.02, vz);
        }
    }
}
