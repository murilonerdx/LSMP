package br.com.murilo.liberthia.client.hud;

/**
 * 4 cantos pra posicionamento do HUD do Matter Profile. Persiste num arquivo
 * de config simples ({@code config/liberthia_hud.txt}) — gravado/lido a cada
 * ciclo via keybind.
 */
public enum HudPosition {
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_LEFT;

    private static volatile HudPosition current = TOP_RIGHT;

    public static HudPosition current() { return current; }

    public static HudPosition cycle() {
        HudPosition[] all = values();
        current = all[(current.ordinal() + 1) % all.length];
        save();
        return current;
    }

    public static void load() {
        try {
            java.nio.file.Path file = configFile();
            if (java.nio.file.Files.exists(file)) {
                String s = java.nio.file.Files.readString(file).trim();
                current = HudPosition.valueOf(s);
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            java.nio.file.Path file = configFile();
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.writeString(file, current.name());
        } catch (Exception ignored) {}
    }

    private static java.nio.file.Path configFile() {
        return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("liberthia_hud.txt");
    }
}
