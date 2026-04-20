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

    public static final RegistryObject<Item> BLOOD_BUCKET = ITEMS.register("blood_bucket",
            () -> new BucketItem(ModFluids.BLOOD.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

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

//    public static final RegistryObject<Item> FIELD_JOURNAL = ITEMS.register("field_journal",
//            () -> new FieldJournalItem(new Item.Properties().stacksTo(1)));

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

    // --- Interrogation Sticks ---
    public static final RegistryObject<Item> MARKING_STICK = ITEMS.register("marking_stick",
            () -> new MarkingStickItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> EXECUTION_STICK = ITEMS.register("execution_stick",
            () -> new ExecutionStickItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- Summon Staff ---
    public static final RegistryObject<Item> SUMMON_STAFF = ITEMS.register("summon_staff",
            () -> new SummonStaffItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Order Weapons ---
    public static final RegistryObject<Item> HOLY_BLADE = ITEMS.register("holy_blade",
            () -> new HolyBladeItem(net.minecraft.world.item.Tiers.NETHERITE, 8, -2.2F,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)
                            .fireResistant()));

    public static final RegistryObject<Item> HOLY_HAMMER = ITEMS.register("holy_hammer",
            () -> new HolyHammerItem(net.minecraft.world.item.Tiers.NETHERITE, 9, -3.0F,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)
                            .fireResistant()));

    // --- Blood Fountain ---
    public static final RegistryObject<Item> BLOOD_FOUNTAIN_ITEM = ITEMS.register("blood_fountain",
            () -> new BlockItem(ModBlocks.BLOOD_FOUNTAIN.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

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

    // --- Blood Ritual / Proliferation ---
    public static final RegistryObject<Item> CHALK = ITEMS.register("chalk",
            () -> new ChalkItem(new Item.Properties().stacksTo(1).durability(32)));

    public static final RegistryObject<Item> BLOOD_CURE_PILL = ITEMS.register("blood_cure_pill",
            () -> new BloodCurePillItem(new Item.Properties().stacksTo(16)
                    .rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> CHALK_SYMBOL_ITEM = ITEMS.register("chalk_symbol",
            () -> new BlockItem(ModBlocks.CHALK_SYMBOL.get(), new Item.Properties()));

    public static final RegistryObject<Item> BLOOD_ALTAR_ITEM = ITEMS.register("blood_altar",
            () -> new BlockItem(ModBlocks.BLOOD_ALTAR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> LIVING_FLESH_ITEM = ITEMS.register("living_flesh",
            () -> new BlockItem(ModBlocks.LIVING_FLESH.get(), new Item.Properties()));

    public static final RegistryObject<Item> FLESH_MOTHER_ITEM = ITEMS.register("flesh_mother",
            () -> new BlockItem(ModBlocks.FLESH_MOTHER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> ATTACKING_FLESH_ITEM = ITEMS.register("attacking_flesh",
            () -> new BlockItem(ModBlocks.ATTACKING_FLESH.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> BLOOD_INFECTION_BLOCK_ITEM = ITEMS.register("blood_infection_block",
            () -> new BlockItem(ModBlocks.BLOOD_INFECTION_BLOCK.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> BLOOD_INFESTATION_BLOCK_ITEM = ITEMS.register("blood_infestation_block",
            () -> new BlockItem(ModBlocks.BLOOD_INFESTATION_BLOCK.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> BLOOD_VOLCANO_ITEM = ITEMS.register("blood_volcano",
            () -> new BlockItem(ModBlocks.BLOOD_VOLCANO.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> BLOOD_SPIKE_ITEM = ITEMS.register("blood_spike",
            () -> new BlockItem(ModBlocks.BLOOD_SPIKE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // --- Blood terrain variants as BlockItems ---
    public static final RegistryObject<Item> BLOOD_DIRT_ITEM = ITEMS.register("blood_dirt",
            () -> new BlockItem(ModBlocks.BLOOD_DIRT.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_SAND_ITEM = ITEMS.register("blood_sand",
            () -> new BlockItem(ModBlocks.BLOOD_SAND.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_STONE_ITEM = ITEMS.register("blood_stone",
            () -> new BlockItem(ModBlocks.BLOOD_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_COAL_ORE_ITEM = ITEMS.register("blood_coal_ore",
            () -> new BlockItem(ModBlocks.BLOOD_COAL_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_IRON_ORE_ITEM = ITEMS.register("blood_iron_ore",
            () -> new BlockItem(ModBlocks.BLOOD_IRON_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_GOLD_ORE_ITEM = ITEMS.register("blood_gold_ore",
            () -> new BlockItem(ModBlocks.BLOOD_GOLD_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_DIAMOND_ORE_ITEM = ITEMS.register("blood_diamond_ore",
            () -> new BlockItem(ModBlocks.BLOOD_DIAMOND_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_REDSTONE_ORE_ITEM = ITEMS.register("blood_redstone_ore",
            () -> new BlockItem(ModBlocks.BLOOD_REDSTONE_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_LAPIS_ORE_ITEM = ITEMS.register("blood_lapis_ore",
            () -> new BlockItem(ModBlocks.BLOOD_LAPIS_ORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_EMERALD_ORE_ITEM = ITEMS.register("blood_emerald_ore",
            () -> new BlockItem(ModBlocks.BLOOD_EMERALD_ORE.get(), new Item.Properties()));

    // --- Blood Armor (strong tanky) ---
    public static final RegistryObject<Item> BLOOD_HELMET = ITEMS.register("blood_helmet",
            () -> new BloodArmorItem(ArmorItem.Type.HELMET,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> BLOOD_CHESTPLATE = ITEMS.register("blood_chestplate",
            () -> new BloodArmorItem(ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> BLOOD_LEGGINGS = ITEMS.register("blood_leggings",
            () -> new BloodArmorItem(ArmorItem.Type.LEGGINGS,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> BLOOD_BOOTS = ITEMS.register("blood_boots",
            () -> new BloodArmorItem(ArmorItem.Type.BOOTS,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // --- Order Armor ---
    public static final RegistryObject<Item> ORDER_HELMET = ITEMS.register("order_helmet",
            () -> new OrderArmorItem(ArmorItem.Type.HELMET,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> ORDER_CHESTPLATE = ITEMS.register("order_chestplate",
            () -> new OrderArmorItem(ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> ORDER_LEGGINGS = ITEMS.register("order_leggings",
            () -> new OrderArmorItem(ArmorItem.Type.LEGGINGS,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> ORDER_BOOTS = ITEMS.register("order_boots",
            () -> new OrderArmorItem(ArmorItem.Type.BOOTS,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Order / Blood Spell Items ---
    public static final RegistryObject<Item> HOLY_SMITE_STAFF = ITEMS.register("holy_smite_staff",
            () -> new HolySmiteStaffItem(new Item.Properties()));

    public static final RegistryObject<Item> SANCTIFY_ORB = ITEMS.register("sanctify_orb",
            () -> new SanctifyOrbItem(new Item.Properties()));

    public static final RegistryObject<Item> BLOOD_SCYTHE = ITEMS.register("blood_scythe",
            () -> new BloodScytheItem(net.minecraft.world.item.Tiers.NETHERITE, 9, -2.4F,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // --- A Mãe (Fase 2) ---
    public static final RegistryObject<Item> HEART_OF_FLESH_BLOCK_ITEM = ITEMS.register("heart_of_flesh_block",
            () -> new BlockItem(ModBlocks.HEART_OF_FLESH.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> HEART_OF_FLESH_ITEM = ITEMS.register("heart_of_flesh",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> HEART_OF_THE_MOTHER = ITEMS.register("heart_of_the_mother",
            () -> new Item(new Item.Properties().stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> SANGUINE_CORE = ITEMS.register("sanguine_core",
            () -> new Item(new Item.Properties().stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> SANGUINE_ESSENCE = ITEMS.register("sanguine_essence",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> FLESH_MOTHER_BOSS_EGG =
            ITEMS.register("flesh_mother_boss_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.FLESH_MOTHER_BOSS, 0x3a0000, 0xc41818,
                            new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Ordem × Sangue (Fase 5) ---
    public static final RegistryObject<Item> ORDER_SHRINE_ITEM = ITEMS.register("order_shrine",
            () -> new BlockItem(ModBlocks.ORDER_SHRINE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> DESECRATED_HOLY_RELIC = ITEMS.register("desecrated_holy_relic",
            () -> new br.com.murilo.liberthia.item.DesecratedHolyRelicItem(new Item.Properties()));
    public static final RegistryObject<Item> ORDER_PALADIN_EGG = ITEMS.register("order_paladin_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.ORDER_PALADIN, 0xffffff, 0xdbb75b, new Item.Properties()));

    // --- Armas & Magia (Fase 4) ---
    public static final RegistryObject<Item> HEMOMANCER_STAFF = ITEMS.register("hemomancer_staff",
            () -> new br.com.murilo.liberthia.item.HemomancerStaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> BLOOD_BOW = ITEMS.register("blood_bow",
            () -> new br.com.murilo.liberthia.item.BloodBowItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> BLOOD_RITUAL_DAGGER = ITEMS.register("blood_ritual_dagger",
            () -> new br.com.murilo.liberthia.item.BloodRitualDaggerItem(
                    net.minecraft.world.item.Tiers.IRON, 5, -2.0F,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> BLOOD_PACT_AMULET = ITEMS.register("blood_pact_amulet",
            () -> new br.com.murilo.liberthia.item.BloodPactAmuletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // --- Alquimia de Sangue (Fase 3) ---
    public static final RegistryObject<Item> BLOOD_CAULDRON_ITEM = ITEMS.register("blood_cauldron",
            () -> new BlockItem(ModBlocks.BLOOD_CAULDRON.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BLOOD_VIAL = ITEMS.register("blood_vial",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BLOOD_VIAL_FILLED = ITEMS.register("blood_vial_filled",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> CONGEALED_BLOOD = ITEMS.register("congealed_blood",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FLESH_THREAD = ITEMS.register("flesh_thread",
            () -> new Item(new Item.Properties()));

    // --- Culto do Sangue (Fase 1) ---
    public static final RegistryObject<Item> BLOODY_RAG = ITEMS.register("bloody_rag",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RUSTED_DAGGER = ITEMS.register("rusted_dagger",
            () -> new br.com.murilo.liberthia.item.RustedDaggerItem(new Item.Properties()));
    public static final RegistryObject<Item> PRIEST_SIGIL = ITEMS.register("priest_sigil",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> TOME_OF_THE_MOTHER = ITEMS.register("tome_of_the_mother",
            () -> new br.com.murilo.liberthia.item.lore.TomeOfTheMotherItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> TOME_OF_THE_PILGRIM = ITEMS.register("tome_of_the_pilgrim",
            () -> new br.com.murilo.liberthia.item.lore.TomeOfThePilgrimItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BLOOD_CULTIST_EGG =
            ITEMS.register("blood_cultist_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.BLOOD_CULTIST, 0x5a0d0d, 0x2b0000, new Item.Properties()));

    public static final RegistryObject<Item> BLOOD_PRIEST_EGG =
            ITEMS.register("blood_priest_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.BLOOD_PRIEST, 0x3a0000, 0x7a1212, new Item.Properties()));

    public static final RegistryObject<Item> WOUNDED_PILGRIM_EGG =
            ITEMS.register("wounded_pilgrim_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.WOUNDED_PILGRIM, 0x6b5a4a, 0x9a3a3a, new Item.Properties()));

    // --- Spawn eggs for new worms ---
    public static final RegistryObject<Item> FLESH_CRAWLER_EGG =
            ITEMS.register("flesh_crawler_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.FLESH_CRAWLER, 0x5a0000, 0x8b2b2b, new Item.Properties()));

    public static final RegistryObject<Item> GORE_WORM_EGG =
            ITEMS.register("gore_worm_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.GORE_WORM, 0x3a0000, 0xc43a3a, new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
