package br.com.murilo.liberthia.client.fog;

/**
 * Estado client-side da NÉVOA PESSOAL — a névoa que segue UM player (só ele vê),
 * tipo efeito de poção. Setado pelo {@code PersonalFogS2CPacket} (que o servidor
 * manda só pra esse player). Lido pelo {@link ClientFogEvents} junto com as zonas.
 */
public final class ClientPersonalFog {

    private static volatile boolean active = false;
    private static volatile int color = 0x05050A;
    private static volatile float density = 0.9f;

    private ClientPersonalFog() {}

    public static void set(boolean a, int c, float d) {
        active = a;
        color = c & 0xFFFFFF;
        density = Math.max(0f, Math.min(2f, d));
    }

    public static void clear() { active = false; }

    public static boolean isActive() { return active; }
    public static int color() { return color; }
    public static float density() { return density; }
}
