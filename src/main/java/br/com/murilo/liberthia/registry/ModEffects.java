package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.effect.BloodInfectionEffect;
import br.com.murilo.liberthia.effect.ClearShieldEffect;
import br.com.murilo.liberthia.effect.DarkInfectionEffect;
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

    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
