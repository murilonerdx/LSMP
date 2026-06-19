package br.com.murilo.liberthia.magic.spell.hotbar;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.162 r138: <b>SpellHotbarLoginSync</b> — envia snapshot do hotbar pro
 * client quando player loga (caso contrário client não sabe o que tá bound).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpellHotbarLoginSync {

    private SpellHotbarLoginSync() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        ModNetwork.sendToPlayer(sp, SpellHotbarSyncS2CPacket.snapshot(sp));
    }

    /** Re-sync ao trocar de dimension (cliente pode reiniciar overlay). */
    @SubscribeEvent
    public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        ModNetwork.sendToPlayer(sp, SpellHotbarSyncS2CPacket.snapshot(sp));
    }
}
