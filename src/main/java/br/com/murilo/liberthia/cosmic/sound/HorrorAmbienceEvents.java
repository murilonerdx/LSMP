package br.com.murilo.liberthia.cosmic.sound;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r176: Ganchos de ambientacao de terror que tocam os sons PROCEDURAIS
 * (gerados por {@code tools/gen_horror_ambience.py}) conforme o estado do jogador.
 *
 * <p><b>Batimento cardiaco por sanidade baixa:</b> quando a sanidade cai abaixo de
 * {@link #LOW_SANITY}, o jogador ouve um coracao batendo (apenas ele, via
 * {@code playNotifySound}) — mais rapido e mais alto quanto mais perto de 0.
 * Acima do limiar, silencio. Totalmente isolado: nao altera a logica de sanidade.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class HorrorAmbienceEvents {

    /** Sanidade abaixo da qual o coracao comeca a bater. */
    public static final int LOW_SANITY = 40; // r183: alinhado ao gate global de horror

    private HorrorAmbienceEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;

        int sanity = SpiritDimension.getSanity(sp);
        if (sanity >= LOW_SANITY) return;

        // f: 0 (sanidade 0 = panico) .. 1 (no limiar). Quanto menor, mais rapido/alto.
        double f = (double) Math.max(0, sanity) / LOW_SANITY;
        // intervalo entre batidas: ~28 ticks (1.4s, urgente) .. ~60 ticks (3s, lento)
        int interval = 28 + (int) Math.round(f * 32);
        if (sp.tickCount % interval != 0) return;

        float volume = (float) (0.5 + (1.0 - f) * 0.5);  // 0.5 .. 1.0
        float pitch = (float) (0.92 + (1.0 - f) * 0.12); // leve aceleracao de pitch
        sp.playNotifySound(ModSounds.AMBIENCE_HEARTBEAT.get(), SoundSource.AMBIENT, volume, pitch);
    }
}
