package br.com.murilo.liberthia.entry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class AdminClientState {

    private static final List<AdminPlayerEntry> PLAYERS = new ArrayList<>();

    private AdminClientState() {}

    public static List<AdminPlayerEntry> snapshotPlayers() {
        return new ArrayList<>(PLAYERS);
    }

    public static void updatePlayers(List<AdminPlayerEntry> newPlayers) {
        PLAYERS.clear();
        PLAYERS.addAll(newPlayers);

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AdminToolScreen screen) {
            screen.replacePlayers(newPlayers);
        }
    }

    public static void openInventory(AdminInventorySnapshot snapshot) {
        Minecraft mc = Minecraft.getInstance();
        Screen parent = mc.screen;
        mc.setScreen(new AdminInventoryScreen(parent, snapshot));
    }
}
