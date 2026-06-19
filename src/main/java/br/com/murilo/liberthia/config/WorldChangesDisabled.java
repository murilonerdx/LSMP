package br.com.murilo.liberthia.config;

/**
 * Kill-switch global pra desligar TODOS os modificadores automáticos de
 * mundo do mod (spread de infection, black hole carving, world spawner,
 * random ticks de blocos infectados, etc).
 *
 * <p>Quando {@link #ACTIVE} é {@code true}:
 * <ul>
 *   <li>Nenhum tick automático modifica blocos</li>
 *   <li>Black hole continua existindo mas não destrói/cria blocos</li>
 *   <li>Random ticks de corrupted soil/infection growth viram no-op</li>
 *   <li>WorldSpawnerEvents não spawna nada</li>
 *   <li>InfectionLogic.evaluateDarkMatterRegion sai antes</li>
 * </ul>
 *
 * <p>Permite ao player colocar/quebrar blocos manualmente — só desativa
 * o "comportamento de espalhamento" do mod. Usado pra debugar chunks
 * recortando.
 */
public final class WorldChangesDisabled {

    /** Setado true → mod fica passivo, nenhuma mudança automática de bloco. */
    public static volatile boolean ACTIVE = true;

    private WorldChangesDisabled() {}
}
