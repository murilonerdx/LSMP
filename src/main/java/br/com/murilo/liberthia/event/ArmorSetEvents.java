package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.item.BloodArmorItem;
import br.com.murilo.liberthia.item.OrderArmorItem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public class ArmorSetEvents {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (p == null || p.level() == null) return;
        BloodArmorItem.tickFullSet(p.level(), p);
        OrderArmorItem.tickFullSet(p.level(), p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player p = event.getEntity();
        if (p != null) {
            BloodInfectionApplier.restore(p);
        }
    }
}
