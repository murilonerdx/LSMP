package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Detecta itens com conteúdo de matéria nas mãos do jogador e adiciona
 * matéria ao perfil gradativamente.
 *
 * <p>Ex: segurar uma {@code DARK_MATTER_SWORD} (DM:40 no registry) gera
 * {@code 40 × 0.005 = 0.2 DM} a cada tick processado (60 ticks = 12 DM/min).
 *
 * <p>Aplica também pra qualquer item registrado em
 * {@link MatterContentRegistry} — segurar uma matéria amarela na mão também
 * cresce o YM, etc.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MatterContactHandler {

    /** A cada N ticks processa o contato. */
    public static final int PERIOD = 60;
    /** Multiplicador: matter * RATE = pontos adicionados por período. */
    public static final float RATE = 0.005f;

    private MatterContactHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % PERIOD != 0) return;
        if (player.isCreative() || player.isSpectator()) return;

        // Soma conteúdo de matéria de main hand + off hand
        MatterContent main  = MatterContentRegistry.of(player.getMainHandItem());
        MatterContent off   = MatterContentRegistry.of(player.getOffhandItem());

        float dm = (main.dark()   + off.dark())   * RATE * PERIOD;
        float wm = (main.white()  + off.white())  * RATE * PERIOD;
        float ym = (main.yellow() + off.yellow()) * RATE * PERIOD;
        if (dm <= 0 && wm <= 0 && ym <= 0) return;

        player.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            if (dm > 0) profile.addDark(dm);
            if (wm > 0) profile.addWhite(wm);
            if (ym > 0) profile.addYellow(ym);
            MatterProfileEvents.syncTo(player);
        });
    }
}
