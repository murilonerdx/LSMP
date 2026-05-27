package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Os 5 efeitos correspondentes aos perfis de matéria.
 *
 * <p>Cada efeito é apenas um marcador visual (ícone, cor, nome) — a
 * lógica real (modificação de stats, animações, etc.) é aplicada pelo
 * tick handler em {@code MatterProfileEvents}.
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LiberthiaMod.MODID);

    /** DM puro — selvagem, agressivo, coceira, tamanho variável. */
    public static final RegistryObject<MobEffect> AGGRESSION = EFFECTS.register("aggression",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x9C2C77));

    /** DM + WM — contido, manipulável. */
    public static final RegistryObject<MobEffect> CONTAINED = EFFECTS.register("contained",
            () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0x6E40C9));

    /** WM puro — inteligente mas com lapsos. */
    public static final RegistryObject<MobEffect> FORGETFULNESS = EFFECTS.register("forgetfulness",
            () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0xE6E6FF));

    /** YM puro — caótico, alucinações, descontrole emocional. */
    public static final RegistryObject<MobEffect> EMOTIONAL_CHAOS = EFFECTS.register("emotional_chaos",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xFFD23F));

    /** YM + WM — estrategista frio. */
    public static final RegistryObject<MobEffect> COLD_FOCUS = EFFECTS.register("cold_focus",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFEC8B));

    /** r120: Void Infection — drain HP, spawn larvas, partículas roxas. */
    public static final RegistryObject<MobEffect> VOID_INFECTION = EFFECTS.register("void_infection",
            () -> new br.com.murilo.liberthia.magic.spell.voidspell.VoidInfectionEffect());

    // ════════════════════════════════════════════════════════════════════════
    // r151: 15 Custom Magic + Cosmic Horror Effects
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<MobEffect> VOID_TOUCH = EFFECTS.register("void_touch",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.VoidTouchEffect());
    public static final RegistryObject<MobEffect> WHISPERS = EFFECTS.register("whispers",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.WhispersEffect());
    public static final RegistryObject<MobEffect> STATIC_VISION = EFFECTS.register("static_vision",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.StaticVisionEffect());
    public static final RegistryObject<MobEffect> TIME_FRACTURE = EFFECTS.register("time_fracture",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.TimeFractureEffect());
    public static final RegistryObject<MobEffect> HOLLOW_HUNGER = EFFECTS.register("hollow_hunger",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.HollowHungerEffect());
    public static final RegistryObject<MobEffect> AETHERIC_SHIFT = EFFECTS.register("aetheric_shift",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.AethericShiftEffect());
    public static final RegistryObject<MobEffect> SOUL_BLEED = EFFECTS.register("soul_bleed",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.SoulBleedEffect());
    public static final RegistryObject<MobEffect> MANA_SURGE = EFFECTS.register("mana_surge",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.ManaSurgeEffect());
    public static final RegistryObject<MobEffect> ARCANE_WARD = EFFECTS.register("arcane_ward",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.ArcaneWardEffect());
    public static final RegistryObject<MobEffect> ASTRAL_SIGHT = EFFECTS.register("astral_sight",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.AstralSightEffect());
    public static final RegistryObject<MobEffect> ELEMENTAL_RESONANCE = EFFECTS.register("elemental_resonance",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.ElementalResonanceEffect());
    public static final RegistryObject<MobEffect> MIND_FORTRESS = EFFECTS.register("mind_fortress",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.MindFortressEffect());
    public static final RegistryObject<MobEffect> CHRONOSURGE = EFFECTS.register("chronosurge",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.ChronosurgeEffect());
    public static final RegistryObject<MobEffect> VOID_ARMOR = EFFECTS.register("void_armor",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.VoidArmorEffect());
    public static final RegistryObject<MobEffect> DRAGON_BREATH_EFFECT = EFFECTS.register("dragon_breath",
            () -> new br.com.murilo.liberthia.magic.effect.custom.CustomMagicEffects.DragonBreathEffect());

    private ModMobEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static final class SimpleEffect extends MobEffect {
        public SimpleEffect(MobEffectCategory cat, int color) {
            super(cat, color);
        }
        @Override public boolean isInstantenous() { return false; }
    }
}
