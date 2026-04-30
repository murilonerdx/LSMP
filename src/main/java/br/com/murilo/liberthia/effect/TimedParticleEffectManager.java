package br.com.murilo.liberthia.effect;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(
        modid = LiberthiaMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class TimedParticleEffectManager {

    private static final List<ActiveEffect> ACTIVE_EFFECTS = new ArrayList<>();

    private TimedParticleEffectManager() {
    }

    public static void add(ActiveEffect effect) {
        ACTIVE_EFFECTS.add(effect);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (ACTIVE_EFFECTS.isEmpty()) {
            return;
        }

        Iterator<ActiveEffect> iterator = ACTIVE_EFFECTS.iterator();

        while (iterator.hasNext()) {
            ActiveEffect effect = iterator.next();

            try {
                if (effect.tick()) {
                    iterator.remove();
                }
            } catch (Exception exception) {
                LiberthiaMod.LOGGER.error("Timed particle effect failed and was removed.", exception);
                iterator.remove();
            }
        }
    }

    @FunctionalInterface
    public interface ActiveEffect {
        /**
         * @return true quando o efeito acabou.
         */
        boolean tick();
    }
}