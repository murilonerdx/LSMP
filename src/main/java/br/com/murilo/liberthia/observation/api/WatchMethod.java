package br.com.murilo.liberthia.observation.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;

/**
 * v0.1.22 r60: <b>WatchMethod</b> — equivalente do {@code AbstractCastMethod}
 * em AN, mas reframed. Representa o "como observar" — entry point do feitiço.
 *
 * <p>O recipe SEMPRE começa com um WatchMethod. Ele determina como o cast é
 * disparado e onde o "hit point" inicial está.
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>{@code DirectGazeMethod} — onde o player olha</li>
 *   <li>{@code PeripheralMethod} — algo na periferia da visão</li>
 *   <li>{@code MemoryMethod} — último local que foi observado</li>
 *   <li>{@code ReflectionMethod} — através de espelho/água</li>
 *   <li>{@code SilenceMethod} — quando ninguém olha</li>
 * </ul>
 */
public abstract class WatchMethod extends ObservationPart {

    protected WatchMethod(ResourceLocation id, String displayName) {
        super(id, displayName);
    }

    @Override public int typeIndex() { return 1; }

    /**
     * Chamado quando o player ativa a observação. Retorna o ponto onde a
     * resolução continua, ou null se a condição falhou.
     *
     * <p>O resolver chama isto, depois passa o HitResult retornado pra cada
     * Manifestation no recipe.
     */
    public abstract HitResult observe(ServerPlayer caster, ServerLevel level,
                                       ObservationContext ctx);
}
