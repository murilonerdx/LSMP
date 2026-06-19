package br.com.murilo.liberthia.magic.spell;

/**
 * v0.1.143 r111: Functional interface — lambda que executa o efeito do feitiço.
 *
 * <p>Recebe {@link CastContext} com tudo que precisa (caster, level, def).
 * Retorna {@code boolean}: {@code true} se castou com sucesso (consome cooldown
 * + durability), {@code false} se falhou (e.g. sem alvo válido).
 */
@FunctionalInterface
public interface SpellCast {
    boolean execute(CastContext ctx);
}
