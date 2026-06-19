package br.com.murilo.liberthia.cosmic.framework;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * v0.1.24 r81: Interface base pra cada um dos 18 sistemas de horror.
 *
 * <p>Cada sistema implementa lógica de tick por player. O framework
 * ({@link HorrorFramework}) chama {@code tick} 1x por player tick (END phase).
 *
 * <p>Sistemas devem ser <b>idempotentes</b> e <b>rate-limited</b> internamente —
 * NÃO confie em ser chamado em interval fixo, e use {@code tick % N == 0}
 * pra reduzir frequência.
 *
 * <p>Sistemas DEVEM:
 * <ul>
 *   <li>Adicionar exposição via {@link HorrorState#addExposure} quando o gatilho rola</li>
 *   <li>Aplicar efeitos cinematográficos (sons, partículas, packets) escalando com a exposição</li>
 *   <li>Decair gradualmente quando o player está em área safe</li>
 *   <li>Logar via {@code LiberthiaMod.LOGGER.debug} — nunca chat spam</li>
 * </ul>
 */
public interface HorrorSystem {

    /** Tipo de horror que este sistema representa. */
    HorrorType type();

    /** Tick principal — chamado cada player tick (END phase). */
    void tick(ServerPlayer sp, ServerLevel level, long gameTime, HorrorState state);

    /** Player trocou de dimensão. Sistemas podem reagir (ex.: reset, boost). */
    default void onDimensionChange(ServerPlayer sp, HorrorState state) {}

    /** Player abriu inventário (perception horror usa isto). */
    default void onInventoryOpen(ServerPlayer sp, HorrorState state) {}

    /** Player digitou em chat. Memetic horror escuta gatilhos textuais. */
    default void onChat(ServerPlayer sp, String message, HorrorState state) {}

    /** Player morreu. */
    default void onDeath(ServerPlayer sp, HorrorState state) {}

    /** Decay de exposição em áreas safe. Default: 0.05 por tick (lento). */
    default float baseDecayRate() {
        return 0.05F;
    }
}
