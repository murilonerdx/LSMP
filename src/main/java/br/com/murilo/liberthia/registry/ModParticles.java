package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LiberthiaMod.MODID);

    public static final RegistryObject<SimpleParticleType> DARK_BLOOD =
            PARTICLE_TYPES.register("dark_blood", () -> new SimpleParticleType(false));


    public static final RegistryObject<ConfigurableParticleType> ENGINE_PARTICLE =
            PARTICLE_TYPES.register("engine_particle", () -> new ConfigurableParticleType(false));

    // r113: AAA spell trail particle (animated spritesheet per school)
    public static final RegistryObject<br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleType> SPELL_TRAIL =
            PARTICLE_TYPES.register("spell_trail",
                    br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleType::new);

    // r35: COSMIC HORROR custom particles
    public static final RegistryObject<SimpleParticleType> COSMIC_ORBIT =
            PARTICLE_TYPES.register("cosmic_orbit", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> VOID_LEAK =
            PARTICLE_TYPES.register("void_leak", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> RIFT_SPARK =
            PARTICLE_TYPES.register("rift_spark", () -> new SimpleParticleType(true));

    // r41: COSMIC COLLAPSE — particles AAA-quality procedurais
    /** Gravitational singularity — orbita+espirala pra dentro, lensing visual. */
    public static final RegistryObject<SimpleParticleType> GRAVITY_SINGULARITY =
            PARTICLE_TYPES.register("gravity_singularity", () -> new SimpleParticleType(true));
    /** Orbital debris — elipse com tilt, wobble vertical. */
    public static final RegistryObject<SimpleParticleType> ORBITAL_DEBRIS =
            PARTICLE_TYPES.register("orbital_debris", () -> new SimpleParticleType(true));
    /** Dimensional crack — rachadura preta com chromatic shimmer. */
    public static final RegistryObject<SimpleParticleType> DIMENSIONAL_CRACK =
            PARTICLE_TYPES.register("dimensional_crack", () -> new SimpleParticleType(true));
    /** Collapse shockwave — anel expansivo pós-implosão. */
    public static final RegistryObject<SimpleParticleType> COLLAPSE_SHOCKWAVE =
            PARTICLE_TYPES.register("collapse_shockwave", () -> new SimpleParticleType(true));

    // r42: CUSTOM SPELL VFX — 10 sprite particles animados pra spell crafting system
    public static final RegistryObject<SimpleParticleType> SPELL_FIRE_BLAST =
            PARTICLE_TYPES.register("spell_fire_blast", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_ICE_LANCE =
            PARTICLE_TYPES.register("spell_ice_lance", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_VOID_ORB =
            PARTICLE_TYPES.register("spell_void_orb", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_LIGHT_RAY =
            PARTICLE_TYPES.register("spell_light_ray", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_BLOOD_SHOT =
            PARTICLE_TYPES.register("spell_blood_shot", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_ARCANE_MISSILE =
            PARTICLE_TYPES.register("spell_arcane_missile", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_EARTH_SPIKE =
            PARTICLE_TYPES.register("spell_earth_spike", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_LIGHTNING_BOLT =
            PARTICLE_TYPES.register("spell_lightning_bolt", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_SHADOW_DART =
            PARTICLE_TYPES.register("spell_shadow_dart", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> SPELL_NATURE_THORN =
            PARTICLE_TYPES.register("spell_nature_thorn", () -> new SimpleParticleType(true));

    // r45: COSMIC HORROR v2 — 6 partículas animadas extras
    public static final RegistryObject<SimpleParticleType> TENTACLE_WRITHE =
            PARTICLE_TYPES.register("tentacle_writhe", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> VULTO_SHADOW =
            PARTICLE_TYPES.register("vulto_shadow", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> GLARING_EYE_PULSE =
            PARTICLE_TYPES.register("glaring_eye_pulse", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> CURSED_PULSE =
            PARTICLE_TYPES.register("cursed_pulse", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> WHISPER_WISP =
            PARTICLE_TYPES.register("whisper_wisp", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> DEMON_GLYPH =
            PARTICLE_TYPES.register("demon_glyph", () -> new SimpleParticleType(true));

    // r63: OBSERVATION CASTING — custom particle com cor RGB + alpha + lifetime
    // Pattern de AN's GlowParticleType: ParticleType<GlowData>.
    public static final RegistryObject<ParticleType<br.com.murilo.liberthia.observation.particle.GlowData>> OBSERVATION_GLOW =
            PARTICLE_TYPES.register("observation_glow",
                    () -> new br.com.murilo.liberthia.observation.particle.GlowParticleType());

    // r64: Line particle — beam que interpola start → dest com fade
    public static final RegistryObject<ParticleType<br.com.murilo.liberthia.observation.particle.LineData>> OBSERVATION_LINE =
            PARTICLE_TYPES.register("observation_line",
                    () -> new br.com.murilo.liberthia.observation.particle.LineParticleType());

    // r120: VOID INFECTION particle — purple animated wisp (8 frames)
    public static final RegistryObject<SimpleParticleType> VOID_INFECTION =
            PARTICLE_TYPES.register("void_infection", () -> new SimpleParticleType(true));

    // r120: MINI BLACK HOLE explosion particle — collapse spritesheet (6 frames)
    public static final RegistryObject<SimpleParticleType> MINI_BLACK_HOLE =
            PARTICLE_TYPES.register("mini_black_hole", () -> new SimpleParticleType(true));

    // r178: Manifestação do Vazio — olho vermelho que VOA + fumaça-sombra (custom, não-vanilla)
    public static final RegistryObject<SimpleParticleType> VOID_FLYING_EYE =
            PARTICLE_TYPES.register("void_flying_eye", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> VOID_SHADOW_WISP =
            PARTICLE_TYPES.register("void_shadow_wisp", () -> new SimpleParticleType(true));

    // Névoa de terror — partícula translúcida colorida (blending normal, não additive)
    public static final RegistryObject<ParticleType<br.com.murilo.liberthia.fog.particle.FogMistData>> FOG_MIST =
            PARTICLE_TYPES.register("fog_mist",
                    () -> new br.com.murilo.liberthia.fog.particle.FogMistParticleType());

    private ModParticles() {
    }
}