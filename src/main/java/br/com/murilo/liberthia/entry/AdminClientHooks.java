package br.com.murilo.liberthia.entry;

import net.minecraft.client.Minecraft;

public final class AdminClientHooks {

    private AdminClientHooks() {}

    public static void openMainScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new AdminToolScreen());
    }
}