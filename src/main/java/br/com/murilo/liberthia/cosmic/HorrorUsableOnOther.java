package br.com.murilo.liberthia.cosmic;

import net.minecraft.server.level.ServerPlayer;

/**
 * r179: itens de horror cósmico que podem ser "usados em OUTRO player" — quando o
 * portador segura SHIFT e interage (clique direito) com outro jogador, o efeito do
 * item recai sobre o ALVO, como se ele tivesse usado o item (susto/voz/observador…).
 *
 * <p>Disparado por {@link HorrorShiftUseHandler}. O {@code use()} normal (sem alvo)
 * continua afetando o próprio portador.
 */
public interface HorrorUsableOnOther {
    void useOnOther(ServerPlayer user, ServerPlayer target);
}
