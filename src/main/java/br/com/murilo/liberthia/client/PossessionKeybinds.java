package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r30: registra a keybind G (Possession Ability).
 *
 * <p>Quando ativa enquanto possui um mob, dispara a habilidade própria do mob:
 * Creeper explode, Enderman teleporta, Ghast atira fireball, etc.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PossessionKeybinds {

    public static final String CATEGORY = "key.categories.liberthia";
    public static final String ABILITY_NAME = "key.liberthia.possession_ability";

    private PossessionKeybinds() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        KeyMapping abilityKey = new KeyMapping(
                ABILITY_NAME,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_G,
                CATEGORY);
        event.register(abilityKey);
        PossessionClient.abilityKey = abilityKey;
    }
}
