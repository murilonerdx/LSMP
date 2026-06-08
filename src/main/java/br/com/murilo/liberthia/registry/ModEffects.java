package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.effect.BloodInfectionEffect;
import br.com.murilo.liberthia.effect.ClearShieldEffect;
import br.com.murilo.liberthia.effect.DarkInfectionEffect;
import br.com.murilo.liberthia.effect.MatterResistanceEffect;
import br.com.murilo.liberthia.effect.RadiationSicknessEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LiberthiaMod.MODID);

    public static final RegistryObject<MobEffect> DARK_INFECTION =
            MOB_EFFECTS.register("dark_infection", DarkInfectionEffect::new);

    // r180: Antimagia — o selo do sistema anti-magia (sem voo, magia anulada, lento)
    public static final RegistryObject<MobEffect> ANTIMAGIA =
            MOB_EFFECTS.register("antimagia", br.com.murilo.liberthia.effect.AntimagiaEffect::new);

    public static final RegistryObject<MobEffect> RADIATION_SICKNESS =
            MOB_EFFECTS.register("radiation_sickness", RadiationSicknessEffect::new);

    public static final RegistryObject<MobEffect> CLEAR_SHIELD =
            MOB_EFFECTS.register("clear_shield", ClearShieldEffect::new);

    public static final RegistryObject<MobEffect> BLOOD_INFECTION =
            MOB_EFFECTS.register("blood_infection", BloodInfectionEffect::new);

    public static final RegistryObject<MobEffect> SANGUINE_VITALITY =
            MOB_EFFECTS.register("sanguine_vitality",
                    br.com.murilo.liberthia.effect.SanguineVitalityEffect::new);

    public static final RegistryObject<MobEffect> BLOOD_FRENZY =
            MOB_EFFECTS.register("blood_frenzy",
                    br.com.murilo.liberthia.effect.BloodFrenzyEffect::new);

    public static final RegistryObject<MobEffect> HEMO_SICKNESS =
            MOB_EFFECTS.register("hemo_sickness",
                    br.com.murilo.liberthia.effect.HemoSicknessEffect::new);

    public static final RegistryObject<MobEffect> INFECTED_SIGHT =
            MOB_EFFECTS.register("infected_sight",
                    br.com.murilo.liberthia.effect.InfectedSightEffect::new);

    public static final RegistryObject<MobEffect> BLOOD_STEP =
            MOB_EFFECTS.register("blood_step",
                    br.com.murilo.liberthia.effect.BloodStepEffect::new);

    public static final RegistryObject<MobEffect> FEATHER_FALL =
            MOB_EFFECTS.register("feather_fall",
                    br.com.murilo.liberthia.effect.FeatherFallEffect::new);

    // v0.1.51: resistências a matter — efeitos marker aplicados pelas pílulas
    // (30 min cada). Bloqueiam ganho de matter no perfil + atenuam efeitos
    // negativos da exposição enquanto ativos. NÃO removem matter acumulada.
    public static final RegistryObject<MobEffect> DARK_MATTER_RESISTANCE =
            MOB_EFFECTS.register("dark_matter_resistance",
                    () -> new MatterResistanceEffect(0xAA60FF)); // violeta

    public static final RegistryObject<MobEffect> CLEAR_MATTER_RESISTANCE =
            MOB_EFFECTS.register("clear_matter_resistance",
                    () -> new MatterResistanceEffect(0xB0E8FF)); // branco perolado

    public static final RegistryObject<MobEffect> YELLOW_MATTER_RESISTANCE =
            MOB_EFFECTS.register("yellow_matter_resistance",
                    () -> new MatterResistanceEffect(0xFFD23F)); // dourado

    // ════════════════════════════════════════════════════════════════════════
    // v0.1.22 r33: LOOM DIMENSION EFFECTS — obsession, madness, dim infection
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<MobEffect> OBSESSION =
            MOB_EFFECTS.register("obsession",
                    br.com.murilo.liberthia.loom.effect.LoomEffects.ObsessionEffect::new);

    public static final RegistryObject<MobEffect> MADNESS =
            MOB_EFFECTS.register("madness",
                    br.com.murilo.liberthia.loom.effect.LoomEffects.MadnessEffect::new);

    public static final RegistryObject<MobEffect> DIMENSIONAL_INFECTION =
            MOB_EFFECTS.register("dimensional_infection",
                    br.com.murilo.liberthia.loom.effect.LoomEffects.DimensionalInfectionEffect::new);

    // ════════════════════════════════════════════════════════════════════════
    // r156: 20 Custom MobEffects — divertidos/criativos/assustadores
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<MobEffect> DIMENSIONAL_BLINDNESS =
            MOB_EFFECTS.register("dimensional_blindness", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.DimensionalBlindness::new);
    public static final RegistryObject<MobEffect> DEVIL_FOOTSTEPS =
            MOB_EFFECTS.register("devil_footsteps", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.DevilFootsteps::new);
    public static final RegistryObject<MobEffect> BLOOD_MOON_AURA =
            MOB_EFFECTS.register("blood_moon_aura", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.BloodMoon::new);
    public static final RegistryObject<MobEffect> SHADOW_DOUBLE =
            MOB_EFFECTS.register("shadow_double", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.ShadowDouble::new);
    public static final RegistryObject<MobEffect> WHISPERS =
            MOB_EFFECTS.register("cosmic_whispers", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.Whispers::new);
    public static final RegistryObject<MobEffect> VERTIGO =
            MOB_EFFECTS.register("vertigo", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.Vertigo::new);
    public static final RegistryObject<MobEffect> HUNGRY_VOID =
            MOB_EFFECTS.register("hungry_void", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.HungryVoid::new);
    public static final RegistryObject<MobEffect> GHOST_TOUCH =
            MOB_EFFECTS.register("ghost_touch", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.GhostTouch::new);
    public static final RegistryObject<MobEffect> SOUL_LINK =
            MOB_EFFECTS.register("soul_link", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.SoulLink::new);
    public static final RegistryObject<MobEffect> HAUNTED_INVENTORY =
            MOB_EFFECTS.register("haunted_inventory", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.HauntedInventory::new);
    public static final RegistryObject<MobEffect> MIRROR_WALK =
            MOB_EFFECTS.register("mirror_walk", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.MirrorWalk::new);
    public static final RegistryObject<MobEffect> TIME_DILATION =
            MOB_EFFECTS.register("time_dilation", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.TimeDilation::new);
    public static final RegistryObject<MobEffect> REVERSE_GRAVITY =
            MOB_EFFECTS.register("reverse_gravity", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.ReverseGravity::new);
    public static final RegistryObject<MobEffect> MAGNET_FIST =
            MOB_EFFECTS.register("magnet_fist", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.MagnetFist::new);
    public static final RegistryObject<MobEffect> POX_SWARM =
            MOB_EFFECTS.register("pox_swarm", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.PoxSwarm::new);
    public static final RegistryObject<MobEffect> NIGHTMARE =
            MOB_EFFECTS.register("nightmare", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.Nightmare::new);
    public static final RegistryObject<MobEffect> LIBERTHIA_BLESSING =
            MOB_EFFECTS.register("liberthia_blessing", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.LiberthiaBlessing::new);
    public static final RegistryObject<MobEffect> CRYSTAL_BLOOM =
            MOB_EFFECTS.register("crystal_bloom", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.CrystalBloom::new);
    public static final RegistryObject<MobEffect> OMINOUS_AURA =
            MOB_EFFECTS.register("ominous_aura", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.OminousAura::new);
    public static final RegistryObject<MobEffect> STARDUST =
            MOB_EFFECTS.register("stardust", br.com.murilo.liberthia.effect.r156.CustomEffectsR156.Stardust::new);

    // r184 — Doenças Dimensionais (permanentes até o Soro Dimensional)
    public static final RegistryObject<MobEffect> DIMENSIONAL_MEMORY =
            MOB_EFFECTS.register("dimensional_memory", br.com.murilo.liberthia.effect.DimensionalMemoryEffect::new);
    // "dimensional_infection" já é usado pelo sistema loom; a doença usa o id "dimensional_blight"
    public static final RegistryObject<MobEffect> DIMENSIONAL_BLIGHT =
            MOB_EFFECTS.register("dimensional_blight", br.com.murilo.liberthia.effect.DimensionalInfectionEffect::new);
    public static final RegistryObject<MobEffect> DIMENSIONAL_PARANOIA =
            MOB_EFFECTS.register("dimensional_paranoia", br.com.murilo.liberthia.effect.DimensionalParanoiaEffect::new);

    // r185 — Radiação Dimensional (emanada pela Adaga Corta-Fendas)
    public static final RegistryObject<MobEffect> DIMENSIONAL_RADIATION =
            MOB_EFFECTS.register("dimensional_radiation", br.com.murilo.liberthia.effect.DimensionalRadiationEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
