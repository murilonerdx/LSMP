package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * v0.1.162 r138: <b>ModFeatures</b> — registra worldgen features customizadas.
 */
public final class ModFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, LiberthiaMod.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> WIZARD_TOWER =
            FEATURES.register("wizard_tower",
                    () -> new br.com.murilo.liberthia.worldgen.WizardTowerFeature(
                            NoneFeatureConfiguration.CODEC));

    private ModFeatures() {}

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
