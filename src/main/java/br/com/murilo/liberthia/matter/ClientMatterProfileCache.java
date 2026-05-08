package br.com.murilo.liberthia.matter;

/**
 * Cache lado-cliente dos valores de matéria do jogador local. Atualizado pelo
 * pacote S2C de sync e lido pelo HUD overlay.
 */
public final class ClientMatterProfileCache {

    private static volatile float dark;
    private static volatile float white;
    private static volatile float yellow;
    private static volatile MatterProfileType activeType = MatterProfileType.NONE;

    private ClientMatterProfileCache() {}

    public static void update(float dm, float wm, float ym) {
        dark = dm; white = wm; yellow = ym;
        activeType = MatterProfileType.detect(dm, wm, ym);
    }

    public static float dark()   { return dark; }
    public static float white()  { return white; }
    public static float yellow() { return yellow; }
    public static MatterProfileType activeType() { return activeType; }
}
