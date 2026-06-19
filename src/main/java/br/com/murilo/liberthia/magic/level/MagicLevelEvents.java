package br.com.murilo.liberthia.magic.level;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.source.MagicLevelData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r157: Hook que dá kill-credits para o sistema de Magic Level.
 *
 * <ul>
 *   <li>{@link LivingDeathEvent} — entity morta por player concede 1 kill credit</li>
 *   <li>{@link PlayerEvent.PlayerLoggedInEvent} — força derivação de stats do level
 *       atual (corrige max source/mana ao logar com level salvo)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MagicLevelEvents {

    private MagicLevelEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) return; // PvP não conta
        var src = event.getSource().getEntity();
        if (!(src instanceof Player p)) return;
        if (p.level().isClientSide) return;
        MagicLevelData.addKills(p, 1);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player p = event.getEntity();
        // Re-derive stats (max source) baseado no level salvo
        MagicLevelData.updateDerivedStats(p, MagicLevelData.getLevel(p));
    }
}
