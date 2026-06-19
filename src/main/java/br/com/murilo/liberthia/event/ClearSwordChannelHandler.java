package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.ClearMatterSwordItem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Intercepta LivingHurtEvent — quando um player que está canalizando a
 * Clear Matter Sword toma dano, marca a flag de interrupção.
 *
 * <p>O check do flag ocorre dentro de {@link ClearMatterSwordItem#onUseTick}
 * no próximo tick — encerra o channel + breaks o som.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ClearSwordChannelHandler {

    private ClearSwordChannelHandler() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (event.getAmount() <= 0) return;
        ClearMatterSwordItem.interrupt(player.getUUID());
    }
}
