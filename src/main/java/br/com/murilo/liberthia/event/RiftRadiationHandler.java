package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.RiftCutterItem;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r185 — <b>Radiação da Adaga Corta-Fendas</b>. Quem segura uma adaga (qualquer nível) irradia
 * todos os jogadores num raio de 8 blocos que NÃO estejam segurando uma adaga. Efeito reaplicado
 * a cada 40 ticks (dura 120) → some 6s depois que o portador se afasta.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RiftRadiationHandler {
    private RiftRadiationHandler() {}

    private static boolean holdingCutter(Player p) {
        return p.getMainHandItem().getItem() instanceof RiftCutterItem
                || p.getOffhandItem().getItem() instanceof RiftCutterItem;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer holder)) return;
        if (holder.tickCount % 40 != 0) return;
        if (!holdingCutter(holder)) return;

        AABB box = holder.getBoundingBox().inflate(8);
        for (Player victim : holder.level().getEntitiesOfClass(Player.class, box,
                p -> p != holder && !p.isCreative() && !p.isSpectator() && !holdingCutter(p))) {
            victim.addEffect(new MobEffectInstance(ModEffects.DIMENSIONAL_RADIATION.get(), 120, 0, false, true, true));
        }
    }
}
