package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.menu.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, LiberthiaMod.MODID);

    public static final RegistryObject<MenuType<SpiritualTradeMenu>> SPIRITUAL_TRADE =
            MENUS.register("spiritual_trade", () ->
                    IForgeMenuType.create((windowId, inventory, data) ->
                            new SpiritualTradeMenu(windowId, inventory, ItemStack.EMPTY)));

    public static final RegistryObject<MenuType<SpiritualTradeConfigMenu>> SPIRITUAL_TRADE_CONFIG =
            MENUS.register("spiritual_trade_config", () ->
                    IForgeMenuType.create((windowId, inventory, data) ->
                            new SpiritualTradeConfigMenu(windowId, inventory, ItemStack.EMPTY)));

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

    public static final RegistryObject<MenuType<DarkMatterAlchemizerMenu>> DARK_MATTER_ALCHEMIZER = registerMenuType(
            "dark_matter_alchemizer", DarkMatterAlchemizerMenu::new);

    public static final RegistryObject<MenuType<DarkMatterGeneratorMenu>> DARK_MATTER_GENERATOR = registerMenuType(
            "dark_matter_generator", DarkMatterGeneratorMenu::new);

    public static final RegistryObject<MenuType<DarkMatterChestMenu>> DARK_MATTER_CHEST = registerMenuType(
            "dark_matter_chest", DarkMatterChestMenu::new);

    public static final RegistryObject<MenuType<FragmentedGeneratorMenu>> FRAGMENTED_GENERATOR = registerMenuType(
            "fragmented_generator", FragmentedGeneratorMenu::new);

    public static final RegistryObject<MenuType<CrystallizerMenu>> CRYSTALLIZER = registerMenuType(
            "crystallizer", CrystallizerMenu::new);

    public static final RegistryObject<MenuType<AutoFarmerMenu>> AUTO_FARMER = registerMenuType(
            "auto_farmer", AutoFarmerMenu::new);

    public static final RegistryObject<MenuType<MatterAnalyzerMenu>> MATTER_ANALYZER = registerMenuType(
            "matter_analyzer", MatterAnalyzerMenu::new);

    public static final RegistryObject<MenuType<DimensionalExtractorMenu>> DIMENSIONAL_EXTRACTOR = registerMenuType(
            "dimensional_extractor", DimensionalExtractorMenu::new);

    public static final RegistryObject<MenuType<DarkMatterBatteryMenu>> DARK_MATTER_BATTERY = registerMenuType(
            "dark_matter_battery", DarkMatterBatteryMenu::new);

    public static final RegistryObject<MenuType<DimensionalChestMenu>> DIMENSIONAL_CHEST = registerMenuType(
            "dimensional_chest", DimensionalChestMenu::new);

    public static final RegistryObject<MenuType<MatterRefinerMenu>> MATTER_REFINER = registerMenuType(
            "matter_refiner", MatterRefinerMenu::new);

    public static final RegistryObject<MenuType<WirelessChargerMenu>> WIRELESS_CHARGER = registerMenuType(
            "wireless_charger", WirelessChargerMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    private ModMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
