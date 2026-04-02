package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.menu.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, LiberthiaMod.MODID);

    public static final RegistryObject<MenuType<PurificationBenchMenu>> PURIFICATION_BENCH = registerMenuType(
            "purification_bench", PurificationBenchMenu::new);

    public static final RegistryObject<MenuType<DarkMatterForgeMenu>> DARK_MATTER_FORGE = registerMenuType(
            "dark_matter_forge", DarkMatterForgeMenu::new);

    public static final RegistryObject<MenuType<MatterInfuserMenu>> MATTER_INFUSER = registerMenuType(
            "matter_infuser", MatterInfuserMenu::new);

    public static final RegistryObject<MenuType<ResearchTableMenu>> RESEARCH_TABLE = registerMenuType(
            "research_table", ResearchTableMenu::new);

    public static final RegistryObject<MenuType<ContainmentChamberMenu>> CONTAINMENT_CHAMBER = registerMenuType(
            "containment_chamber", ContainmentChamberMenu::new);

    public static final RegistryObject<MenuType<MatterTransmuterMenu>> MATTER_TRANSMUTER = registerMenuType(
            "matter_transmuter", MatterTransmuterMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
