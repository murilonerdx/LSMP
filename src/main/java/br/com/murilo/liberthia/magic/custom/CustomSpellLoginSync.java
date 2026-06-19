package br.com.murilo.liberthia.magic.custom;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r42: Hooks de login/respawn pra sincronizar custom spells do player
 * com o client (necessário pra Spell Wheel renderizar os feitiços corretos).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CustomSpellLoginSync {

    private CustomSpellLoginSync() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        syncTo(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        syncTo(sp);
    }

    @SubscribeEvent
    public static void onChangedDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        syncTo(sp);
    }

    private static void syncTo(ServerPlayer sp) {
        try {
            ModNetwork.sendToPlayer(sp,
                    new SyncCustomSpellsS2CPacket(
                            CustomSpellStorage.getAll(sp),
                            CustomSpellStorage.getSelectedId(sp)));
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[CustomSpell] login sync error: {}", t.toString());
        }
    }
}
