package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * r81 #11/18 — <b>Analog Horror</b> (Local58 / Mandela Catalogue).
 *
 * <p>Player periodicamente recebe "transmissões de emergência" — title
 * messages crípticas com símbolos VHS, sons de static, intermitência.
 * Diferente do cosmic horror antigo (que já foi muted), isto roda raramente
 * (uma vez a cada 15-30 min) e só pra players com exposure ANALOG &gt;= 40.
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Exposure cresce passivamente em chunk-load events / dimensional swap</li>
 *   <li>A cada 30 min de jogo (cooldown), emite transmissão</li>
 * </ul>
 */
public final class AnalogSystem implements HorrorSystem {

    private static final String[] TRANSMISSIONS = {
            "§7§l[ § §r§7§lEMERGENCY BROADCAST §r§7§l ]\n§r§7§o███ ████ ██ ███████",
            "§7§l[ §l█§r§7§l SIGNAL LOST §l█§r§7§l ]\n§r§4§oattention all citizens — do not look",
            "§7§l[ § §r§7§lUNKNOWN TRANSMISSION §r§7§l ]\n§r§7§o…they are inside the walls…",
            "§7§l[ §l█§r§7§l TEST PATTERN §l█§r§7§l ]\n§r§5§obreathing detected — source unknown",
            "§7§l[ § §r§7§lLOOPING BROADCAST §r§7§l ]\n§r§7§o…stay where you are. do not move…"
    };

    @Override
    public HorrorType type() {
        return HorrorType.ANALOG;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 100 != 0) return;

        // Exposure passiva conforme tempo de jogo
        if (sp.tickCount > 12000) {
            state.addExposure(HorrorType.ANALOG, 0.1F);
        }

        float exp = state.getExposure(HorrorType.ANALOG);

        // Transmissão a cada 18000 ticks (15 min) em exp alto
        if (exp >= 40 && state.canFireEvent("analog_broadcast", gameTime, 18000)
                && Math.random() < 0.3) {
            emitBroadcast(sp);
            state.markEventFired("analog_broadcast", gameTime);
        }

        // Decay
        state.decayExposure(HorrorType.ANALOG, baseDecayRate() * 0.5F);
    }

    private void emitBroadcast(ServerPlayer sp) {
        String msg = TRANSMISSIONS[(int)(Math.random() * TRANSMISSIONS.length)];
        // Title + subtitle
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                Component.literal(msg.split("\n")[0])));
        if (msg.contains("\n")) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.literal(msg.split("\n")[1])));
        }
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                10, 80, 10));

        // Static noise
        sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.MASTER, 0.8F, 0.4F);
    }
}
