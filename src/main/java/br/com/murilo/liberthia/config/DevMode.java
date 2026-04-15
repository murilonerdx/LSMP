package br.com.murilo.liberthia.config;

/**
 * Temporary dev/worker mode flag.
 * When ACTIVE is true:
 *  - Infection tick logic is skipped (server side)
 *  - All custom HUD overlays are hidden (client side)
 * The mod features themselves remain registered and intact.
 */
public final class DevMode {
    public static boolean ACTIVE = true;

    private DevMode() {
    }
}
