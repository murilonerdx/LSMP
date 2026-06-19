package br.com.murilo.liberthia.fourthwall;

/**
 * Os efeitos de "quebra de quarta parede" do servidor. Cada um pode ser
 * ligado/desligado individualmente ({@link FourthWallData}).
 *
 * <p>{@code scheduler=true} = entra no sorteio aleatório periódico por player.
 * Os {@code false} são disparados por gatilho próprio (morte / madrugada).
 */
public enum FourthWallFeature {
    CROSSHAIR_EYE("crosshair", true),   // a mira pisca virando um olho
    REAL_CLOCK("relogio", true),        // taunt usando o relógio REAL do PC
    F3_LIE("f3", true),                 // debug (F3) mostra coords/bioma errados
    FAKE_CRASH("crash", true),          // tela de "crash" falsa que volta sozinha
    TAB_GHOST("tab", true),             // nome fantasma na lista (TAB)
    DEATH_SCREEN("morte", false),       // causa da morte vira algo críptico
    SIGN_REWRITE("placa", false);       // placas reescritas de madrugada

    public final String id;
    public final boolean scheduler;

    FourthWallFeature(String id, boolean scheduler) {
        this.id = id;
        this.scheduler = scheduler;
    }

    public static FourthWallFeature byId(String s) {
        if (s == null) return null;
        for (FourthWallFeature f : values()) {
            if (f.id.equalsIgnoreCase(s) || f.name().equalsIgnoreCase(s)) return f;
        }
        return null;
    }
}
