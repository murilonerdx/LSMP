package br.com.murilo.liberthia.observation.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r60: <b>Distortion</b> — equivalente do {@code AbstractAugment}
 * em AN. Modifica intensidade, alcance, duração, eco do efeito ANTES dele
 * ser executado.
 *
 * <p>No recipe, Distortions são colocadas DEPOIS da Manifestation que
 * modificam. Múltiplas podem se acumular.
 *
 * <h2>Custo</h2>
 * Distortions adicionam sanity cost via {@link #sanityCostForPart(ObservationPart)} —
 * pode variar dependendo do parent (ex: amplify ×2 custa mais em Manifestações
 * AOE do que em single-target).
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>{@code AmplifyDistortion} — intensity ×1.5</li>
 *   <li>{@code ExpandDistortion} — reach +2 blocos</li>
 *   <li>{@code LingerDistortion} — duration +60 ticks</li>
 *   <li>{@code EchoDistortion} — echoCount +1 (efeito repete)</li>
 *   <li>{@code RevealDistortion} — perceptionLevel +1 (mais visível)</li>
 *   <li>{@code SecretDistortion} — perceptionLevel -1 (só caster vê)</li>
 * </ul>
 */
public abstract class Distortion extends ObservationPart {

    protected Distortion(ResourceLocation id, String displayName) {
        super(id, displayName);
    }

    @Override public int typeIndex() { return 10; }

    /**
     * Custo adicional baseado em qual part está sendo modificado.
     * <p>Override pra escalar com type/intensity do parent.
     */
    public int sanityCostForPart(@Nullable ObservationPart parent) {
        return sanityCost();
    }

    /** Distortion sempre tem applyToStats — esse é o ponto. */
    @Override
    public abstract void applyToStats(ObservationStats.Builder builder,
                                       @Nullable ObservationPart augmented);
}
