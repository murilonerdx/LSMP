package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, LiberthiaMod.MODID);

    public static final RegistryObject<Potion> HEMORRHAGE =
            POTIONS.register("hemorrhage",
                    () -> new Potion("hemorrhage",
                            new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 200, 0)));

    public static final RegistryObject<Potion> SANGUINE_VITALITY =
            POTIONS.register("sanguine_vitality",
                    () -> new Potion("sanguine_vitality",
                            new MobEffectInstance(ModEffects.SANGUINE_VITALITY.get(), 2400, 0)));

    public static final RegistryObject<Potion> BLOODLUST =
            POTIONS.register("bloodlust",
                    () -> new Potion("bloodlust",
                            new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 1800, 1),
                            new MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 1800, 0)));

    public static final RegistryObject<Potion> ANEMIA =
            POTIONS.register("anemia",
                    () -> new Potion("anemia",
                            new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 1800, 1),
                            new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 1800, 1)));

    private ModPotions() {}

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
