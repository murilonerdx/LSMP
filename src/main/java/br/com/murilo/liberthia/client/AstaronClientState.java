package br.com.murilo.liberthia.client;

/** r187 — espelho client-side do estado de Astaron (lido pela Escala, HUD e tint do céu). */
public final class AstaronClientState {
    private AstaronClientState() {}
    public static volatile int radiation = 0;
    public static volatile int cosmicCount = 0;
    public static volatile int skyColor = 0xFF0000;

    public static void update(int rad, int count, int color) {
        radiation = rad; cosmicCount = count; skyColor = color;
    }

    public static float radiationFraction() { return radiation / 100.0f; }
}
