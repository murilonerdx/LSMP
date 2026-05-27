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

    /** r42: Spell Wheel — abre o radial menu de custom spells (default X). */
    public static final String KEY_SPELL_WHEEL = "key.liberthia.spell_wheel";
    public static final KeyMapping SPELL_WHEEL_KEY = new KeyMapping(
            KEY_SPELL_WHEEL,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            KEY_CATEGORY_LIBERTHIA
    );

    /** r42: Quick-cast — casta o feitiço selecionado sem abrir wheel (default V). */
    public static final String KEY_QUICK_CAST = "key.liberthia.quick_cast";
    public static final KeyMapping QUICK_CAST_KEY = new KeyMapping(
            KEY_QUICK_CAST,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY_LIBERTHIA
    );

    /** r138: Spell Hotbar slots — 3 quick-cast bindings (default Z, B, N). */
    public static final KeyMapping SPELL_HOTBAR_1 = new KeyMapping(
            "key.liberthia.spell_hotbar_1",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            KEY_CATEGORY_LIBERTHIA
    );
    public static final KeyMapping SPELL_HOTBAR_2 = new KeyMapping(
            "key.liberthia.spell_hotbar_2",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY_LIBERTHIA
    );
    public static final KeyMapping SPELL_HOTBAR_3 = new KeyMapping(
            "key.liberthia.spell_hotbar_3",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            KEY_CATEGORY_LIBERTHIA
    );

    /** r155: Spell Wheel — abre menu radial de 8 slots (hold R). */
    public static final KeyMapping SPELL_WHEEL_RADIAL = new KeyMapping(
            "key.liberthia.spell_wheel_radial",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY_LIBERTHIA
    );

    private KeyBindings() {
    }
}
