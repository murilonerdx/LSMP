package br.com.murilo.liberthia.observation;

import br.com.murilo.liberthia.observation.api.ObservationRegistry;
import br.com.murilo.liberthia.observation.parts.AmplifyDistortion;
import br.com.murilo.liberthia.observation.parts.DirectGazeMethod;
import br.com.murilo.liberthia.observation.parts.TendrilManifestation;

/**
 * v0.1.22 r60: Bootstrap dos parts foundationais. Chamado uma vez no setup
 * do mod.
 *
 * <p>Cada part é um singleton — uma instance, registrada com seu ID, e usada
 * em todos os Spells que a referenciam.
 */
public final class ObservationParts {

    // r60 foundationais
    public static final DirectGazeMethod DIRECT_GAZE =
            ObservationRegistry.register(new DirectGazeMethod());
    public static final TendrilManifestation TENDRIL =
            ObservationRegistry.register(new TendrilManifestation());
    public static final AmplifyDistortion AMPLIFY =
            ObservationRegistry.register(new AmplifyDistortion());

    // r61: 4 novos WatchMethods
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.PeripheralMethod PERIPHERAL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.PeripheralMethod());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.MemoryMethod MEMORY =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.MemoryMethod());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.SilenceMethod SILENCE_WATCH =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.SilenceMethod());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.ReflectionMethod REFLECTION =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.ReflectionMethod());

    // r61: 5 novas Manifestations
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.SilenceManifestation SILENCE_MANIFEST =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.SilenceManifestation());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.MirrorManifestation MIRROR =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.MirrorManifestation());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.WhisperManifestation WHISPER =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.WhisperManifestation());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.DecayManifestation DECAY =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.DecayManifestation());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.GlimpseManifestation GLIMPSE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.GlimpseManifestation());

    // r61: 3 novas Distortions
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.LingerDistortion LINGER =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.LingerDistortion());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.EchoDistortion ECHO =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.EchoDistortion());
    public static final br.com.murilo.liberthia.observation.parts.MoreParts.SecretDistortion SECRET =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.MoreParts.SecretDistortion());

    // ═══════════════════════════════════════════════════════════════════════
    // r68: 8 ObservationParts portados DIRETO do Ars Nouveau source
    // ═══════════════════════════════════════════════════════════════════════

    // 2 Methods (port AbstractCastMethod)
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.TouchMethod TOUCH =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.TouchMethod());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.SelfMethod SELF =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.SelfMethod());

    // 6 Effects (port AbstractEffect)
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.IgniteEffect IGNITE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.IgniteEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.HarmEffect HARM =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.HarmEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.HealEffect HEAL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.HealEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.FreezeEffect FREEZE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.FreezeEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.LaunchEffect LAUNCH =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.LaunchEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts.SlowfallEffect SLOWFALL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts.SlowfallEffect());

    // ═══════════════════════════════════════════════════════════════════════
    // r70: 12 ObservationParts ADICIONAIS portados do AN
    // 5 Methods + 7 Effects
    // ═══════════════════════════════════════════════════════════════════════

    // 5 Methods
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.LaserMethod LASER =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.LaserMethod());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.BurstMethod BURST =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.BurstMethod());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.OrbitMethod ORBIT =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.OrbitMethod());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.WallMethod WALL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.WallMethod());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.ChainMethod CHAIN =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.ChainMethod());

    // 7 Effects
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.LightningEffect LIGHTNING =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.LightningEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.GravityEffect GRAVITY =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.GravityEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.BlindEffect BLIND =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.BlindEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.LevitateEffect LEVITATE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.LevitateEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.KnockbackEffect KNOCKBACK =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.KnockbackEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.ExplosionEffect EXPLOSION =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.ExplosionEffect());
    public static final br.com.murilo.liberthia.observation.parts.AnPorts2.FangsEffect FANGS =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.AnPorts2.FangsEffect());

    // ═══════════════════════════════════════════════════════════════════════
    // r71: 25 ElementalSpells — classificados em 5 classes (Fire/Water/Earth/Air/Cosmic)
    // ═══════════════════════════════════════════════════════════════════════

    // FIRE (5)
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.FireballEffect FIREBALL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.FireballEffect());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.InfernoEffect INFERNO =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.InfernoEffect());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.CleansingFlame CLEANSING_FLAME =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.CleansingFlame());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.SolarPulse SOLAR_PULSE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.SolarPulse());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.BurningAura BURNING_AURA =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.BurningAura());

    // WATER (5)
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.BubbleShield BUBBLE_SHIELD =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.BubbleShield());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.TidalWave TIDAL_WAVE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.TidalWave());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.FrostLance FROST_LANCE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.FrostLance());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.MistVeil MIST_VEIL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.MistVeil());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.HealingRain HEALING_RAIN =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.HealingRain());

    // EARTH (5)
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.StoneSpikes STONE_SPIKES =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.StoneSpikes());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.QuakeStep QUAKE_STEP =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.QuakeStep());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.VeinSight VEIN_SIGHT =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.VeinSight());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.EarthenWall EARTHEN_WALL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.EarthenWall());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.RootsEffect ROOTS =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.RootsEffect());

    // AIR (5)
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.GustEffect GUST =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.GustEffect());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.TornadoEffect TORNADO =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.TornadoEffect());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.SkyStep SKY_STEP =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.SkyStep());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.VelocityEffect VELOCITY =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.VelocityEffect());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.WindCutter WIND_CUTTER =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.WindCutter());

    // COSMIC (5)
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.VoidPull VOID_PULL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.VoidPull());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.DreadStare DREAD_STARE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.DreadStare());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.MindSpike MIND_SPIKE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.MindSpike());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.RealityTear REALITY_TEAR =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.RealityTear());
    public static final br.com.murilo.liberthia.observation.parts.ElementalSpells.SingularityEffect SINGULARITY =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.ElementalSpells.SingularityEffect());

    // ═══════════════════════════════════════════════════════════════════════
    // r72: 10 UtilityGlyphs (7 effects + 3 augments) portados do AN
    // ═══════════════════════════════════════════════════════════════════════
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.PlaceBlockEffect PLACE_BLOCK =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.PlaceBlockEffect());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.BreakBlockEffect BREAK_BLOCK =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.BreakBlockEffect());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.ConjureWaterEffect CONJURE_WATER =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.ConjureWaterEffect());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.LightEffect LIGHT =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.LightEffect());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.SnareEffect SNARE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.SnareEffect());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.HexEffect HEX =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.HexEffect());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.PickupEffect PICKUP =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.PickupEffect());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.PierceAugment PIERCE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.PierceAugment());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.SplitAugment SPLIT =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.SplitAugment());
    public static final br.com.murilo.liberthia.observation.parts.UtilityGlyphs.AoeAugment AOE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.UtilityGlyphs.AoeAugment());

    // ═══════════════════════════════════════════════════════════════════════
    // r172: 8 glifos novos e divertidos (Manifestações com efeito real)
    // ═══════════════════════════════════════════════════════════════════════
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.VampiricGaze VAMPIRIC_GAZE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.VampiricGaze());
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.GravityPull GRAVITY_PULL =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.GravityPull());
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Repulse REPULSE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Repulse());
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Petrify PETRIFY =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Petrify());
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Soothe SOOTHE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Soothe());
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Scorch SCORCH =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Scorch());
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Updraft UPDRAFT =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Updraft());
    public static final br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Enfeeble ENFEEBLE =
            ObservationRegistry.register(new br.com.murilo.liberthia.observation.parts.CustomGlyphsR172.Enfeeble());

    private ObservationParts() {}

    /** Chama uma vez no setup pra garantir que a class loaded. */
    public static void init() {
        // No-op — só força init da class
    }
}
