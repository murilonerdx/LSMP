package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.block.entity.FlightBeaconBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r184 — concede/revoga VOO de sobrevivência conforme o marcador do {@link FlightBeaconBlockEntity}.
 * Enquanto dentro do campo (marcador renovado a cada tick) o player pode voar; ao sair / faltar FE,
 * o voo é revogado (só o que o farol concedeu). NUNCA mexe em criativo/espectador.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class FlightBeaconHandler {
    private FlightBeaconHandler() {}
    private static final String GRANTED = "liberthia.beaconGranted";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        var d = sp.getPersistentData();
        if (sp.isCreative() || sp.isSpectator()) { d.putInt(FlightBeaconBlockEntity.NBT_FLY, 0); return; }

        int fly = d.getInt(FlightBeaconBlockEntity.NBT_FLY);
        if (fly > 0) {
            d.putInt(FlightBeaconBlockEntity.NBT_FLY, fly - 1);
            if (!sp.getAbilities().mayfly) {
                sp.getAbilities().mayfly = true;
                d.putBoolean(GRANTED, true);
                sp.onUpdateAbilities();
            }
        } else if (d.getBoolean(GRANTED)) {
            sp.getAbilities().mayfly = false;
            sp.getAbilities().flying = false;
            d.putBoolean(GRANTED, false);
            sp.onUpdateAbilities();
        }
    }
}
