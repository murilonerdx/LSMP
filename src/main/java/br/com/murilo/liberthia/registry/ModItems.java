package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LiberthiaMod.MODID);

    public static final RegistryObject<Item> DARK_MATTER_BLOCK_ITEM = ITEMS.register("dark_matter_block",
            () -> new BlockItem(ModBlocks.DARK_MATTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_BLOCK_ITEM = ITEMS.register("clear_matter_block",
            () -> new BlockItem(ModBlocks.CLEAR_MATTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_MATTER_BLOCK_ITEM = ITEMS.register("yellow_matter_block",
            () -> new BlockItem(ModBlocks.YELLOW_MATTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_SOIL_ITEM = ITEMS.register("corrupted_soil",
            () -> new BlockItem(ModBlocks.CORRUPTED_SOIL.get(), new Item.Properties()));

    public static final RegistryObject<Item> DARK_MATTER_BUCKET = ITEMS.register("dark_matter_bucket",
            () -> new BucketItem(ModFluids.DARK_MATTER.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    public static final RegistryObject<Item> CLEAR_MATTER_INJECTOR = ITEMS.register("clear_matter_injector",
            () -> new ClearMatterInjectorItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> YELLOW_MATTER_INGOT = ITEMS.register("yellow_matter_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_MATTER_HELMET = ITEMS.register("yellow_matter_helmet",
            () -> new ArmorItem(YellowMatterArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_MATTER_CHESTPLATE = ITEMS.register("yellow_matter_chestplate",
            () -> new ArmorItem(YellowMatterArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_MATTER_LEGGINGS = ITEMS.register("yellow_matter_leggings",
            () -> new ArmorItem(YellowMatterArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_MATTER_BOOTS = ITEMS.register("yellow_matter_boots",
            () -> new ArmorItem(YellowMatterArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));

    // --- Ores ---
    public static final RegistryObject<Item> DARK_MATTER_ORE_ITEM = ITEMS.register("dark_matter_ore",
            () -> new BlockItem(ModBlocks.DARK_MATTER_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_DARK_MATTER_ORE_ITEM = ITEMS.register("deepslate_dark_matter_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> WHITE_MATTER_ORE_ITEM = ITEMS.register("white_matter_ore",
            () -> new BlockItem(ModBlocks.WHITE_MATTER_ORE.get(), new Item.Properties()));

    // --- Machines & Defense ---
    public static final RegistryObject<Item> PURIFICATION_BENCH_ITEM = ITEMS.register("purification_bench",
            () -> new BlockItem(ModBlocks.PURIFICATION_BENCH.get(), new Item.Properties()));

    public static final RegistryObject<Item> PURITY_BEACON_ITEM = ITEMS.register("purity_beacon",
            () -> new BlockItem(ModBlocks.PURITY_BEACON.get(), new Item.Properties()));

    // --- Tools ---
    public static final RegistryObject<Item> DARK_MATTER_SHARD = ITEMS.register("dark_matter_shard",
            () -> new DarkMatterShardItem(new Item.Properties()));

    public static final RegistryObject<Item> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CLEAR_MATTER_PILL = ITEMS.register("clear_matter_pill",
            () -> new ClearMatterPillItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> CLEAR_MATTER_SHIELD = ITEMS.register("clear_matter_shield",
            () -> new ClearMatterShieldItem(new Item.Properties().stacksTo(4)));

    public static final RegistryObject<Item> WHITE_LIGHT_WAND = ITEMS.register("white_light_wand",
            () -> new WhiteLightWand(new Item.Properties().stacksTo(1).durability(100)));

    public static final RegistryObject<Item> WHITE_MATTER_FINDER = ITEMS.register("white_matter_finder",
            () -> new WhiteMatterFinder(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SAFE_SIPHON = ITEMS.register("safe_siphon",
            () -> new SafeSiphon(new Item.Properties().stacksTo(1)));

    // --- Misc ---
    public static final RegistryObject<Item> INFECTION_GROWTH_ITEM = ITEMS.register("infection_growth",
            () -> new BlockItem(ModBlocks.INFECTION_GROWTH.get(), new Item.Properties()));

    public static final RegistryObject<Item> HOLY_ESSENCE = ITEMS.register("holy_essence",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WHITE_MATTER_BOMB_ITEM = ITEMS.register("white_matter_bomb",
            () -> new BlockItem(ModBlocks.WHITE_MATTER_BOMB_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> CLEANSING_GRENADE = ITEMS.register("cleansing_grenade",
            () -> new CleansingGrenadeItem(new Item.Properties().stacksTo(16)));

    // --- Workbench Block Items ---
    public static final RegistryObject<Item> DARK_MATTER_FORGE_ITEM = ITEMS.register("dark_matter_forge",
            () -> new BlockItem(ModBlocks.DARK_MATTER_FORGE.get(), new Item.Properties()));

    public static final RegistryObject<Item> MATTER_INFUSER_ITEM = ITEMS.register("matter_infuser",
            () -> new BlockItem(ModBlocks.MATTER_INFUSER.get(), new Item.Properties()));

    public static final RegistryObject<Item> RESEARCH_TABLE_ITEM = ITEMS.register("research_table",
            () -> new BlockItem(ModBlocks.RESEARCH_TABLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> CONTAINMENT_CHAMBER_ITEM = ITEMS.register("containment_chamber",
            () -> new BlockItem(ModBlocks.CONTAINMENT_CHAMBER.get(), new Item.Properties()));

    public static final RegistryObject<Item> MATTER_TRANSMUTER_ITEM = ITEMS.register("matter_transmuter",
            () -> new BlockItem(ModBlocks.MATTER_TRANSMUTER.get(), new Item.Properties()));

    // --- New Materials ---
    public static final RegistryObject<Item> STABILIZED_DARK_MATTER = ITEMS.register("stabilized_dark_matter",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VOID_CRYSTAL = ITEMS.register("void_crystal",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SINGULARITY_CORE = ITEMS.register("singularity_core",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MATTER_CORE = ITEMS.register("matter_core",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PURIFIED_ESSENCE = ITEMS.register("purified_essence",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESEARCH_NOTES = ITEMS.register("research_notes",
            () -> new Item(new Item.Properties()));

    // --- Dark Matter Tools ---
    public static final RegistryObject<Item> DARK_MATTER_SWORD = ITEMS.register("dark_matter_sword",
            () -> new DarkMatterSwordItem(new Item.Properties()));

    public static final RegistryObject<Item> DARK_MATTER_PICKAXE = ITEMS.register("dark_matter_pickaxe",
            () -> new DarkMatterPickaxeItem(new Item.Properties()));

    public static final RegistryObject<Item> DARK_MATTER_AXE = ITEMS.register("dark_matter_axe",
            () -> new DarkMatterAxeItem(new Item.Properties()));

    // --- Clear Matter Armor ---
    public static final RegistryObject<Item> CLEAR_MATTER_HELMET = ITEMS.register("clear_matter_helmet",
            () -> new ArmorItem(ClearMatterArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_CHESTPLATE = ITEMS.register("clear_matter_chestplate",
            () -> new ArmorItem(ClearMatterArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_LEGGINGS = ITEMS.register("clear_matter_leggings",
            () -> new ArmorItem(ClearMatterArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_BOOTS = ITEMS.register("clear_matter_boots",
            () -> new ArmorItem(ClearMatterArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));

    // --- Lore Items ---
    public static final RegistryObject<Item> HOST_JOURNAL = ITEMS.register("host_journal",
            () -> new HostJournalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WORKER_BADGE = ITEMS.register("worker_badge",
            () -> new WorkerBadgeItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
