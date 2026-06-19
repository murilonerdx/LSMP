package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Tiro Marcado (Arco da Caçadora): enquanto a vítima tiver o efeito
 * {@code MARKED}, recebe +30% de dano de QUALQUER fonte.
 *
 * <p>Registrado manualmente em {@code LiberthiaMod} (padrão confiável do mod).
 */
public final class HuntressMarkHandler {

    private HuntressMarkHandler() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().hasEffect(ModEffects.MARKED.get())) {
            event.setAmount(event.getAmount() * 1.3F);
        }
    }
}
