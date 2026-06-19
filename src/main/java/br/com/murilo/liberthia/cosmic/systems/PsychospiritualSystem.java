package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * r81 #18/18 — <b>Psychospiritual Horror</b>.
 *
 * <p>Mente + alma + dimensões. Player em Spirit World ganha exposure
 * rápida deste tipo. Em exposure alta, sonha eventos — recebe MobEffects
 * de NIGHT_VISION + GLOWING enquanto dorme.
 *
 * <p>Quando 2+ players estão simultaneamente em Spirit World com exposure
 * &gt; 70, recebem mensagem "...os outros sonham com você..." (cross-player
 * shared dream).
 */
public final class PsychospiritualSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.PSYCHOSPIRITUAL;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        if (gameTime % 60 != 0) return;

        if (SpiritDimension.isInSpiritWorld(sp)) {
            state.addExposure(HorrorType.PSYCHOSPIRITUAL, 1.5F);
        }

        // Player sleeping ganha exposure rapida
        if (sp.isSleeping()) {
            state.addExposure(HorrorType.PSYCHOSPIRITUAL, 3.0F);
        }

        float exp = state.getExposure(HorrorType.PSYCHOSPIRITUAL);

        // 50+: night vision brief + particles
        if (exp >= 50 && gameTime % 200 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 80, 0, true, false));
        }

        // 70+: shared dream check
        if (exp >= 70 && state.canFireEvent("shared_dream", gameTime, 24000)) {
            int spiritPlayers = 0;
            for (var other : sp.server.getPlayerList().getPlayers()) {
                if (other == sp) continue;
                if (SpiritDimension.isInSpiritWorld(other)) {
                    spiritPlayers++;
                }
            }
            if (spiritPlayers >= 1) {
                sp.displayClientMessage(Component.literal(
                        "§5§o✦ ...os outros sonham com você..."), false);
                state.markEventFired("shared_dream", gameTime);
                // Glowing brief — pode ver outros através de blocos
                sp.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, true, false));
            }
        }

        // Decay fora de spirit
        if (!SpiritDimension.isInSpiritWorld(sp) && gameTime % 200 == 0) {
            state.decayExposure(HorrorType.PSYCHOSPIRITUAL, baseDecayRate());
        }
    }
}
