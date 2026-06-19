package br.com.murilo.liberthia.client;

/**
 * Espelho client-side do estado da Lua do Medo (lido pelos renderers de céu/lua).
 * Classe simples (sem imports de cliente) — segura de referenciar no handle do packet.
 */
public final class ClientFearMoonState {
    private ClientFearMoonState() {}

    public static volatile boolean active = false;
    public static volatile int color = 0xC0102A; // vermelho-sangue default

    public static void update(boolean a, int c) {
        active = a;
        color = c;
    }
}
