package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LiberthiaMod.MODID);

    public static final RegistryObject<SoundEvent> DARK_WHISPER = register("dark_whisper");
    public static final RegistryObject<SoundEvent> DARK_PULSE = register("dark_pulse");
    public static final RegistryObject<SoundEvent> CLEAR_HUM = register("clear_hum");
    public static final RegistryObject<SoundEvent> GEIGER_TICK = register("geiger_tick");
    public static final RegistryObject<SoundEvent> INFECTION_ALERT = register("infection_alert");
    public static final RegistryObject<SoundEvent> ISOLATION_WARNING = register("isolation_warning");
    public static final RegistryObject<SoundEvent> FLESH_MOTHER_GRUM = register("flesh_mother_grum");
    public static final RegistryObject<SoundEvent> DARK_MATTER_SWORD = register("dark_matter_sword");


    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(LiberthiaMod.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
