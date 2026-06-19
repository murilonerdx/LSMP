package br.com.murilo.liberthia.magic.custom.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.KeyBindings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r42: Registra as keybinds {@code SPELL_WHEEL_KEY} (X) e
 * {@code QUICK_CAST_KEY} (V) no event bus de mod.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpellKeybindRegister {

    private SpellKeybindRegister() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.SPELL_WHEEL_KEY);
        event.register(KeyBindings.QUICK_CAST_KEY);
        // r138: 3 spell hotbar slots
        event.register(KeyBindings.SPELL_HOTBAR_1);
        event.register(KeyBindings.SPELL_HOTBAR_2);
        event.register(KeyBindings.SPELL_HOTBAR_3);
        // r155: Spell Wheel radial (R)
        event.register(KeyBindings.SPELL_WHEEL_RADIAL);
    }
}
