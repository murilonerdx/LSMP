package br.com.murilo.liberthia.cosmic.systems;

import br.com.murilo.liberthia.cosmic.framework.HorrorState;
import br.com.murilo.liberthia.cosmic.framework.HorrorSystem;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * r81 #5/18 — <b>Cognitive Horror</b>.
 *
 * <p>A mente não consegue processar o que vê. Aplica CONFUSION efeito
 * brief quando exposição passa de 40, e HUNGER (não come, mas mente
 * "queima"). Em níveis altos, símbolos do mapa/UI aparecem corrompidos
 * (handled client-side eventually).
 *
 * <h2>Mecânica</h2>
 * <ul>
 *   <li>Exposição passiva +0.2/tick quando lendo livros (segura BookItem)</li>
 *   <li>Confusão automática quando &gt;= 40 exposição (every 200t)</li>
 *   <li>Slowness leve quando &gt;= 70 (mente lenta)</li>
 * </ul>
 */
public final class CognitiveSystem implements HorrorSystem {

    @Override
    public HorrorType type() {
        return HorrorType.COGNITIVE;
    }

    @Override
    public void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state) {
        // Segurando livro aumenta exposição
        net.minecraft.world.item.Item held = sp.getMainHandItem().getItem();
        if (held instanceof net.minecraft.world.item.BookItem
                || held instanceof net.minecraft.world.item.WrittenBookItem
                || held instanceof net.minecraft.world.item.WritableBookItem) {
            if (gameTime % 20 == 0) {
                state.addExposure(HorrorType.COGNITIVE, 0.2F);
            }
        }

        float exp = state.getExposure(HorrorType.COGNITIVE);

        // Confusão intermitente em ~40 exposure
        if (exp >= 40 && gameTime % 200 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false));
        }

        // Slowness em 70+
        if (exp >= 70 && gameTime % 100 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
        }

        // Decay
        if (gameTime % 100 == 0 && level.isDay() && !sp.isUsingItem()) {
            state.decayExposure(HorrorType.COGNITIVE, baseDecayRate());
        }
    }
}
