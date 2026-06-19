package br.com.murilo.liberthia.magic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r36: regen passivo de mana.
 *
 * <p>+2 mana/sec até MAX_MANA quando player não está em combate ativo.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ManaTickHandler {
    private ManaTickHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % PlayerSpellKnowledge.MANA_REGEN_INTERVAL != 0) return;
        int cur = PlayerSpellKnowledge.getMana(sp);
        if (cur < PlayerSpellKnowledge.MAX_MANA) {
            PlayerSpellKnowledge.setMana(sp, cur + PlayerSpellKnowledge.MANA_PER_TICK);
        }
    }
}
