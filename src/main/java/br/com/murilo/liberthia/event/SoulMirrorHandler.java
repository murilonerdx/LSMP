package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.SpiritMagicItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.EnderManAngerEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r29: handler que aplica reflexão de dano do {@code soul_mirror}
 * (curio). Quando player com soul mirror recebe dano de uma entidade viva,
 * 25% do dano é refletido ao atacante.
 *
 * <p>r164 FIX (bug #39): adicionado handler de pacificação Enderman.
 * O tooltip do item dizia "Endermen nunca ficam hostis" mas nenhum
 * listener implementava isso — agora cancelamos:
 * <ul>
 *   <li>{@link EnderManAngerEvent} (Forge dispara quando enderman olharia)</li>
 *   <li>{@link LivingChangeTargetEvent} se atacante é enderman e alvo tem mirror</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SoulMirrorHandler {

    private SoulMirrorHandler() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getSource().getEntity() == victim) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        float reflect = SpiritMagicItems.SoulMirror.reflectFactor(victim);
        if (reflect <= 0) return;

        float reflected = event.getAmount() * reflect;
        if (reflected <= 0.1F) return;

        // Aplica dano de retorno (usa damage type magic pra evitar loop)
        attacker.hurt(attacker.damageSources().magic(), reflected);
    }

    /**
     * r164: Forge dispara este evento quando enderman vai ficar agressivo
     * por player olhar pra ele. Cancela se o player tiver o Hermetic Mirror.
     */
    @SubscribeEvent
    public static void onEndermanAnger(EnderManAngerEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (SpiritMagicItems.SoulMirror.reflectFactor(player) > 0) {
            event.setCanceled(true);
        }
    }

    /**
     * r164: fallback — se um enderman ja escolheu o player com mirror como
     * alvo (ex.: dropoff de raio ou via comando), reseta o target pra null.
     */
    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.EnderMan)) return;
        if (!(event.getNewTarget() instanceof Player p)) return;
        if (SpiritMagicItems.SoulMirror.reflectFactor(p) > 0) {
            event.setCanceled(true);
        }
    }
}
