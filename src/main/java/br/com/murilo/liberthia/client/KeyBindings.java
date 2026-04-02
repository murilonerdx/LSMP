package br.com.murilo.liberthia.client;

import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final String KEY_CATEGORY_LIBERTHIA = "key.category.liberthia";
    public static final String KEY_HUD_CONFIG = "key.liberthia.hud_config";

    public static final KeyMapping HUD_CONFIG_KEY = new KeyMapping(
            KEY_HUD_CONFIG,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            KEY_CATEGORY_LIBERTHIA
    );

    private KeyBindings() {
    }
}
