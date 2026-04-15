package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entry.AdminToolItem;
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

    public static final RegistryObject<Item> ADMIN_TOOL = ITEMS.register(
            "admin_tool",
            () -> new AdminToolItem(new Item.Properties())
    );

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

    // --- New Infection Block Items ---
    public static final RegistryObject<Item> CORRUPTED_STONE_ITEM = ITEMS.register("corrupted_stone",
            () -> new BlockItem(ModBlocks.CORRUPTED_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> INFECTION_VEIN_ITEM = ITEMS.register("infection_vein",
            () -> new BlockItem(ModBlocks.INFECTION_VEIN.get(), new Item.Properties()));

    public static final RegistryObject<Item> SPORE_BLOOM_ITEM = ITEMS.register("spore_bloom",
            () -> new BlockItem(ModBlocks.SPORE_BLOOM.get(), new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_LOG_ITEM = ITEMS.register("corrupted_log",
            () -> new BlockItem(ModBlocks.CORRUPTED_LOG.get(), new Item.Properties()));

    public static final RegistryObject<Item> WHITE_MATTER_TNT_ITEM = ITEMS.register("white_matter_tnt",
            () -> new BlockItem(ModBlocks.WHITE_MATTER_TNT.get(), new Item.Properties()));

    // --- Clear Matter Tools ---
    public static final RegistryObject<Item> CLEAR_MATTER_SWORD = ITEMS.register("clear_matter_sword",
            () -> new ClearMatterSwordItem(new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_PICKAXE = ITEMS.register("clear_matter_pickaxe",
            () -> new ClearMatterPickaxeItem(new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_AXE = ITEMS.register("clear_matter_axe",
            () -> new ClearMatterAxeItem(new Item.Properties()));

    // --- Yellow Matter Tools ---
    public static final RegistryObject<Item> YELLOW_MATTER_SWORD = ITEMS.register("yellow_matter_sword",
            () -> new YellowMatterSwordItem(new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_MATTER_PICKAXE = ITEMS.register("yellow_matter_pickaxe",
            () -> new YellowMatterPickaxeItem(new Item.Properties()));

    public static final RegistryObject<Item> YELLOW_MATTER_AXE = ITEMS.register("yellow_matter_axe",
            () -> new YellowMatterAxeItem(new Item.Properties()));

    // --- Yellow Matter Shield ---
    public static final RegistryObject<Item> YELLOW_MATTER_SHIELD = ITEMS.register("yellow_matter_shield",
            () -> new YellowMatterShieldItem(new Item.Properties()));

    // --- Containment Suit ---
    public static final RegistryObject<Item> CONTAINMENT_SUIT_HELMET = ITEMS.register("containment_suit_helmet",
            () -> new ContainmentSuitItem(ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> CONTAINMENT_SUIT_CHESTPLATE = ITEMS.register("containment_suit_chestplate",
            () -> new ContainmentSuitItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> CONTAINMENT_SUIT_LEGGINGS = ITEMS.register("containment_suit_leggings",
            () -> new ContainmentSuitItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> CONTAINMENT_SUIT_BOOTS = ITEMS.register("containment_suit_boots",
            () -> new ContainmentSuitItem(ArmorItem.Type.BOOTS, new Item.Properties()));

    // --- Protection Ruby ---
    public static final RegistryObject<Item> PROTECTION_RUBY = ITEMS.register("protection_ruby",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- Matter Buckets ---
    public static final RegistryObject<Item> CLEAR_MATTER_BUCKET = ITEMS.register("clear_matter_bucket",
            () -> new BucketItem(ModFluids.CLEAR_MATTER.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    public static final RegistryObject<Item> YELLOW_MATTER_BUCKET = ITEMS.register("yellow_matter_bucket",
            () -> new BucketItem(ModFluids.YELLOW_MATTER.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    // --- White Matter Syringe ---
    public static final RegistryObject<Item> WHITE_MATTER_SYRINGE = ITEMS.register("white_matter_syringe",
            () -> new WhiteMatterSyringeItem(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- New Block Items ---
    public static final RegistryObject<Item> GLITCH_BLOCK_ITEM = ITEMS.register("glitch_block",
            () -> new BlockItem(ModBlocks.GLITCH_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> WORMHOLE_BLOCK_ITEM = ITEMS.register("wormhole_block",
            () -> new BlockItem(ModBlocks.WORMHOLE_BLOCK.get(), new Item.Properties()));

    // --- Lore Items ---
    public static final RegistryObject<Item> HOST_JOURNAL = ITEMS.register("host_journal",
            () -> new HostJournalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WORKER_BADGE = ITEMS.register("worker_badge",
            () -> new WorkerBadgeItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FIELD_JOURNAL = ITEMS.register("field_journal",
            () -> new FieldJournalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> EYE_OF_HORUS = ITEMS.register("eye_of_horus",
            () -> new EyeOfHorusItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> EQUILIBRIUM_FRAGMENT = ITEMS.register("equilibrium_fragment",
            () -> new EquilibriumFragmentItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> EXPEDITION_TRACKER = ITEMS.register("expedition_tracker",
            () -> new ExpeditionTrackerItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MATTER_AMPOULE = ITEMS.register("matter_ampoule",
            () -> new MatterAmpouleItem(new Item.Properties().stacksTo(16)));

    // --- Worker Admin Tools ---
    public static final RegistryObject<Item> WORKER_TELEPORTER = ITEMS.register("worker_teleporter",
            () -> new WorkerTeleporterItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> WORKER_LIGHTNING = ITEMS.register("worker_lightning",
            () -> new WorkerLightningItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> WORKER_INVENTORY_VIEWER = ITEMS.register("worker_inventory_viewer",
            () -> new WorkerInventoryViewerItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> WORKER_VOICE_BOX = ITEMS.register("worker_voice_box",
            () -> new WorkerVoiceBoxItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> WORKER_CLONE = ITEMS.register("worker_clone",
            () -> new WorkerCloneItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Gravity Trap ---
    public static final RegistryObject<Item> GRAVITY_TRAP = ITEMS.register("gravity_trap",
            () -> new GravityTrapItem(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Revelation Lens ---
    public static final RegistryObject<Item> REVELATION_LENS = ITEMS.register("revelation_lens",
            () -> new RevelationLensItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- Gravity Anchor ---
    public static final RegistryObject<Item> GRAVITY_ANCHOR = ITEMS.register("gravity_anchor",
            () -> new GravityAnchorItem(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Freeze Staff ---
    public static final RegistryObject<Item> FREEZE_STAFF = ITEMS.register("freeze_staff",
            () -> new FreezeStaffItem(new Item.Properties().stacksTo(1).durability(50).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- New Infection Blocks (F3, F7, F8, F9) ---
    public static final RegistryObject<Item> SCARRED_EARTH_ITEM = ITEMS.register("scarred_earth",
            () -> new BlockItem(ModBlocks.SCARRED_EARTH.get(), new Item.Properties()));

    public static final RegistryObject<Item> SCARRED_STONE_ITEM = ITEMS.register("scarred_stone",
            () -> new BlockItem(ModBlocks.SCARRED_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Item> QUARANTINE_WARD_ITEM = ITEMS.register("quarantine_ward",
            () -> new BlockItem(ModBlocks.QUARANTINE_WARD.get(), new Item.Properties()));

    public static final RegistryObject<Item> UNSTABLE_MATTER_ITEM = ITEMS.register("unstable_matter",
            () -> new BlockItem(ModBlocks.UNSTABLE_MATTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> INFECTION_HEART_ITEM = ITEMS.register("infection_heart",
            () -> new BlockItem(ModBlocks.INFECTION_HEART.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
