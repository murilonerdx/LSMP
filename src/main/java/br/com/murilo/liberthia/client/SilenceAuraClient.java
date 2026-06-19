package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.horror.entity.SilenceShepherdEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r180b — <b>Aura de Silêncio do Pastor</b> (cliente). Quando o player local está
 * dentro da aura de um {@link SilenceShepherdEntity}, TODO som prestes a tocar é
 * <b>engolido</b> (setSound(null)) — não fica abafado, simplesmente deixa de existir.
 * Lê o raio sincronizado via {@link SilenceShepherdEntity#getAuraRadius()}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class SilenceAuraClient {

    private SilenceAuraClient() {}

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (event.getSound() == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof SilenceShepherdEntity pastor)) continue;
            float aura = pastor.getAuraRadius();
            double dx = px - e.getX(), dy = py - e.getY(), dz = pz - e.getZ();
            if (dx * dx + dy * dy + dz * dz <= aura * aura) {
                event.setSound(null); // engolido pelo silêncio
                return;
            }
        }
    }
}
