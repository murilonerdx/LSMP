package br.com.murilo.liberthia.observation.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;

/**
 * v0.1.22 r60: <b>Manifestation</b> — equivalente do {@code AbstractEffect}
 * em AN. O "o que se manifesta" — o efeito real do feitiço.
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>{@code TendrilManifestation} — tentáculos atacam alvo</li>
 *   <li>{@code SilenceManifestation} — silêncio em raio</li>
 *   <li>{@code FoldingManifestation} — recurssão de espaço</li>
 *   <li>{@code MirrorManifestation} — cria clone</li>
 *   <li>{@code WhisperManifestation} — força whispers em alguém</li>
 * </ul>
 */
public abstract class Manifestation extends ObservationPart {

    protected Manifestation(ResourceLocation id, String displayName) {
        super(id, displayName);
    }

    @Override public int typeIndex() { return 5; }

    /**
     * Resolve o efeito. Pode acessar:
     * <ul>
     *   <li>{@code hit} — onde o WatchMethod determinou (ponto/entidade)</li>
     *   <li>{@code stats} — pré-calculados pelos Distortions</li>
     *   <li>{@code ctx} — state da observação atual</li>
     * </ul>
     *
     * <p>Pode também armazenar attachments no ctx pra que Manifestations
     * posteriores no recipe usem (echo, chain etc).
     */
    public abstract void manifest(HitResult hit, ServerLevel level,
                                   ServerPlayer caster,
                                   ObservationStats stats,
                                   ObservationContext ctx);
}
