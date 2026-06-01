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

    // ════════════════════════════════════════════════════════════════════════
    // r34: SONS NOVOS — todos os .ogg que o user criou no ElevenLabs
    // ════════════════════════════════════════════════════════════════════════
    // Loom dimension
    public static final RegistryObject<SoundEvent> LOOM_AMBIENT = register("ambient");
    public static final RegistryObject<SoundEvent> WATCHER_STEP = register("watcher_step");
    public static final RegistryObject<SoundEvent> WATCHER_BREATH = register("watcher_breath");
    public static final RegistryObject<SoundEvent> PERIPHERAL_WHISPER = register("peripheral_whisper");
    public static final RegistryObject<SoundEvent> PERIPHERAL_BLIND = register("peripheral_blind");
    public static final RegistryObject<SoundEvent> SCREAMER_TELEPORT = register("screamer_teleport");
    public static final RegistryObject<SoundEvent> SCREAMER_SCREAM = register("screamer_scream");
    public static final RegistryObject<SoundEvent> WORM_BITE = register("worm_bite");
    public static final RegistryObject<SoundEvent> PORTAL_AMBIENT = register("portal_ambient");
    // Cosmic horror set
    public static final RegistryObject<SoundEvent> COSMIC_WHISPER_1 = register("cosmic.whisper_1");
    public static final RegistryObject<SoundEvent> COSMIC_WHISPER_2 = register("cosmic.whisper_2");
    public static final RegistryObject<SoundEvent> COSMIC_WHISPER_3 = register("cosmic.whisper_3");
    public static final RegistryObject<SoundEvent> COSMIC_AZATHOTH_EYE = register("cosmic.azathoth_eye");
    public static final RegistryObject<SoundEvent> COSMIC_GLITCH = register("cosmic.glitch");
    public static final RegistryObject<SoundEvent> COSMIC_DIMENSIONAL_TEAR = register("cosmic.dimensional_tear");

    // ════════════════════════════════════════════════════════════════════════
    // r52: Cosmic Sound Framework — 10 horror sounds com variants
    // (cada um pode ter múltiplos .ogg files — sounds.json escolhe random)
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<SoundEvent> COSMIC_DISTANT_WHISPERS = register("cosmic.distant_whispers");
    public static final RegistryObject<SoundEvent> COSMIC_EYE_PULSE = register("cosmic.eye_pulse");
    public static final RegistryObject<SoundEvent> COSMIC_FALSE_FOOTSTEPS = register("cosmic.false_footsteps");
    public static final RegistryObject<SoundEvent> COSMIC_VOID_BREATHING = register("cosmic.void_breathing");
    public static final RegistryObject<SoundEvent> COSMIC_RADIO_BROADCAST = register("cosmic.radio_broadcast");
    public static final RegistryObject<SoundEvent> COSMIC_SKY_HUM = register("cosmic.sky_hum");
    public static final RegistryObject<SoundEvent> COSMIC_TENDRIL_MOVEMENT = register("cosmic.tendril_movement");
    public static final RegistryObject<SoundEvent> COSMIC_REALITY_DISTORTION = register("cosmic.reality_distortion");
    public static final RegistryObject<SoundEvent> COSMIC_DISTANT_SCREAM = register("cosmic.distant_scream");
    public static final RegistryObject<SoundEvent> COSMIC_AUDIENCE_PRESENCE = register("cosmic.audience_presence");

    // >>> gen_horror_ambience (auto) — nao editar a mao
    public static final RegistryObject<SoundEvent> AMBIENCE_DREAD_DRONE = register("ambience.dread_drone");
    public static final RegistryObject<SoundEvent> AMBIENCE_SUB_RUMBLE = register("ambience.sub_rumble");
    public static final RegistryObject<SoundEvent> AMBIENCE_SKY_HUM = register("ambience.sky_hum");
    public static final RegistryObject<SoundEvent> AMBIENCE_VOID_BREATHING = register("ambience.void_breathing");
    public static final RegistryObject<SoundEvent> AMBIENCE_WIND_HOWL = register("ambience.wind_howl");
    public static final RegistryObject<SoundEvent> AMBIENCE_AUDIENCE_PRESENCE = register("ambience.audience_presence");
    public static final RegistryObject<SoundEvent> AMBIENCE_DISTANT_WHISPERS = register("ambience.distant_whispers");
    public static final RegistryObject<SoundEvent> AMBIENCE_GRANULAR_TEXTURE = register("ambience.granular_texture");
    public static final RegistryObject<SoundEvent> AMBIENCE_HEARTBEAT = register("ambience.heartbeat");
    public static final RegistryObject<SoundEvent> AMBIENCE_FALSE_FOOTSTEPS = register("ambience.false_footsteps");
    public static final RegistryObject<SoundEvent> AMBIENCE_RISER = register("ambience.riser");
    public static final RegistryObject<SoundEvent> AMBIENCE_REVERSE_STINGER = register("ambience.reverse_stinger");
    public static final RegistryObject<SoundEvent> AMBIENCE_STINGER_HIT = register("ambience.stinger_hit");
    public static final RegistryObject<SoundEvent> AMBIENCE_METALLIC_GROAN = register("ambience.metallic_groan");
    public static final RegistryObject<SoundEvent> AMBIENCE_BELL_TOLL = register("ambience.bell_toll");
    public static final RegistryObject<SoundEvent> AMBIENCE_STATIC_BURST = register("ambience.static_burst");
    public static final RegistryObject<SoundEvent> AMBIENCE_TENDRIL_MOVEMENT = register("ambience.tendril_movement");
    // <<< gen_horror_ambience

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
