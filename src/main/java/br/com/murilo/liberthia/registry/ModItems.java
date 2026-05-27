package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entry.AdminToolItem;
import br.com.murilo.liberthia.item.*;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LiberthiaMod.MODID);

    public static final RegistryObject<Item> MAGIC_BOOK =
            ITEMS.register("magic_book",
                    () -> new MagicBookItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> BONE_SEAL =
            ITEMS.register("bone_seal",
                    () -> new SealingSealItem(
                            SealTier.BONE,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(SealTier.BONE.durability())
                                    .rarity(Rarity.UNCOMMON)
                    ));

    public static final RegistryObject<Item> GOLD_SEAL =
            ITEMS.register("gold_seal",
                    () -> new SealingSealItem(
                            SealTier.GOLD,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(SealTier.GOLD.durability())
                                    .rarity(Rarity.RARE)
                    ));

    public static final RegistryObject<Item> DIAMOND_SEAL =
            ITEMS.register("diamond_seal",
                    () -> new SealingSealItem(
                            SealTier.DIAMOND,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(SealTier.DIAMOND.durability())
                                    .rarity(Rarity.EPIC)
                    ));

    public static final RegistryObject<Item> NETHERITE_SEAL =
            ITEMS.register("netherite_seal",
                    () -> new SealingSealItem(
                            SealTier.NETHERITE,
                            new Item.Properties()
                                    .stacksTo(1)
                                    .durability(SealTier.NETHERITE.durability())
                                    .rarity(Rarity.EPIC)
                    ));

    public static final RegistryObject<Item> IMAGE_FRAME_BOOK_BUILDER =
            ITEMS.register("image_frame_book_builder",
                    () -> new ImageFrameBookBuilderItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> IMAGE_FRAME_BOOK =
            ITEMS.register("image_frame_book",
                    () -> new ImageFrameBookItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> SPIRITUAL_CONNECTION =
            ITEMS.register("spiritual_connection", () ->
                    new SpiritualConnectionItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SPIRITUAL_LINK =
            ITEMS.register("spiritual_link", () ->
                    new SpiritualLinkItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DARK_MATTER_BLOCK_ITEM = ITEMS.register("dark_matter_block",
            () -> new br.com.murilo.liberthia.item.PurityAwareBlockItem(
                    ModBlocks.DARK_MATTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_BLOCK_ITEM = ITEMS.register("clear_matter_block",
            () -> new BlockItem(ModBlocks.CLEAR_MATTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> SWORD_BRUM =
            ITEMS.register("sword_brum", () ->
                    new SwordBrumItem(
                            Tiers.NETHERITE,
                            16,
                            -2.4F,
                            new Item.Properties()
                                    .rarity(Rarity.EPIC)
                                    .fireResistant()
                    )
            );

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

    // v0.1.52: ingots de DM e CM (matching o YM já existente). Usados no
    // Matter Infuser refatorado (que antes pedia blocos cheios — 9× mais caro).
    public static final RegistryObject<Item> DARK_MATTER_INGOT = ITEMS.register("dark_matter_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> CLEAR_MATTER_INGOT = ITEMS.register("clear_matter_ingot",
            () -> new Item(new Item.Properties()));

    // v0.1.22: Selo de Passagem — quando no inventário do player, criaturas de
    // sangue e blocos atacantes IGNORAM o player. Não tem cooldown, não consome,
    // só precisa estar em qualquer slot do inventário (hotbar/main/offhand).
    public static final RegistryObject<Item> PASSAGE_SIGIL = ITEMS.register("passage_sigil",
            () -> new br.com.murilo.liberthia.item.PassageSigilItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // v0.1.22: Coroa do Boss — item OP toggleável (right-click). Quando ativo
    // + no inventário, aplica 1800 corações, Regen III, aura, gaze, reflect,
    // panic explosion. Lógica em br.com.murilo.liberthia.event.BossCrownHandler.
    public static final RegistryObject<Item> BOSS_CROWN = ITEMS.register("boss_crown",
            () -> new br.com.murilo.liberthia.item.BossCrownItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

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

    // Pílulas específicas — cada uma cura SÓ um tipo de matter no profile.
    // Antes a Clear Matter Pill curava DM (errado — agora purga só WM).
    public static final RegistryObject<Item> DARK_MATTER_PILL = ITEMS.register("dark_matter_pill",
            () -> new DarkMatterPillItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> YELLOW_MATTER_PILL = ITEMS.register("yellow_matter_pill",
            () -> new YellowMatterPillItem(new Item.Properties().stacksTo(16)));

    // Curas/manutenção do Matter Profile do player.
    // - MATTER_CURE: zera DM/WM/YM + Regen II + Resistance I (emergência)
    // - DAILY_PILL: Resistance/Regen I por 20min + Absorption I 1min (rotina)
    public static final RegistryObject<Item> MATTER_CURE = ITEMS.register("matter_cure",
            () -> new MatterCureItem(new Item.Properties().stacksTo(8)));

    public static final RegistryObject<Item> DAILY_PILL = ITEMS.register("daily_pill",
            () -> new DailyPillItem(new Item.Properties().stacksTo(16)));

    // WHITE_MATTER_FINDER, SAFE_SIPHON, HOLY_ESSENCE removidos (decisão do design)
    // Substitutos onde necessário: PURIFIED_ESSENCE no lugar do HOLY_ESSENCE
    // em receitas de máquinas (DarkMatterForge, MatterTransmuter, etc).

    // --- Misc ---
    public static final RegistryObject<Item> INFECTION_GROWTH_ITEM = ITEMS.register("infection_growth",
            () -> new BlockItem(ModBlocks.INFECTION_GROWTH.get(), new Item.Properties()));

    // REMOVIDO v0.1.13: WHITE_MATTER_BOMB_ITEM, CLEANSING_GRENADE
    // (decisão de design — items redundantes; cura é via Matter Cure/Daily Pill)

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

    public static final RegistryObject<Item> DARK_MATTER_ALCHEMIZER_ITEM = ITEMS.register("dark_matter_alchemizer",
            () -> new BlockItem(ModBlocks.DARK_MATTER_ALCHEMIZER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> PLAYER_LOCK = ITEMS.register("player_lock",
            () -> new br.com.murilo.liberthia.item.PlayerLockItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> DARK_MATTER_GENERATOR_ITEM = ITEMS.register("dark_matter_generator",
            () -> new BlockItem(ModBlocks.DARK_MATTER_GENERATOR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> ENERGY_CABLE_ITEM = ITEMS.register("energy_cable",
            () -> new BlockItem(ModBlocks.ENERGY_CABLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ITEM_PIPE_ITEM = ITEMS.register("item_pipe",
            () -> new BlockItem(ModBlocks.ITEM_PIPE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ITEM_EXTRACTOR_ITEM = ITEMS.register("item_extractor",
            () -> new BlockItem(ModBlocks.ITEM_EXTRACTOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> ITEM_INSERTER_ITEM = ITEMS.register("item_inserter",
            () -> new BlockItem(ModBlocks.ITEM_INSERTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> DIMENSIONAL_CHEST_ITEM = ITEMS.register("dimensional_chest",
            () -> new BlockItem(ModBlocks.DIMENSIONAL_CHEST.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> MATTER_REFINER_ITEM = ITEMS.register("matter_refiner",
            () -> new BlockItem(ModBlocks.MATTER_REFINER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // --- Upgrades de gerador ---
    public static final RegistryObject<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade",
            () -> new Item(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade",
            () -> new Item(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> CAPACITY_UPGRADE = ITEMS.register("capacity_upgrade",
            () -> new Item(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // --- Cadeia de Matéria Escura ---
    public static final RegistryObject<Item> INACTIVE_DARK_MATTER = ITEMS.register("inactive_dark_matter",
            () -> new br.com.murilo.liberthia.item.PurityAwareItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> ACTIVE_DARK_MATTER = ITEMS.register("active_dark_matter",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> DARK_MATTER_CATALYST = ITEMS.register("dark_matter_catalyst",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> DIMENSIONAL_COMPASS = ITEMS.register("dimensional_compass",
            () -> new br.com.murilo.liberthia.item.DimensionalCompassItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> ENERGY_METER = ITEMS.register("energy_meter",
            () -> new br.com.murilo.liberthia.item.EnergyMeterItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // --- Block items para cadeia de refinação ---
    public static final RegistryObject<Item> DARK_MATTER_CHEST_ITEM = ITEMS.register("dark_matter_chest",
            () -> new BlockItem(ModBlocks.DARK_MATTER_CHEST.get(), new Item.Properties()));

    public static final RegistryObject<Item> FRAGMENTED_GENERATOR_ITEM = ITEMS.register("fragmented_generator",
            () -> new BlockItem(ModBlocks.FRAGMENTED_GENERATOR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> LASER_EMITTER_ITEM = ITEMS.register("laser_emitter",
            () -> new BlockItem(ModBlocks.LASER_EMITTER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> CRYSTALLIZER_ITEM = ITEMS.register("crystallizer",
            () -> new BlockItem(ModBlocks.CRYSTALLIZER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> AUTO_FARMER_ITEM = ITEMS.register("auto_farmer",
            () -> new BlockItem(ModBlocks.AUTO_FARMER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> DIMENSIONAL_EXTRACTOR_ITEM = ITEMS.register("dimensional_extractor",
            () -> new BlockItem(ModBlocks.DIMENSIONAL_EXTRACTOR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> CONTAINMENT_GLOVE = ITEMS.register("containment_glove",
            () -> new Item(new Item.Properties().stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.UNCOMMON)
                    .durability(500)));

    // --- Lore items ---
    public static final RegistryObject<Item> RESEARCHER_CODEX = ITEMS.register("researcher_codex",
            () -> new br.com.murilo.liberthia.item.ResearcherCodexItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> HORUS_EYE_SHARD = ITEMS.register("horus_eye_shard",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> EQUILIBRIUM_CRYSTAL = ITEMS.register("equilibrium_crystal",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> SAMPLE_VIAL = ITEMS.register("sample_vial",
            () -> new br.com.murilo.liberthia.item.SampleVialItem(
                    new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MATTER_ANALYZER_ITEM = ITEMS.register("matter_analyzer",
            () -> new BlockItem(ModBlocks.MATTER_ANALYZER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> LIBERTHIA_MANUAL = ITEMS.register("liberthia_manual",
            () -> new br.com.murilo.liberthia.item.LiberthiaManualItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> BATTERY_BASIC_ITEM = ITEMS.register("dm_battery_basic",
            () -> new BlockItem(ModBlocks.BATTERY_BASIC.get(), new Item.Properties()
                    .rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> BATTERY_ADVANCED_ITEM = ITEMS.register("dm_battery_advanced",
            () -> new BlockItem(ModBlocks.BATTERY_ADVANCED.get(), new Item.Properties()
                    .rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> BATTERY_QUANTUM_ITEM = ITEMS.register("dm_battery_quantum",
            () -> new BlockItem(ModBlocks.BATTERY_QUANTUM.get(), new Item.Properties()
                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    // REMOVIDO v0.1.13: LIBERTHIA_WRENCH (decisão de design)

    public static final RegistryObject<Item> PYLON_REMOTE = ITEMS.register("pylon_remote",
            () -> new br.com.murilo.liberthia.item.PylonRemoteItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> DARK_MATTER_CELL = ITEMS.register("dark_matter_cell",
            () -> new br.com.murilo.liberthia.item.DarkMatterCellItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> WIRELESS_CHARGER_ITEM = ITEMS.register("wireless_charger",
            () -> new BlockItem(ModBlocks.WIRELESS_CHARGER.get(), new Item.Properties()
                    .rarity(net.minecraft.world.item.Rarity.RARE)));

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
            () -> new DarkMatterSwordItem(
                    new Item.Properties()
                            .durability(2500)
                            .rarity(Rarity.EPIC)
                            .fireResistant()
            )
    );

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
    // REMOVIDO v0.1.13: GLITCH_BLOCK_ITEM

    public static final RegistryObject<Item> WORMHOLE_BLOCK_ITEM = ITEMS.register("wormhole_block",
            () -> new BlockItem(ModBlocks.WORMHOLE_BLOCK.get(), new Item.Properties()));

    // ============================================================
    // INFECTED VARIANT BLOCK ITEMS — v0.1.13
    // 12 BlockItems pros novos blocos infectados (dirt/sand/stone/grass × DM/WM/YM)
    // ============================================================
    public static final RegistryObject<Item> DM_INFECTED_DIRT_ITEM  = ITEMS.register("dm_infected_dirt",  () -> new BlockItem(ModBlocks.DM_INFECTED_DIRT.get(),  new Item.Properties()));
    public static final RegistryObject<Item> DM_INFECTED_SAND_ITEM  = ITEMS.register("dm_infected_sand",  () -> new BlockItem(ModBlocks.DM_INFECTED_SAND.get(),  new Item.Properties()));
    public static final RegistryObject<Item> DM_INFECTED_STONE_ITEM = ITEMS.register("dm_infected_stone", () -> new BlockItem(ModBlocks.DM_INFECTED_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DM_INFECTED_GRASS_ITEM = ITEMS.register("dm_infected_grass", () -> new BlockItem(ModBlocks.DM_INFECTED_GRASS.get(), new Item.Properties()));
    public static final RegistryObject<Item> WM_BLEACHED_DIRT_ITEM  = ITEMS.register("wm_bleached_dirt",  () -> new BlockItem(ModBlocks.WM_BLEACHED_DIRT.get(),  new Item.Properties()));
    public static final RegistryObject<Item> WM_BLEACHED_SAND_ITEM  = ITEMS.register("wm_bleached_sand",  () -> new BlockItem(ModBlocks.WM_BLEACHED_SAND.get(),  new Item.Properties()));
    public static final RegistryObject<Item> WM_BLEACHED_STONE_ITEM = ITEMS.register("wm_bleached_stone", () -> new BlockItem(ModBlocks.WM_BLEACHED_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WM_BLEACHED_GRASS_ITEM = ITEMS.register("wm_bleached_grass", () -> new BlockItem(ModBlocks.WM_BLEACHED_GRASS.get(), new Item.Properties()));
    public static final RegistryObject<Item> YM_UNSTABLE_DIRT_ITEM  = ITEMS.register("ym_unstable_dirt",  () -> new BlockItem(ModBlocks.YM_UNSTABLE_DIRT.get(),  new Item.Properties()));
    public static final RegistryObject<Item> YM_UNSTABLE_SAND_ITEM  = ITEMS.register("ym_unstable_sand",  () -> new BlockItem(ModBlocks.YM_UNSTABLE_SAND.get(),  new Item.Properties()));
    public static final RegistryObject<Item> YM_UNSTABLE_STONE_ITEM = ITEMS.register("ym_unstable_stone", () -> new BlockItem(ModBlocks.YM_UNSTABLE_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> YM_UNSTABLE_GRASS_ITEM = ITEMS.register("ym_unstable_grass", () -> new BlockItem(ModBlocks.YM_UNSTABLE_GRASS.get(), new Item.Properties()));

    // ============================================================
    // WHITE MATTER PENDANT — v0.1.13 (Curio cosmetic + matter suppressor)
    // ============================================================
    public static final RegistryObject<Item> WHITE_MATTER_PENDANT = ITEMS.register("white_matter_pendant",
            () -> new WhiteMatterPendantItem(new Item.Properties()
                    .stacksTo(1)
                    .durability(br.com.murilo.liberthia.item.WhiteMatterPendantItem.MAX_DURABILITY)
                    .rarity(net.minecraft.world.item.Rarity.RARE)));

    // ============================================================
    // REFINED CONTAINMENT ARTIFACTS — v0.1.30
    // Evolução do Pendant/Glove existentes: usam purified_clear_matter_ingot
    // (do Matter Purifier) em vez de crystal raw. Pausam o GANHO de matter
    // (em vez de só suprimir os efeitos colaterais como o pendant original).
    // ============================================================
    public static final RegistryObject<Item> REFINED_CONTAINMENT_PENDANT =
            ITEMS.register("refined_containment_pendant",
                    () -> new br.com.murilo.liberthia.item.RefinedContainmentPendantItem(
                            new Item.Properties().stacksTo(1)
                                    .durability(br.com.murilo.liberthia.item.RefinedContainmentPendantItem.MAX_DURABILITY)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> REFINED_CONTAINMENT_GLOVE =
            ITEMS.register("refined_containment_glove",
                    () -> new br.com.murilo.liberthia.item.RefinedContainmentGloveItem(
                            new Item.Properties().stacksTo(1)
                                    .durability(br.com.murilo.liberthia.item.RefinedContainmentGloveItem.MAX_DURABILITY)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    // ============================================================
    // v1 — Matter Pendants (Dark/Clear/Yellow) — bloqueiam ganho ambient
    // ============================================================
    public static final RegistryObject<Item> DARK_MATTER_PENDANT =
            ITEMS.register("dark_matter_pendant",
                    () -> new br.com.murilo.liberthia.item.DarkMatterPendantItem(
                            new Item.Properties().stacksTo(1)
                                    .durability(br.com.murilo.liberthia.item.DarkMatterPendantItem.MAX_DURABILITY)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> CLEAR_MATTER_PENDANT =
            ITEMS.register("clear_matter_pendant",
                    () -> new br.com.murilo.liberthia.item.ClearMatterPendantItem(
                            new Item.Properties().stacksTo(1)
                                    .durability(br.com.murilo.liberthia.item.ClearMatterPendantItem.MAX_DURABILITY)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> YELLOW_MATTER_PENDANT =
            ITEMS.register("yellow_matter_pendant",
                    () -> new br.com.murilo.liberthia.item.YellowMatterPendantItem(
                            new Item.Properties().stacksTo(1)
                                    .durability(br.com.murilo.liberthia.item.YellowMatterPendantItem.MAX_DURABILITY)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

    // ============================================================
    // v1 — Astaron Relics (cinto + pés) + chaves bound
    // ============================================================
    public static final RegistryObject<Item> RELIQUIA_PROTECAO_ASTARON =
            ITEMS.register("reliquia_protecao_astaron",
                    () -> new br.com.murilo.liberthia.item.ReliquiaProtecaoAstaronItem(
                            new Item.Properties().stacksTo(1)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)
                                    .fireResistant()));

    public static final RegistryObject<Item> ASTARON_ACCESS_KEY =
            ITEMS.register("astaron_access_key",
                    () -> new br.com.murilo.liberthia.item.AstaronAccessKeyItem(
                            new Item.Properties().stacksTo(1)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)
                                    .fireResistant()));

    // v0.1.43: ADICIONADA .durability(19200). Antes era sem durability — qualquer
    // chamada de damageStack deletava o item porque newDamage=1 >= maxDamage=0.
    // 19200 ticks = 16 min ÷ 1 dmg cada 200t = roughly 16 horas de uso real.
    public static final RegistryObject<Item> PES_QUEIMANTES_ASTARON =
            ITEMS.register("pes_queimantes_astaron",
                    () -> new br.com.murilo.liberthia.item.PesQueimantesAstaronItem(
                            new Item.Properties().stacksTo(1)
                                    .durability(19200)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)
                                    .fireResistant()));

    public static final RegistryObject<Item> FLAME_KEY =
            ITEMS.register("flame_key",
                    () -> new br.com.murilo.liberthia.item.FlameKeyItem(
                            new Item.Properties().stacksTo(1)
                                    .rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> BOTAS_MERCURIAIS =
            ITEMS.register("botas_mercuriais",
                    () -> new br.com.murilo.liberthia.item.BotasMercuriaisItem(
                            new Item.Properties().stacksTo(1)
                                    .durability(br.com.murilo.liberthia.item.BotasMercuriaisItem.MAX_DURABILITY)
                                    .rarity(net.minecraft.world.item.Rarity.EPIC)));

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

    public static final RegistryObject<Item> RED_KIRIKO_BOOK = ITEMS.register("book_red_kiriko",
            () -> new BookRedKirikoItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()
            ));

    public static final RegistryObject<Item> DARK_BLOOD_TEST_ITEM =
            ITEMS.register("dark_blood_test_item", () -> new DarkBloodTestItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)
            ));

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

    public static final RegistryObject<Item> WITHERING_EYE_ITEM = ITEMS.register("withering_eye",
            () -> new BlockItem(ModBlocks.WITHERING_EYE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> VENOM_GEYSER_ITEM = ITEMS.register("venom_geyser",
            () -> new BlockItem(ModBlocks.VENOM_GEYSER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> LIGHTNING_COIL_ITEM = ITEMS.register("lightning_coil",
            () -> new BlockItem(ModBlocks.LIGHTNING_COIL.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- T9: more attacking blocks (BlockItems) ---
    public static final RegistryObject<Item> THORN_BRIAR_ITEM = ITEMS.register("thorn_briar",
            () -> new BlockItem(ModBlocks.THORN_BRIAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> LIGHTNING_NODE_ITEM = ITEMS.register("lightning_node",
            () -> new BlockItem(ModBlocks.LIGHTNING_NODE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SCREAMING_SOUL_ITEM = ITEMS.register("screaming_soul",
            () -> new BlockItem(ModBlocks.SCREAMING_SOUL.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> MAGNETIC_PYLON_ITEM = ITEMS.register("magnetic_pylon",
            () -> new BlockItem(ModBlocks.MAGNETIC_PYLON.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- T9: throwables + utility items ---
    public static final RegistryObject<Item> LIGHTNING_GRENADE = ITEMS.register("lightning_grenade",
            () -> new br.com.murilo.liberthia.item.LightningGrenadeItem(
                    new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BURNING_GEM = ITEMS.register("burning_gem",
            () -> new br.com.murilo.liberthia.item.BurningGemItem(
                    new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FROST_FLASK = ITEMS.register("frost_flask",
            () -> new br.com.murilo.liberthia.item.FrostFlaskItem(
                    new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EYE_OF_DECAY = ITEMS.register("eye_of_decay",
            () -> new br.com.murilo.liberthia.item.EyeOfDecayItem(
                    new Item.Properties().stacksTo(1).durability(64).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> WITHERED_TOTEM = ITEMS.register("withered_totem",
            () -> new br.com.murilo.liberthia.item.WitheredTotemItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

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

    // --- T6: EvilCraft ports ---
    public static final RegistryObject<Item> BLOOD_TELEPORT_PEARL =
            ITEMS.register("blood_teleport_pearl",
                    () -> new br.com.murilo.liberthia.item.BloodTeleportPearlItem(
                            new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> TAINTED_APPLE =
            ITEMS.register("tainted_apple",
                    () -> new br.com.murilo.liberthia.item.TaintedAppleItem(
                            new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> PURGING_PENDANT =
            ITEMS.register("purging_pendant",
                    () -> new br.com.murilo.liberthia.item.PurgingPendantItem(
                            new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- T8: Vanilla-effect throwables ---
    public static final RegistryObject<Item> VEILING_ORB =
            ITEMS.register("veiling_orb",
                    () -> new br.com.murilo.liberthia.item.VeilingOrbItem(
                            new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> MIND_SPLINTER_DART =
            ITEMS.register("mind_splinter_dart",
                    () -> new br.com.murilo.liberthia.item.MindSplinterDartItem(
                            new Item.Properties().stacksTo(16)));

    // --- Seringa de Sangue (T5b) ---
    public static final RegistryObject<Item> BLOOD_SYRINGE =
            ITEMS.register("blood_syringe",
                    () -> new br.com.murilo.liberthia.item.BloodSyringeItem(
                            new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BLOOD_MAGE_EGG =
            ITEMS.register("blood_mage_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.BLOOD_MAGE, 0x2b0000, 0xc20012, new Item.Properties()));

    public static final RegistryObject<Item> BLOOD_HOUND_EGG =
            ITEMS.register("blood_hound_spawn_egg",
                    () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                            ModEntities.BLOOD_HOUND, 0x4a0000, 0x9a2020, new Item.Properties()));

    // --- Op tools: scale rods + command tablet + command pylon ---
    public static final RegistryObject<Item> GROWTH_ROD = ITEMS.register("growth_rod",
            () -> new br.com.murilo.liberthia.item.GrowthRodItem(
                    new Item.Properties().stacksTo(1)
                            .rarity(net.minecraft.world.item.Rarity.RARE), 0.1));

    public static final RegistryObject<Item> SHRINK_ROD = ITEMS.register("shrink_rod",
            () -> new br.com.murilo.liberthia.item.GrowthRodItem(
                    new Item.Properties().stacksTo(1)
                            .rarity(net.minecraft.world.item.Rarity.RARE), -0.1));

    public static final RegistryObject<Item> COMMAND_TABLET = ITEMS.register("command_tablet",
            () -> new br.com.murilo.liberthia.item.CommandTabletItem(
                    new Item.Properties().stacksTo(1)
                            .rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> COMMAND_PYLON_ITEM = ITEMS.register("command_pylon",
            () -> new BlockItem(ModBlocks.COMMAND_PYLON.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> SCRIPT_TABLET = ITEMS.register("script_tablet",
            () -> new br.com.murilo.liberthia.item.script.ScriptTabletItem(
                    new Item.Properties().stacksTo(1)
                            .rarity(net.minecraft.world.item.Rarity.EPIC)));

    // --- Sanguine Ward set (anti Blood Infection) ---
    public static final RegistryObject<Item> SANGUINE_WARD_HELMET = ITEMS.register("sanguine_ward_helmet",
            () -> new br.com.murilo.liberthia.item.SanguineWardArmorItem(
                    ArmorItem.Type.HELMET,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SANGUINE_WARD_CHESTPLATE = ITEMS.register("sanguine_ward_chestplate",
            () -> new br.com.murilo.liberthia.item.SanguineWardArmorItem(
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SANGUINE_WARD_LEGGINGS = ITEMS.register("sanguine_ward_leggings",
            () -> new br.com.murilo.liberthia.item.SanguineWardArmorItem(
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SANGUINE_WARD_BOOTS = ITEMS.register("sanguine_ward_boots",
            () -> new br.com.murilo.liberthia.item.SanguineWardArmorItem(
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SANGUINE_WARD_SWORD = ITEMS.register("sanguine_ward_sword",
            () -> new br.com.murilo.liberthia.item.SanguineWardSwordItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SANGUINE_WARD_PICKAXE = ITEMS.register("sanguine_ward_pickaxe",
            () -> new br.com.murilo.liberthia.item.SanguineWardPickaxeItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> BLOOD_WARD_CHARM = ITEMS.register("blood_ward_charm",
            () -> new br.com.murilo.liberthia.item.BloodWardCharmItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // --- Cleansing items (cures blood infection) ---
    public static final RegistryObject<Item> TAINTED_ESSENCE = ITEMS.register("tainted_essence",
            () -> new br.com.murilo.liberthia.item.TaintedEssenceItem(
                    new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> CLEANSING_SALT = ITEMS.register("cleansing_salt",
            () -> new br.com.murilo.liberthia.item.CleansingSaltItem(
                    new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> PURIFYING_FLASK = ITEMS.register("purifying_flask",
            () -> new br.com.murilo.liberthia.item.PurifyingFlaskItem(
                    new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- Ritual bowl block items ---
    public static final RegistryObject<Item> BLOOD_SACRIFICIAL_BOWL_ITEM = ITEMS.register("blood_sacrificial_bowl",
            () -> new BlockItem(ModBlocks.BLOOD_SACRIFICIAL_BOWL.get(), new Item.Properties()));

    public static final RegistryObject<Item> GOLDEN_BLOOD_BOWL_ITEM = ITEMS.register("golden_blood_bowl",
            () -> new BlockItem(ModBlocks.GOLDEN_BLOOD_BOWL.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> CRYSTALLIZED_BLOOD_SOUL = ITEMS.register("crystallized_blood_soul",
            () -> new br.com.murilo.liberthia.item.CrystallizedBloodSoulItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- Staves / swords mirroring attacking blocks ---
    public static final RegistryObject<Item> THORN_STAFF = ITEMS.register("thorn_staff",
            () -> new br.com.murilo.liberthia.item.ThornStaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    public static final RegistryObject<Item> LIGHTNING_STAFF = ITEMS.register("lightning_staff",
            () -> new br.com.murilo.liberthia.item.LightningStaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> SOUL_SCREAM_SWORD = ITEMS.register("soul_scream_sword",
            () -> new br.com.murilo.liberthia.item.SoulScreamSwordItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    public static final RegistryObject<Item> MAGNETIC_WAND = ITEMS.register("magnetic_wand",
            () -> new br.com.murilo.liberthia.item.MagneticWandItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // --- Block items for attacking blood blocks ---
    public static final RegistryObject<Item> HEMORRHAGE_SPIKE_ITEM = ITEMS.register("hemorrhage_spike",
            () -> new BlockItem(ModBlocks.HEMORRHAGE_SPIKE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_SNARE_ITEM = ITEMS.register("sanguine_snare",
            () -> new BlockItem(ModBlocks.SANGUINE_SNARE.get(), new Item.Properties()));
    public static final RegistryObject<Item> VEIL_OF_MADNESS_ITEM = ITEMS.register("veil_of_madness",
            () -> new BlockItem(ModBlocks.VEIL_OF_MADNESS.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> PHANTOM_PORTAL_ITEM = ITEMS.register("phantom_portal",
            () -> new BlockItem(ModBlocks.PHANTOM_PORTAL.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // --- Occultism-port items ---
    public static final RegistryObject<Item> BLOOD_CHALK = ITEMS.register("blood_chalk",
            () -> new br.com.murilo.liberthia.item.BloodChalkItem(
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BLOOD_TORCH_ITEM = ITEMS.register("blood_torch",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.BLOOD_TORCH.get(),
                    new Item.Properties()));

    // --- Sanguine Wood block items ---
    public static final RegistryObject<Item> SANGUINE_LOG_ITEM = ITEMS.register("sanguine_log",
            () -> new BlockItem(ModBlocks.SANGUINE_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_WOOD_ITEM = ITEMS.register("sanguine_wood",
            () -> new BlockItem(ModBlocks.SANGUINE_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRIPPED_SANGUINE_LOG_ITEM = ITEMS.register("stripped_sanguine_log",
            () -> new BlockItem(ModBlocks.STRIPPED_SANGUINE_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRIPPED_SANGUINE_WOOD_ITEM = ITEMS.register("stripped_sanguine_wood",
            () -> new BlockItem(ModBlocks.STRIPPED_SANGUINE_WOOD.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_PLANKS_ITEM = ITEMS.register("sanguine_planks",
            () -> new BlockItem(ModBlocks.SANGUINE_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_LEAVES_ITEM = ITEMS.register("sanguine_leaves",
            () -> new BlockItem(ModBlocks.SANGUINE_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_SAPLING_ITEM = ITEMS.register("sanguine_sapling",
            () -> new BlockItem(ModBlocks.SANGUINE_SAPLING.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_STAIRS_ITEM = ITEMS.register("sanguine_stairs",
            () -> new BlockItem(ModBlocks.SANGUINE_STAIRS.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_SLAB_ITEM = ITEMS.register("sanguine_slab",
            () -> new BlockItem(ModBlocks.SANGUINE_SLAB.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_FENCE_ITEM = ITEMS.register("sanguine_fence",
            () -> new BlockItem(ModBlocks.SANGUINE_FENCE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_FENCE_GATE_ITEM = ITEMS.register("sanguine_fence_gate",
            () -> new BlockItem(ModBlocks.SANGUINE_FENCE_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_BUTTON_ITEM = ITEMS.register("sanguine_button",
            () -> new BlockItem(ModBlocks.SANGUINE_BUTTON.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_PRESSURE_PLATE_ITEM = ITEMS.register("sanguine_pressure_plate",
            () -> new BlockItem(ModBlocks.SANGUINE_PRESSURE_PLATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_DOOR_ITEM = ITEMS.register("sanguine_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.SANGUINE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SANGUINE_TRAPDOOR_ITEM = ITEMS.register("sanguine_trapdoor",
            () -> new BlockItem(ModBlocks.SANGUINE_TRAPDOOR.get(), new Item.Properties()));

    // --- Blood Tree wood block items ---
    public static final RegistryObject<Item> BLOOD_LOG_ITEM = ITEMS.register("blood_log",
            () -> new BlockItem(ModBlocks.BLOOD_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> STRIPPED_BLOOD_LOG_ITEM = ITEMS.register("stripped_blood_log",
            () -> new BlockItem(ModBlocks.STRIPPED_BLOOD_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_PLANKS_ITEM = ITEMS.register("blood_planks",
            () -> new BlockItem(ModBlocks.BLOOD_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_LEAVES_ITEM = ITEMS.register("blood_leaves",
            () -> new BlockItem(ModBlocks.BLOOD_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_SAPLING_ITEM = ITEMS.register("blood_sapling",
            () -> new BlockItem(ModBlocks.BLOOD_SAPLING.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_STAIRS_ITEM = ITEMS.register("blood_stairs",
            () -> new BlockItem(ModBlocks.BLOOD_STAIRS.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_SLAB_ITEM = ITEMS.register("blood_slab",
            () -> new BlockItem(ModBlocks.BLOOD_SLAB.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_FENCE_ITEM = ITEMS.register("blood_fence",
            () -> new BlockItem(ModBlocks.BLOOD_FENCE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_FENCE_GATE_ITEM = ITEMS.register("blood_fence_gate",
            () -> new BlockItem(ModBlocks.BLOOD_FENCE_GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_DOOR_ITEM = ITEMS.register("blood_door",
            () -> new net.minecraft.world.item.DoubleHighBlockItem(ModBlocks.BLOOD_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> BLOOD_TRAPDOOR_ITEM = ITEMS.register("blood_trapdoor",
            () -> new BlockItem(ModBlocks.BLOOD_TRAPDOOR.get(), new Item.Properties()));

    // --- Boss artifacts (passive aura items dropped from FleshMother) ---
    public static final RegistryObject<Item> CURSED_IDOL = ITEMS.register("cursed_idol",
            () -> new br.com.murilo.liberthia.item.BossArtifactItem(
                    new Item.Properties(),
                    "Ídolo Maldito — pulsa Lentidão",
                    () -> net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    1, 80, 8.0));

    public static final RegistryObject<Item> VEILED_LANTERN = ITEMS.register("veiled_lantern",
            () -> new br.com.murilo.liberthia.item.BossArtifactItem(
                    new Item.Properties(),
                    "Lanterna Velada — pulsa Cegueira",
                    () -> net.minecraft.world.effect.MobEffects.BLINDNESS,
                    0, 80, 12.0));

    public static final RegistryObject<Item> PULSING_HEART = ITEMS.register("pulsing_heart",
            () -> new br.com.murilo.liberthia.item.BossArtifactItem(
                    new Item.Properties(),
                    "Coração Pulsante — pulsa Wither",
                    () -> net.minecraft.world.effect.MobEffects.WITHER,
                    0, 100, 6.0));

    // --- Possessed mob spawn eggs ---
    public static final RegistryObject<Item> POSSESSED_ZOMBIE_EGG = ITEMS.register("possessed_zombie_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.POSSESSED_ZOMBIE, 0x2a0606, 0x4a1a0a, new Item.Properties()));

    public static final RegistryObject<Item> POSSESSED_SKELETON_EGG = ITEMS.register("possessed_skeleton_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.POSSESSED_SKELETON, 0x4a0a0a, 0x8a3a3a, new Item.Properties()));

    public static final RegistryObject<Item> BLOOD_WARDEN_EGG = ITEMS.register("blood_warden_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.BLOOD_WARDEN, 0x1a0205, 0x8a0e1a,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<Item> WEAVING_SHADE_EGG = ITEMS.register("weaving_shade_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.WEAVING_SHADE, 0x202030, 0x6a4a8a, new Item.Properties()));

    public static final RegistryObject<Item> DISARMER_EGG = ITEMS.register("disarmer_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.DISARMER, 0x3a2a0a, 0x8a5a2a, new Item.Properties()));

    /**
     * Pipe Filter Not — quando o jogador segura esse item e right-clica numa
     * face de Item Pipe, flipa o modo de filtro daquela face de WHITELIST pra
     * BLACKLIST (e vice-versa). Significa: pipe passa TUDO menos os itens que
     * estão listados no filtro.
     *
     * <p>Era possível trocar isso via shift+empty-hand antes, mas só funcionava
     * com filtro JÁ preenchido (e era escondido). Esse item dá um affordance
     * visual e funciona independente de ter filtro.
     */
    public static final RegistryObject<Item> PIPE_FILTER_NOT =
            ITEMS.register("pipe_filter_not",
                    () -> new PipeFilterNotItem(new Item.Properties().stacksTo(16)));

    /**
     * Pipe Filter — irmão do {@code pipe_filter_not}. Força WHITELIST na face
     * (default já é WHITELIST, então esse item serve pra UNDO depois de aplicar
     * o _not). Roadmap v0.1.17, implementado em v0.1.15 antecipado.
     */
    public static final RegistryObject<Item> PIPE_FILTER =
            ITEMS.register("pipe_filter",
                    () -> new PipeFilterItem(new Item.Properties().stacksTo(16)));

    // ────────────────────────────────────────────────────────────────────
    // Purified matter ingots — saída do Matter Purifier. Usados pra craftar
    // as 3 armaduras de matter (substituíram os ingots/itens crus nas recipes
    // de armor — passa pelo purifier antes pra estabilizar).
    // ────────────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> PURIFIED_DARK_MATTER_INGOT =
            ITEMS.register("purified_dark_matter_ingot",
                    () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PURIFIED_CLEAR_MATTER_INGOT =
            ITEMS.register("purified_clear_matter_ingot",
                    () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PURIFIED_YELLOW_MATTER_INGOT =
            ITEMS.register("purified_yellow_matter_ingot",
                    () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // ────────────────────────────────────────────────────────────────────
    // Dark Matter Armor — set completo dá Strength + Resistance + 4 corações
    // amarelos (Absorption III). Receita usa purified_dark_matter_ingot do
    // Matter Purifier. Material: DarkMatterArmorMaterial.
    // ────────────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> DARK_MATTER_HELMET = ITEMS.register("dark_matter_helmet",
            () -> new ArmorItem(DarkMatterArmorMaterial.INSTANCE, ArmorItem.Type.HELMET,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DARK_MATTER_CHESTPLATE = ITEMS.register("dark_matter_chestplate",
            () -> new ArmorItem(DarkMatterArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DARK_MATTER_LEGGINGS = ITEMS.register("dark_matter_leggings",
            () -> new ArmorItem(DarkMatterArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DARK_MATTER_BOOTS = ITEMS.register("dark_matter_boots",
            () -> new ArmorItem(DarkMatterArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS,
                    new Item.Properties().rarity(Rarity.EPIC)));

    // ────────────────────────────────────────────────────────────────────
    // v1: Vision Swap Lens — espia pelos olhos de quem tem WM ≥ 50.
    // 8s de POV alheio, 60s cooldown. Receita: spyglass + 4× purified clear +
    // ender_eye. Ver VisionSwapLensItem + VisionSwapManager.
    // ────────────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> VISION_SWAP_LENS =
            ITEMS.register("vision_swap_lens",
                    () -> new VisionSwapLensItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)));

    // ────────────────────────────────────────────────────────────────────
    // v1: Possession Amulet — controlar outro player ou mob por right-click.
    // EPIC, com foil, SEM RECIPE (só admin/loot raro). Shift do possessor
    // cancela. Ver PossessionAmuletItem + PossessionManager.
    // ────────────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> POSSESSION_AMULET =
            ITEMS.register("possession_amulet",
                    () -> new PossessionAmuletItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()));

    // v0.1.22 r22: Mind Ward — antídoto ao Possession Amulet. Bloqueia
    // qualquer tentativa de posse quando no inv. Stack 1, raridade RARE
    // (mais comum que o amulet — defesa deve ser acessível).
    public static final RegistryObject<Item> MIND_WARD =
            ITEMS.register("mind_ward",
                    () -> new MindWardItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.RARE)
                            .fireResistant()));

    // ────────────────────────────────────────────────────────────────────
    // v0.1.22 r23: COSMIC HORROR — 5 itens de loucura/mente
    // ────────────────────────────────────────────────────────────────────

    /** Madness Aura — Aura passiva de alucinações em 20 blocos. */
    public static final RegistryObject<Item> MADNESS_AURA =
            ITEMS.register("madness_aura",
                    () -> new MadnessAuraItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()));

    /** Maddening Gaze — olhar enlouquecedor (raycast). */
    public static final RegistryObject<Item> MADDENING_GAZE =
            ITEMS.register("maddening_gaze",
                    () -> new MaddeningGazeItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()));

    /** Mass Possession Crown — controle de múltiplos via chat. */
    public static final RegistryObject<Item> MASS_POSSESSION_CROWN =
            ITEMS.register("mass_possession_crown",
                    () -> new MassPossessionCrownItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()));

    /** Mirror of Insanity — distorção perceptual. */
    public static final RegistryObject<Item> MIRROR_OF_INSANITY =
            ITEMS.register("mirror_of_insanity",
                    () -> new MirrorOfInsanityItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()));

    /** Soul Cloner — frasco que clona players com skin. */
    public static final RegistryObject<Item> SOUL_CLONER =
            ITEMS.register("soul_cloner",
                    () -> new SoulClonerItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .durability(8)));

    // ────────────────────────────────────────────────────────────────────
    // v0.1.22 r24: SPIRIT WORLD — items + block items
    // ────────────────────────────────────────────────────────────────────

    /** Soul Sever — corta consciência do corpo (entra/sai do Spirit World). */
    public static final RegistryObject<Item> SOUL_SEVER =
            ITEMS.register("soul_sever",
                    () -> new br.com.murilo.liberthia.item.SoulSeverItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC)
                                    .fireResistant()));

    /** Spirit Altar — block item do altar de ritual. */
    public static final RegistryObject<Item> SPIRIT_ALTAR_ITEM =
            ITEMS.register("spirit_altar",
                    () -> new net.minecraft.world.item.BlockItem(
                            br.com.murilo.liberthia.registry.ModBlocks.SPIRIT_ALTAR.get(),
                            new Item.Properties().rarity(Rarity.RARE)));

    /** Spirit Stone — block item da pedra espiritual. */
    public static final RegistryObject<Item> SPIRIT_STONE_ITEM =
            ITEMS.register("spirit_stone",
                    () -> new net.minecraft.world.item.BlockItem(
                            br.com.murilo.liberthia.registry.ModBlocks.SPIRIT_STONE.get(),
                            new Item.Properties()));

    /** Dimensional Antenna — block item da antena. */
    public static final RegistryObject<Item> DIMENSIONAL_ANTENNA_ITEM =
            ITEMS.register("dimensional_antenna",
                    () -> new net.minecraft.world.item.BlockItem(
                            br.com.murilo.liberthia.registry.ModBlocks.DIMENSIONAL_ANTENNA.get(),
                            new Item.Properties().rarity(Rarity.RARE)));

    /** v0.1.22 r28: Quantum Terminal — block item do computador receptor. */
    public static final RegistryObject<Item> QUANTUM_TERMINAL_ITEM =
            ITEMS.register("quantum_terminal",
                    () -> new net.minecraft.world.item.BlockItem(
                            br.com.murilo.liberthia.registry.ModBlocks.QUANTUM_TERMINAL.get(),
                            new Item.Properties().rarity(Rarity.RARE)));

    // ────────────────────────────────────────────────────────────────────
    // v0.1.35: Pulso — artifact que dispara cone sônico estilo Warden.
    // 20 dano + Weakness II + Fatigue III + Slowness III por 8s. Cooldown 20s.
    // ────────────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> PULSO =
            ITEMS.register("pulso",
                    () -> new br.com.murilo.liberthia.item.PulsoItem(
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ════════════════════════════════════════════════════════════════════
    // v0.1.22 r29: SPIRIT WORLD — block items
    // ════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Item> SPIRIT_GRASS_BLOCK_ITEM = ITEMS.register("spirit_grass_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPIRIT_GRASS_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_DIRT_ITEM = ITEMS.register("spirit_dirt",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPIRIT_DIRT.get(), new Item.Properties()));
    public static final RegistryObject<Item> ETHEREAL_STONE_ITEM = ITEMS.register("ethereal_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ETHEREAL_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> ETHEREAL_STONE_BRICKS_ITEM = ITEMS.register("ethereal_stone_bricks",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ETHEREAL_STONE_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> SOUL_BRICK_ITEM = ITEMS.register("soul_brick",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SOUL_BRICK.get(), new Item.Properties()));
    public static final RegistryObject<Item> HALO_MARBLE_ITEM = ITEMS.register("halo_marble",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.HALO_MARBLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> HALO_MARBLE_BRICKS_ITEM = ITEMS.register("halo_marble_bricks",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.HALO_MARBLE_BRICKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> DREAM_GLASS_ITEM = ITEMS.register("dream_glass",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.DREAM_GLASS.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHISPERWOOD_LOG_ITEM = ITEMS.register("whisperwood_log",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.WHISPERWOOD_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHISPERWOOD_PLANKS_ITEM = ITEMS.register("whisperwood_planks",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.WHISPERWOOD_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHISPERWOOD_LEAVES_ITEM = ITEMS.register("whisperwood_leaves",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.WHISPERWOOD_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> ASTRAL_LANTERN_ITEM = ITEMS.register("astral_lantern",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ASTRAL_LANTERN.get(), new Item.Properties()));
    public static final RegistryObject<Item> CRYSTAL_SPIRIT_ORE_ITEM = ITEMS.register("crystal_spirit_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CRYSTAL_SPIRIT_ORE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SANCTUM_WARD_ITEM = ITEMS.register("sanctum_ward",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SANCTUM_WARD.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> HOLY_CENSER_ITEM = ITEMS.register("holy_censer",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.HOLY_CENSER.get(), new Item.Properties().rarity(Rarity.RARE)));

    // ════════════════════════════════════════════════════════════════════
    // v0.1.22 r29: COSMIC HORROR ITEMS (8)
    // ════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Item> WHISPERING_VEIL = ITEMS.register("whispering_veil",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.WhisperingVeil(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> EYES_OF_ABYSS = ITEMS.register("eyes_of_abyss",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.EyesOfAbyss(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CURSED_CRADLE = ITEMS.register("cursed_cradle",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.CursedCradle(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> PENDULUM_OF_DREAD = ITEMS.register("pendulum_of_dread",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.PendulumOfDread(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> LANTERN_OF_FALSE_MEMORY = ITEMS.register("lantern_of_false_memory",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.LanternOfFalseMemory(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> TONGUE_OF_OLD_ONES = ITEMS.register("tongue_of_old_ones",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.TongueOfOldOnes(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> HOURGLASS_OF_REGRESSION = ITEMS.register("hourglass_of_regression",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.HourglassOfRegression(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> VOID_SEER_ORB = ITEMS.register("void_seer_orb",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.VoidSeerOrb(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ════════════════════════════════════════════════════════════════════
    // v0.1.22 r29: ANGEL / SPIRIT MAGIC ITEMS (12)
    // ════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Item> HALO_OF_LIGHT = ITEMS.register("halo_of_light",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.HaloOfLight(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> WINGS_OF_ASCENSION = ITEMS.register("wings_of_ascension",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.WingsOfAscension(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> ANGEL_TEAR_AMULET = ITEMS.register("angel_tear_amulet",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.AngelTearAmulet(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SPIRIT_ANCHOR = ITEMS.register("spirit_anchor",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.SpiritAnchor(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> CHOIR_BELL = ITEMS.register("choir_bell",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.ChoirBell(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DIVINE_SMITE_ROD = ITEMS.register("divine_smite_rod",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.DivineSmiteRod(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SOUL_MIRROR = ITEMS.register("soul_mirror",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.SoulMirror(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SPIRIT_COMPASS = ITEMS.register("spirit_compass",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.SpiritCompass(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> HOLY_WATER_BUCKET = ITEMS.register("holy_water_bucket",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.HolyWaterBucket(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ANGEL_WING_FEATHER = ITEMS.register("angel_wing_feather",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.AngelWingFeather(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SERAPH_BLADE = ITEMS.register("seraph_blade",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.SeraphBlade(
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> PRAYER_BOOK = ITEMS.register("prayer_book",
            () -> new br.com.murilo.liberthia.item.SpiritMagicItems.PrayerBook(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ════════════════════════════════════════════════════════════════════════
    // v0.1.22 r32: OCCULT SYSTEM ITEMS
    // ════════════════════════════════════════════════════════════════════════

    // 5 Chalks
    public static final RegistryObject<Item> CHALK_WHITE = ITEMS.register("chalk_white",
            () -> new br.com.murilo.liberthia.occult.OccultItems.ChalkItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.COMMON),
                    br.com.murilo.liberthia.occult.OccultItems.ChalkColor.WHITE));
    public static final RegistryObject<Item> CHALK_GOLDEN = ITEMS.register("chalk_golden",
            () -> new br.com.murilo.liberthia.occult.OccultItems.ChalkItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                    br.com.murilo.liberthia.occult.OccultItems.ChalkColor.GOLDEN));
    public static final RegistryObject<Item> CHALK_PURPLE = ITEMS.register("chalk_purple",
            () -> new br.com.murilo.liberthia.occult.OccultItems.ChalkItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                    br.com.murilo.liberthia.occult.OccultItems.ChalkColor.PURPLE));
    public static final RegistryObject<Item> CHALK_RED = ITEMS.register("chalk_red",
            () -> new br.com.murilo.liberthia.occult.OccultItems.ChalkItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                    br.com.murilo.liberthia.occult.OccultItems.ChalkColor.RED));
    public static final RegistryObject<Item> CHALK_BLACK = ITEMS.register("chalk_black",
            () -> new br.com.murilo.liberthia.occult.OccultItems.ChalkItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    br.com.murilo.liberthia.occult.OccultItems.ChalkColor.BLACK));

    // 5 Candle block items
    public static final RegistryObject<Item> CANDLE_WHITE_OCCULT_ITEM = ITEMS.register("candle_occult_white",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CANDLE_WHITE_OCCULT.get(), new Item.Properties()));
    public static final RegistryObject<Item> CANDLE_GOLDEN_OCCULT_ITEM = ITEMS.register("candle_occult_golden",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CANDLE_GOLDEN_OCCULT.get(), new Item.Properties()));
    public static final RegistryObject<Item> CANDLE_PURPLE_OCCULT_ITEM = ITEMS.register("candle_occult_purple",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CANDLE_PURPLE_OCCULT.get(), new Item.Properties()));
    public static final RegistryObject<Item> CANDLE_RED_OCCULT_ITEM = ITEMS.register("candle_occult_red",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CANDLE_RED_OCCULT.get(), new Item.Properties()));
    public static final RegistryObject<Item> CANDLE_BLACK_OCCULT_ITEM = ITEMS.register("candle_occult_black",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CANDLE_BLACK_OCCULT.get(), new Item.Properties()));

    // Lighter
    public static final RegistryObject<Item> LIGHTER = ITEMS.register("lighter",
            () -> new br.com.murilo.liberthia.occult.OccultItems.LighterItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.COMMON)));

    // Ritual Dagger + Chalice
    public static final RegistryObject<Item> RITUAL_DAGGER = ITEMS.register("ritual_dagger",
            () -> new br.com.murilo.liberthia.occult.OccultItems.RitualDaggerItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RITUAL_CHALICE = ITEMS.register("ritual_chalice",
            () -> new br.com.murilo.liberthia.occult.OccultItems.RitualChaliceItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // 10 Sigils (nomes secretos)
    public static final RegistryObject<Item> SIGIL_FOLIOT = ITEMS.register("sigil_foliot",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.UNCOMMON),
                    "Foliot", "Hermetism", "Espírito menor das pedras e raízes"));
    public static final RegistryObject<Item> SIGIL_DJINNI = ITEMS.register("sigil_djinni",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.RARE),
                    "Djinni", "Arábica", "Espírito do ar e do desejo"));
    public static final RegistryObject<Item> SIGIL_AFRIT = ITEMS.register("sigil_afrit",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.RARE),
                    "Afrit", "Arábica", "Espírito do fogo e da guerra"));
    public static final RegistryObject<Item> SIGIL_BAEL = ITEMS.register("sigil_bael",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.EPIC),
                    "Bael", "Goetia", "Primeiro Rei do Inferno — invisibilidade"));
    public static final RegistryObject<Item> SIGIL_LUCIFER = ITEMS.register("sigil_lucifer",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.EPIC),
                    "Lúcifer", "Goetia", "Estrela da Manhã — luz proibida"));
    public static final RegistryObject<Item> SIGIL_SANDALPHON = ITEMS.register("sigil_sandalphon",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.EPIC),
                    "Sandalphon", "Kabbalah", "Arcanjo das orações — Malkuth"));
    public static final RegistryObject<Item> SIGIL_METATRON = ITEMS.register("sigil_metatron",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.EPIC),
                    "Metatron", "Kabbalah", "Voz de Deus — Kether"));
    public static final RegistryObject<Item> SIGIL_NECRO = ITEMS.register("sigil_necro",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.RARE),
                    "Necromante", "Necromancia", "Convoca os mortos das tumbas"));
    public static final RegistryObject<Item> SIGIL_BANISHING = ITEMS.register("sigil_banishing",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.RARE),
                    "Banimento", "Golden Dawn", "LBRP — limpa influências hostis"));
    public static final RegistryObject<Item> SIGIL_DIMENSIONAL = ITEMS.register("sigil_dimensional",
            () -> new br.com.murilo.liberthia.occult.OccultItems.SigilItem(
                    new Item.Properties().rarity(Rarity.EPIC),
                    "Salto Dimensional", "Hauntologia", "Atravessa o véu pra o Spirit World"));

    // Bound Crystals (resultado de rituais)
    public static final RegistryObject<Item> BOUND_FOLIOT_CRYSTAL = ITEMS.register("bound_foliot_crystal",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
                @Override
                public boolean isFoil(ItemStack s) { return true; }
                @Override
                public void appendHoverText(ItemStack s, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level l,
                                            java.util.List<net.minecraft.network.chat.Component> t,
                                            net.minecraft.world.item.TooltipFlag f) {
                    int ch = s.getOrCreateTag().getInt("Charges");
                    if (ch == 0 && !s.getTag().contains("Charges")) {
                        s.getTag().putInt("Charges", 1000);
                        ch = 1000;
                    }
                    t.add(net.minecraft.network.chat.Component.literal(
                            "§5§oCristal Vinculado: Foliot").withStyle(net.minecraft.ChatFormatting.ITALIC));
                    t.add(net.minecraft.network.chat.Component.literal(
                            "§7Cargas: §e" + ch + " §7/ 1000"));
                    t.add(net.minecraft.network.chat.Component.literal(
                            "§7Coloca em chest adjacente a Spirit Miner pra automatizar."));
                }
            });
    public static final RegistryObject<Item> BOUND_DJINNI_CRYSTAL = ITEMS.register("bound_djinni_crystal",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)) {
                @Override public boolean isFoil(ItemStack s) { return true; }
            });
    public static final RegistryObject<Item> BOUND_AFRIT_CRYSTAL = ITEMS.register("bound_afrit_crystal",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)) {
                @Override public boolean isFoil(ItemStack s) { return true; }
            });

    // Block items
    public static final RegistryObject<Item> RITUAL_CIRCLE_ITEM = ITEMS.register("ritual_circle",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RITUAL_CIRCLE.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SPIRIT_MINER_ITEM = ITEMS.register("spirit_miner",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPIRIT_MINER.get(),
                    new Item.Properties().rarity(Rarity.RARE)));

    // ════════════════════════════════════════════════════════════════════════
    // v0.1.22 r33: LOOM DIMENSION ITEMS
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Item> DARK_MATTER_LASER = ITEMS.register("dark_matter_laser",
            () -> new br.com.murilo.liberthia.loom.DarkMatterLaserItem(
                    new Item.Properties().rarity(Rarity.EPIC)));

    // Spawn eggs pros 3 monstros
    public static final RegistryObject<Item> LOOM_WATCHER_EGG = ITEMS.register("loom_watcher_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.LOOM_WATCHER, 0x222222, 0x440099,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> LOOM_PERIPHERAL_EGG = ITEMS.register("loom_peripheral_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.LOOM_PERIPHERAL, 0x000000, 0xFFFFFF,
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> LOOM_SCREAMER_EGG = ITEMS.register("loom_screamer_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.LOOM_SCREAMER, 0x551122, 0xAA3344,
                    new Item.Properties().rarity(Rarity.EPIC)));

    // r34: Sky Tear demo item
    public static final RegistryObject<Item> SKY_TEAR_HORN = ITEMS.register("sky_tear_horn",
            () -> new br.com.murilo.liberthia.sky.SkyTearItem(
                    new Item.Properties().rarity(Rarity.EPIC)));

    // r34: LOOM ORES + shards
    public static final RegistryObject<Item> LOOM_STONE_ITEM = ITEMS.register("loom_stone",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.LOOM_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> VOIDITE_ORE_ITEM = ITEMS.register("loom_voidite_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.VOIDITE_ORE.get(),
                    new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RIFTITE_ORE_ITEM = ITEMS.register("loom_riftite_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RIFTITE_ORE.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> UMBRAL_ORE_ITEM = ITEMS.register("loom_umbral_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.UMBRAL_ORE.get(),
                    new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> VOIDITE_SHARD = ITEMS.register("voidite_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RIFTITE_SHARD = ITEMS.register("riftite_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> UMBRAL_SHARD = ITEMS.register("umbral_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    // r35: COSMIC HORROR SYSTEM trigger
    public static final RegistryObject<Item> FORBIDDEN_TOME = ITEMS.register("forbidden_tome",
            () -> new br.com.murilo.liberthia.cosmic.CosmicTriggerItem(
                    new Item.Properties().rarity(Rarity.EPIC)));

    // r36: GRIMOIRE — sistema de feitiços aprendidos via rituais
    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("grimoire",
            () -> new br.com.murilo.liberthia.magic.GrimoireItem(
                    new Item.Properties().rarity(Rarity.EPIC)));

    // r40: CREATIVE GRIMOIRE — todos os feitiços, mana infinita, sem cooldown
    public static final RegistryObject<Item> CREATIVE_GRIMOIRE = ITEMS.register("creative_grimoire",
            () -> new br.com.murilo.liberthia.magic.CreativeGrimoireItem(
                    new Item.Properties().rarity(Rarity.EPIC)));

    // r42: SPELL CRAFTING TABLE (item portátil) — abre GUI de craft de custom spells
    public static final RegistryObject<Item> SPELL_CRAFTING_TABLE = ITEMS.register("spell_crafting_table",
            () -> new br.com.murilo.liberthia.magic.custom.SpellCraftingItem(
                    new Item.Properties().rarity(Rarity.RARE)));

    // r45: COSMIC HORROR EXPANSION — 8 itens novos
    public static final RegistryObject<Item> CURSED_EFFIGY = ITEMS.register("cursed_effigy",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.CursedEffigyItem(
                    new Item.Properties()));
    public static final RegistryObject<Item> WATCHER_MARK = ITEMS.register("watcher_mark",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.WatcherMarkItem(
                    new Item.Properties()));
    public static final RegistryObject<Item> PHANTOM_CALLER = ITEMS.register("phantom_caller",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.PhantomCallerItem(
                    new Item.Properties()));
    public static final RegistryObject<Item> VULTO_LENS = ITEMS.register("vulto_lens",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.VultoLensItem(
                    new Item.Properties()));
    public static final RegistryObject<Item> INSANITY_CROWN = ITEMS.register("insanity_crown",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.InsanityCrownItem(
                    new Item.Properties()));
    public static final RegistryObject<Item> TENDRIL_SIGIL = ITEMS.register("tendril_sigil",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.TendrilSigilItem(
                    new Item.Properties()));
    public static final RegistryObject<Item> VOICE_CURSE_AMULET = ITEMS.register("voice_curse_amulet",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.VoiceCurseAmuletItem(
                    new Item.Properties()));
    public static final RegistryObject<Item> SILENT_WITNESS_CLOAK = ITEMS.register("silent_witness_cloak",
            () -> new br.com.murilo.liberthia.cosmic.curse.HorrorItems.SilentWitnessCloakItem(
                    new Item.Properties()));

    // r46: ELDRITCH ARTIFACTS — 10 itens cosmic horror profundo
    public static final RegistryObject<Item> WATCHING_EYE = ITEMS.register("watching_eye",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.WatchingEyeItem(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_SIGNAL_RADIO = ITEMS.register("black_signal_radio",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.BlackSignalRadioItem(new Item.Properties()));
    public static final RegistryObject<Item> HOLLOW_MASK = ITEMS.register("hollow_mask",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.HollowMaskItem(new Item.Properties()));
    public static final RegistryObject<Item> FLESH_LANTERN = ITEMS.register("flesh_lantern",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.FleshLanternItem(new Item.Properties()));
    public static final RegistryObject<Item> FALSE_TOTEM = ITEMS.register("false_totem",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.FalseTotemItem(new Item.Properties()));
    public static final RegistryObject<Item> INFECTION_NEEDLE = ITEMS.register("infection_needle",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.InfectionNeedleItem(new Item.Properties()));
    public static final RegistryObject<Item> BOOK_IMPOSSIBLE = ITEMS.register("book_impossible",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.BookOfImpossibleGeometryItem(new Item.Properties()));
    public static final RegistryObject<Item> MIMIC_HEART = ITEMS.register("mimic_heart",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.MimicHeartItem(new Item.Properties()));
    public static final RegistryObject<Item> RED_TAPE = ITEMS.register("red_tape",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.RedTapeItem(new Item.Properties()));
    public static final RegistryObject<Item> NULL_BELL = ITEMS.register("null_bell",
            () -> new br.com.murilo.liberthia.cosmic.eldritch.EldritchArtifacts.NullBellItem(new Item.Properties()));

    // r47: LIVING SERVER ARTIFACTS — 4 itens interagindo com chunk memory + observation
    public static final RegistryObject<Item> GEOMETRY_KEY = ITEMS.register("geometry_key",
            () -> new br.com.murilo.liberthia.cosmic.living.LivingArtifacts.GeometryKeyItem(new Item.Properties()));
    public static final RegistryObject<Item> LOW_SIGNAL = ITEMS.register("low_signal",
            () -> new br.com.murilo.liberthia.cosmic.living.LivingArtifacts.LowSignalItem(new Item.Properties()));
    public static final RegistryObject<Item> PALE_THREAD = ITEMS.register("pale_thread",
            () -> new br.com.murilo.liberthia.cosmic.living.LivingArtifacts.PaleThreadItem(new Item.Properties()));
    public static final RegistryObject<Item> MIRROR_FRUIT = ITEMS.register("mirror_fruit",
            () -> new br.com.murilo.liberthia.cosmic.living.LivingArtifacts.MirrorFruitItem(new Item.Properties()));

    // r48: REFLECTION SEED — admin artifact que cria clone vivo do player
    public static final RegistryObject<Item> REFLECTION_SEED = ITEMS.register("reflection_seed",
            () -> new br.com.murilo.liberthia.cosmic.observatory.ReflectionSeedItem(new Item.Properties()));

    // r50: CARETAKER CONSOLE — GUI pra injetar fake chat msgs em target
    public static final RegistryObject<Item> CARETAKER_CONSOLE = ITEMS.register("caretaker_console",
            () -> new br.com.murilo.liberthia.cosmic.observatory.console.CaretakerConsoleItem(
                    new Item.Properties()));

    // r51: 10 ADMIN COSMIC ARTIFACTS — advanced horror tools
    public static final RegistryObject<Item> BLACK_VEIL = ITEMS.register("black_veil",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.BlackVeilItem(new Item.Properties()));
    public static final RegistryObject<Item> TENDRIL_CROWN = ITEMS.register("tendril_crown",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.TendrilCrownItem(new Item.Properties()));
    public static final RegistryObject<Item> FALSE_SUN = ITEMS.register("false_sun",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.FalseSunItem(new Item.Properties()));
    public static final RegistryObject<Item> MIRROR_PULSE = ITEMS.register("mirror_pulse",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.MirrorPulseItem(new Item.Properties()));
    public static final RegistryObject<Item> SILENT_BELL = ITEMS.register("silent_bell",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.SilentBellItem(new Item.Properties()));
    public static final RegistryObject<Item> OPEN_EYE = ITEMS.register("open_eye",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.OpenEyeItem(new Item.Properties()));
    public static final RegistryObject<Item> THREAD_OF_DISTANCE = ITEMS.register("thread_of_distance",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.ThreadOfDistanceItem(new Item.Properties()));
    public static final RegistryObject<Item> FLESH_SIGNAL = ITEMS.register("flesh_signal",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.FleshSignalItem(new Item.Properties()));
    public static final RegistryObject<Item> DEEP_WATER = ITEMS.register("deep_water",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.DeepWaterItem(new Item.Properties()));
    public static final RegistryObject<Item> AUDIENCE_MARK = ITEMS.register("audience_mark",
            () -> new br.com.murilo.liberthia.cosmic.admin.AdminCosmicArtifacts.AudienceMarkItem(new Item.Properties()));

    // r55: PALE WATCH ARTIFACTS — itens espirituais avançados
    public static final RegistryObject<Item> CLONE_ARMY = ITEMS.register("clone_army",
            () -> new br.com.murilo.liberthia.cosmic.palewatch.PaleWatchArtifacts.CloneArmyItem(new Item.Properties()));
    public static final RegistryObject<Item> STAREDOWN_PENDANT = ITEMS.register("staredown_pendant",
            () -> new br.com.murilo.liberthia.cosmic.palewatch.PaleWatchArtifacts.StaredownPendantItem(new Item.Properties()));
    public static final RegistryObject<Item> PALE_BLINK_PENDANT = ITEMS.register("pale_blink_pendant",
            () -> new br.com.murilo.liberthia.cosmic.palewatch.PaleWatchArtifacts.PaleBlinkPendantItem(new Item.Properties()));
    public static final RegistryObject<Item> PARALYZE_PENDANT = ITEMS.register("paralyze_pendant",
            () -> new br.com.murilo.liberthia.cosmic.palewatch.PaleWatchArtifacts.ParalyzePendantItem(new Item.Properties()));
    public static final RegistryObject<Item> SPIRIT_GUIDE = ITEMS.register("spirit_guide",
            () -> new br.com.murilo.liberthia.cosmic.palewatch.PaleWatchArtifacts.SpiritGuideItem(new Item.Properties()));

    // r55: Pale Ingots / Materials — minérios das dimensões espiritual/loom
    public static final RegistryObject<Item> PALE_IRON_INGOT = ITEMS.register("pale_iron_ingot",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SOULSTEEL_INGOT = ITEMS.register("soulsteel_ingot",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> RIFT_CRYSTAL = ITEMS.register("rift_crystal",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // r56: 15 NEW COSMIC ARTIFACTS (cryptic, mysterious)
    public static final RegistryObject<Item> PULLED_STRING = ITEMS.register("pulled_string",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.PulledStringItem(new Item.Properties()));
    public static final RegistryObject<Item> QUIET_MARK = ITEMS.register("quiet_mark",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.QuietMarkItem(new Item.Properties()));
    public static final RegistryObject<Item> LONELY_ECHO = ITEMS.register("lonely_echo",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.LonelyEchoItem(new Item.Properties()));
    public static final RegistryObject<Item> FOLDED_DISTANCE = ITEMS.register("folded_distance",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.FoldedDistanceItem(new Item.Properties()));
    public static final RegistryObject<Item> THROAT_SALT = ITEMS.register("throat_salt",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.ThroatSaltItem(new Item.Properties()));
    public static final RegistryObject<Item> SOFT_WOUND = ITEMS.register("soft_wound",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.SoftWoundItem(new Item.Properties()));
    public static final RegistryObject<Item> LOOKING_GLASS = ITEMS.register("looking_glass",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.LookingGlassItem(new Item.Properties()));
    public static final RegistryObject<Item> HALF_STEP = ITEMS.register("half_step",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.HalfStepItem(new Item.Properties()));
    public static final RegistryObject<Item> BENT_IRON = ITEMS.register("bent_iron",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.BentIronItem(new Item.Properties()));
    public static final RegistryObject<Item> PALE_COIN = ITEMS.register("pale_coin",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.PaleCoinItem(new Item.Properties()));
    public static final RegistryObject<Item> WET_BELL = ITEMS.register("wet_bell",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.WetBellItem(new Item.Properties()));
    public static final RegistryObject<Item> MARROW_WHISTLE = ITEMS.register("marrow_whistle",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.MarrowWhistleItem(new Item.Properties()));
    public static final RegistryObject<Item> LISTENING_GLASS = ITEMS.register("listening_glass",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.ListeningGlassItem(new Item.Properties()));
    public static final RegistryObject<Item> SUNKEN_RING = ITEMS.register("sunken_ring",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.SunkenRingItem(new Item.Properties()));
    public static final RegistryObject<Item> HAND_ON_GLASS = ITEMS.register("hand_on_glass",
            () -> new br.com.murilo.liberthia.cosmic.artifacts.CosmicArtifactsR56.HandOnGlassItem(new Item.Properties()));

    // r56: EXODUS BOOK — legendary escape from Tome Curse
    public static final RegistryObject<Item> EXODUS_BOOK = ITEMS.register("exodus_book",
            () -> new br.com.murilo.liberthia.cosmic.exodus.ExodusBookItem(new Item.Properties()));

    // r56: 6 NEW SPIRIT ORES (items só — blocks viriam depois)
    public static final RegistryObject<Item> SOULITE_SHARD = ITEMS.register("soulite_shard",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> PALE_CRYSTAL_SHARD = ITEMS.register("pale_crystal_shard",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> VEINSTONE_FRAGMENT = ITEMS.register("veinstone_fragment",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> HOLLOW_SILVER_NUGGET = ITEMS.register("hollow_silver_nugget",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> MOURNING_EMBER = ITEMS.register("mourning_ember",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> GHOST_QUARTZ_SHARD = ITEMS.register("ghost_quartz_shard",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r61: OBSERVATION CASTING
    public static final RegistryObject<Item> SPELLSWORD = ITEMS.register("spellsword",
            () -> new br.com.murilo.liberthia.observation.item.SpellswordItem(new Item.Properties()));
    public static final RegistryObject<Item> SOURCE_GEM = ITEMS.register("source_gem",
            () -> new br.com.murilo.liberthia.observation.item.SourceGemItem(new Item.Properties()));

    // r62: Observation Tome + Source Jar block item
    public static final RegistryObject<Item> OBSERVATION_TOME = ITEMS.register("observation_tome",
            () -> new br.com.murilo.liberthia.observation.item.ObservationTomeItem(new Item.Properties()));
    public static final RegistryObject<Item> SOURCE_JAR_ITEM = ITEMS.register("source_jar",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SOURCE_JAR.get(), new Item.Properties()));

    // r64: Grimório de Observação — projectile spawner real
    public static final RegistryObject<Item> GRIMOIRE_OF_OBSERVATION = ITEMS.register("grimoire_of_observation",
            () -> new br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem(new Item.Properties()));

    // ════════════════════════════════════════════════════════════════════════
    // r68: GLYPH ITEMS — 23 itens físicos (15 existentes + 8 AN-portados)
    // Pattern AN's Glyph: cada AbstractSpellPart tem seu Glyph item correspondente
    // ════════════════════════════════════════════════════════════════════════

    private static RegistryObject<Item> registerGlyph(String itemId, String partId) {
        return ITEMS.register(itemId, () ->
            new br.com.murilo.liberthia.observation.item.GlyphItem(new Item.Properties(), partId));
    }

    // 4 Watch Methods originais (r60/r61)
    public static final RegistryObject<Item> GLYPH_DIRECT_GAZE      = registerGlyph("glyph_direct_gaze",      "liberthia:direct_gaze");
    public static final RegistryObject<Item> GLYPH_WATCH_PERIPHERAL = registerGlyph("glyph_watch_peripheral", "liberthia:watch/peripheral");
    public static final RegistryObject<Item> GLYPH_WATCH_MEMORY     = registerGlyph("glyph_watch_memory",     "liberthia:watch/memory");
    public static final RegistryObject<Item> GLYPH_WATCH_SILENCE    = registerGlyph("glyph_watch_silence",    "liberthia:watch/silence");
    public static final RegistryObject<Item> GLYPH_WATCH_REFLECTION = registerGlyph("glyph_watch_reflection", "liberthia:watch/reflection");

    // 6 Manifestations originais (r60/r61)
    public static final RegistryObject<Item> GLYPH_TENDRIL          = registerGlyph("glyph_tendril",          "liberthia:tendril_manifestation");
    public static final RegistryObject<Item> GLYPH_MANIFEST_SILENCE = registerGlyph("glyph_manifest_silence", "liberthia:manifest/silence");
    public static final RegistryObject<Item> GLYPH_MANIFEST_MIRROR  = registerGlyph("glyph_manifest_mirror",  "liberthia:manifest/mirror");
    public static final RegistryObject<Item> GLYPH_MANIFEST_WHISPER = registerGlyph("glyph_manifest_whisper", "liberthia:manifest/whisper");
    public static final RegistryObject<Item> GLYPH_MANIFEST_DECAY   = registerGlyph("glyph_manifest_decay",   "liberthia:manifest/decay");
    public static final RegistryObject<Item> GLYPH_MANIFEST_GLIMPSE = registerGlyph("glyph_manifest_glimpse", "liberthia:manifest/glimpse");

    // 4 Distortions originais (r60/r61)
    public static final RegistryObject<Item> GLYPH_AMPLIFY          = registerGlyph("glyph_amplify",          "liberthia:amplify_distortion");
    public static final RegistryObject<Item> GLYPH_LINGER           = registerGlyph("glyph_linger",           "liberthia:distort/linger");
    public static final RegistryObject<Item> GLYPH_ECHO             = registerGlyph("glyph_echo",             "liberthia:distort/echo");
    public static final RegistryObject<Item> GLYPH_SECRET           = registerGlyph("glyph_secret",           "liberthia:distort/secret");

    // 2 Methods AN-portados (r68)
    public static final RegistryObject<Item> GLYPH_METHOD_TOUCH     = registerGlyph("glyph_method_touch",     "liberthia:method/touch");
    public static final RegistryObject<Item> GLYPH_METHOD_SELF      = registerGlyph("glyph_method_self",      "liberthia:method/self");

    // 6 Effects AN-portados (r68)
    public static final RegistryObject<Item> GLYPH_EFFECT_IGNITE    = registerGlyph("glyph_effect_ignite",    "liberthia:effect/ignite");
    public static final RegistryObject<Item> GLYPH_EFFECT_HARM      = registerGlyph("glyph_effect_harm",      "liberthia:effect/harm");
    public static final RegistryObject<Item> GLYPH_EFFECT_HEAL      = registerGlyph("glyph_effect_heal",      "liberthia:effect/heal");
    public static final RegistryObject<Item> GLYPH_EFFECT_FREEZE    = registerGlyph("glyph_effect_freeze",    "liberthia:effect/freeze");
    public static final RegistryObject<Item> GLYPH_EFFECT_LAUNCH    = registerGlyph("glyph_effect_launch",    "liberthia:effect/launch");
    public static final RegistryObject<Item> GLYPH_EFFECT_SLOWFALL  = registerGlyph("glyph_effect_slowfall",  "liberthia:effect/slowfall");

    // r70: 12 GLYPHS ADICIONAIS (5 Methods + 7 Effects portados AN)
    public static final RegistryObject<Item> GLYPH_METHOD_LASER  = registerGlyph("glyph_method_laser",  "liberthia:method/laser");
    public static final RegistryObject<Item> GLYPH_METHOD_BURST  = registerGlyph("glyph_method_burst",  "liberthia:method/burst");
    public static final RegistryObject<Item> GLYPH_METHOD_ORBIT  = registerGlyph("glyph_method_orbit",  "liberthia:method/orbit");
    public static final RegistryObject<Item> GLYPH_METHOD_WALL   = registerGlyph("glyph_method_wall",   "liberthia:method/wall");
    public static final RegistryObject<Item> GLYPH_METHOD_CHAIN  = registerGlyph("glyph_method_chain",  "liberthia:method/chain");
    public static final RegistryObject<Item> GLYPH_EFFECT_LIGHTNING = registerGlyph("glyph_effect_lightning", "liberthia:effect/lightning");
    public static final RegistryObject<Item> GLYPH_EFFECT_GRAVITY   = registerGlyph("glyph_effect_gravity",   "liberthia:effect/gravity");
    public static final RegistryObject<Item> GLYPH_EFFECT_BLIND     = registerGlyph("glyph_effect_blind",     "liberthia:effect/blind");
    public static final RegistryObject<Item> GLYPH_EFFECT_LEVITATE  = registerGlyph("glyph_effect_levitate",  "liberthia:effect/levitate");
    public static final RegistryObject<Item> GLYPH_EFFECT_KNOCKBACK = registerGlyph("glyph_effect_knockback", "liberthia:effect/knockback");
    public static final RegistryObject<Item> GLYPH_EFFECT_EXPLOSION = registerGlyph("glyph_effect_explosion", "liberthia:effect/explosion");
    public static final RegistryObject<Item> GLYPH_EFFECT_FANGS     = registerGlyph("glyph_effect_fangs",     "liberthia:effect/fangs");

    // r68: Spell Parchment — armazena recipe construída no NBT
    public static final RegistryObject<Item> SPELL_PARCHMENT = ITEMS.register("spell_parchment",
            () -> new br.com.murilo.liberthia.observation.item.SpellParchmentItem(new Item.Properties()));

    // r138: Caster Wand — varinha customizável que bind 1 spell via offhand scroll
    public static final RegistryObject<Item> CASTER_WAND = ITEMS.register("caster_wand",
            () -> new br.com.murilo.liberthia.magic.spell.wand.CasterWandItem(new Item.Properties()));

    // r138: Lay Line — block item (rare drop, normally only worldgen)
    public static final RegistryObject<Item> LAY_LINE_ITEM = ITEMS.register("lay_line",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.LAY_LINE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // r138: Spell Mutator — bloco que combina 2 scrolls
    public static final RegistryObject<Item> SPELL_MUTATOR_ITEM = ITEMS.register("spell_mutator",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPELL_MUTATOR.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r155 Phase 2: Arcane Workbench BlockItem
    public static final RegistryObject<Item> ARCANE_WORKBENCH_ITEM = ITEMS.register("arcane_workbench",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ARCANE_WORKBENCH.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // r162: Class Pedestal BlockItem
    public static final RegistryObject<Item> CLASS_PEDESTAL_ITEM = ITEMS.register("class_pedestal",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CLASS_PEDESTAL.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // r139: Spirit World ores + plantas (block items)
    public static final RegistryObject<Item> SPIRIT_CRYSTAL_ORE_ITEM = ITEMS.register("spirit_crystal_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPIRIT_CRYSTAL_ORE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> SOUL_IRON_ORE_ITEM = ITEMS.register("soul_iron_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SOUL_IRON_ORE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> WHISPER_PETAL_BUSH_ITEM = ITEMS.register("whisper_petal_bush",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.WHISPER_PETAL_BUSH.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> GHOST_MUSHROOM_BLOCK_ITEM = ITEMS.register("ghost_mushroom_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GHOST_MUSHROOM.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // r139: Glyph Brazier — bloco onde player faz rituais
    public static final RegistryObject<Item> GLYPH_BRAZIER_ITEM = ITEMS.register("glyph_brazier",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GLYPH_BRAZIER.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r69: Scribes Table — block item
    public static final RegistryObject<Item> SCRIBES_TABLE_ITEM = ITEMS.register("scribes_table",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SCRIBES_TABLE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r71: 4 Source upgrade items
    public static final RegistryObject<Item> SOURCE_CRYSTAL = ITEMS.register("source_crystal",
            () -> new br.com.murilo.liberthia.observation.item.SourceUpgradeItems.SourceCrystalItem(new Item.Properties()));
    public static final RegistryObject<Item> SOURCE_CATALYST = ITEMS.register("source_catalyst",
            () -> new br.com.murilo.liberthia.observation.item.SourceUpgradeItems.SourceCatalystItem(new Item.Properties()));
    public static final RegistryObject<Item> SOURCE_LENS = ITEMS.register("source_lens",
            () -> new br.com.murilo.liberthia.observation.item.SourceUpgradeItems.SourceLensItem(new Item.Properties()));
    public static final RegistryObject<Item> SOUL_FRAGMENT = ITEMS.register("soul_fragment",
            () -> new br.com.murilo.liberthia.observation.item.SourceUpgradeItems.SoulFragmentItem(new Item.Properties()));

    // r71: 25 glyphs elementais (5 por classe: Fire/Water/Earth/Air/Cosmic)
    // FIRE (5)
    public static final RegistryObject<Item> GLYPH_FIREBALL        = registerGlyph("glyph_fireball",        "liberthia:elem/fireball");
    public static final RegistryObject<Item> GLYPH_INFERNO         = registerGlyph("glyph_inferno",         "liberthia:elem/inferno");
    public static final RegistryObject<Item> GLYPH_CLEANSING_FLAME = registerGlyph("glyph_cleansing_flame", "liberthia:elem/cleansing_flame");
    public static final RegistryObject<Item> GLYPH_SOLAR_PULSE     = registerGlyph("glyph_solar_pulse",     "liberthia:elem/solar_pulse");
    public static final RegistryObject<Item> GLYPH_BURNING_AURA    = registerGlyph("glyph_burning_aura",    "liberthia:elem/burning_aura");
    // WATER (5)
    public static final RegistryObject<Item> GLYPH_BUBBLE_SHIELD   = registerGlyph("glyph_bubble_shield",   "liberthia:elem/bubble_shield");
    public static final RegistryObject<Item> GLYPH_TIDAL_WAVE      = registerGlyph("glyph_tidal_wave",      "liberthia:elem/tidal_wave");
    public static final RegistryObject<Item> GLYPH_FROST_LANCE     = registerGlyph("glyph_frost_lance",     "liberthia:elem/frost_lance");
    public static final RegistryObject<Item> GLYPH_MIST_VEIL       = registerGlyph("glyph_mist_veil",       "liberthia:elem/mist_veil");
    public static final RegistryObject<Item> GLYPH_HEALING_RAIN    = registerGlyph("glyph_healing_rain",    "liberthia:elem/healing_rain");
    // EARTH (5)
    public static final RegistryObject<Item> GLYPH_STONE_SPIKES    = registerGlyph("glyph_stone_spikes",    "liberthia:elem/stone_spikes");
    public static final RegistryObject<Item> GLYPH_QUAKE_STEP      = registerGlyph("glyph_quake_step",      "liberthia:elem/quake_step");
    public static final RegistryObject<Item> GLYPH_VEIN_SIGHT      = registerGlyph("glyph_vein_sight",      "liberthia:elem/vein_sight");
    public static final RegistryObject<Item> GLYPH_EARTHEN_WALL    = registerGlyph("glyph_earthen_wall",    "liberthia:elem/earthen_wall");
    public static final RegistryObject<Item> GLYPH_ROOTS           = registerGlyph("glyph_roots",           "liberthia:elem/roots");
    // AIR (5)
    public static final RegistryObject<Item> GLYPH_GUST            = registerGlyph("glyph_gust",            "liberthia:elem/gust");
    public static final RegistryObject<Item> GLYPH_TORNADO         = registerGlyph("glyph_tornado",         "liberthia:elem/tornado");
    public static final RegistryObject<Item> GLYPH_SKY_STEP        = registerGlyph("glyph_sky_step",        "liberthia:elem/sky_step");
    public static final RegistryObject<Item> GLYPH_VELOCITY        = registerGlyph("glyph_velocity",        "liberthia:elem/velocity");
    public static final RegistryObject<Item> GLYPH_WIND_CUTTER     = registerGlyph("glyph_wind_cutter",     "liberthia:elem/wind_cutter");
    // COSMIC (5)
    public static final RegistryObject<Item> GLYPH_VOID_PULL       = registerGlyph("glyph_void_pull",       "liberthia:elem/void_pull");
    public static final RegistryObject<Item> GLYPH_DREAD_STARE     = registerGlyph("glyph_dread_stare",     "liberthia:elem/dread_stare");
    public static final RegistryObject<Item> GLYPH_MIND_SPIKE      = registerGlyph("glyph_mind_spike",      "liberthia:elem/mind_spike");
    public static final RegistryObject<Item> GLYPH_REALITY_TEAR    = registerGlyph("glyph_reality_tear",    "liberthia:elem/reality_tear");
    public static final RegistryObject<Item> GLYPH_SINGULARITY     = registerGlyph("glyph_singularity",     "liberthia:elem/singularity");

    // r72: 10 UtilityGlyphs (7 effects + 3 augments)
    public static final RegistryObject<Item> GLYPH_PLACE_BLOCK   = registerGlyph("glyph_place_block",   "liberthia:util/place_block");
    public static final RegistryObject<Item> GLYPH_BREAK_BLOCK   = registerGlyph("glyph_break_block",   "liberthia:util/break_block");
    public static final RegistryObject<Item> GLYPH_CONJURE_WATER = registerGlyph("glyph_conjure_water", "liberthia:util/conjure_water");
    public static final RegistryObject<Item> GLYPH_LIGHT         = registerGlyph("glyph_light",         "liberthia:util/light");
    public static final RegistryObject<Item> GLYPH_SNARE         = registerGlyph("glyph_snare",         "liberthia:util/snare");
    public static final RegistryObject<Item> GLYPH_HEX           = registerGlyph("glyph_hex",           "liberthia:util/hex");
    public static final RegistryObject<Item> GLYPH_PICKUP        = registerGlyph("glyph_pickup",        "liberthia:util/pickup");
    public static final RegistryObject<Item> GLYPH_PIERCE        = registerGlyph("glyph_pierce",        "liberthia:aug/pierce");
    public static final RegistryObject<Item> GLYPH_SPLIT         = registerGlyph("glyph_split",         "liberthia:aug/split");
    public static final RegistryObject<Item> GLYPH_AOE           = registerGlyph("glyph_aoe",           "liberthia:aug/aoe");

    // r72: Observation Chalk item (desenha runas) — renomeado pra não colidir com CHALK existente
    public static final RegistryObject<Item> OBSERVATION_CHALK = ITEMS.register("observation_chalk",
            () -> new br.com.murilo.liberthia.observation.item.ChalkItem(new Item.Properties()));

    // r72: Rune block item
    public static final RegistryObject<Item> RUNE_BLOCK_ITEM = ITEMS.register("rune_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RUNE_BLOCK.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r73: Imbuement Table block item
    public static final RegistryObject<Item> IMBUEMENT_TABLE_ITEM = ITEMS.register("imbuement_table",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.IMBUEMENT_TABLE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // r74: Sourcestone + Spirit Gem ores
    public static final RegistryObject<Item> SOURCESTONE_ORE_ITEM = ITEMS.register("sourcestone_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SOURCESTONE_ORE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SPIRIT_GEM_ORE_ITEM = ITEMS.register("spirit_gem_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPIRIT_GEM_ORE.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r74: Mana Berry — bush + comestible
    public static final RegistryObject<Item> MANA_BERRY_BUSH_ITEM = ITEMS.register("mana_berry_bush",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MANA_BERRY_BUSH.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> MANA_BERRY = ITEMS.register("mana_berry",
            () -> new br.com.murilo.liberthia.observation.item.ManaBerryItem(new Item.Properties()));

    // r74: Source Relay block item
    public static final RegistryObject<Item> SOURCE_RELAY_ITEM = ITEMS.register("source_relay",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SOURCE_RELAY.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r74: 3 Spell Book Tiers
    public static final RegistryObject<Item> GRIMOIRE_APPRENTICE = ITEMS.register("grimoire_apprentice",
            () -> new br.com.murilo.liberthia.observation.item.GrimoireTierItem.Apprentice(new Item.Properties()));
    public static final RegistryObject<Item> GRIMOIRE_MASTER = ITEMS.register("grimoire_master",
            () -> new br.com.murilo.liberthia.observation.item.GrimoireTierItem.Master(new Item.Properties()));
    public static final RegistryObject<Item> GRIMOIRE_ARCHMAGE = ITEMS.register("grimoire_archmage",
            () -> new br.com.murilo.liberthia.observation.item.GrimoireTierItem.Archmage(new Item.Properties()));

    // r74: Bookwyrm Spawn Egg
    public static final RegistryObject<Item> BOOKWYRM_SPAWN_EGG = ITEMS.register("bookwyrm_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    br.com.murilo.liberthia.registry.ModEntities.BOOKWYRM,
                    0x9D4DD6, 0xFFEE66,
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r77: Spell Binding Pedestal block item — combina Book + Parchment → Output
    public static final RegistryObject<Item> SPELL_BINDING_PEDESTAL_ITEM = ITEMS.register("spell_binding_pedestal",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPELL_BINDING_PEDESTAL.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r72: 8 Prebuilt Spell Tomes
    public static final RegistryObject<Item> TOME_PYROMANCER = ITEMS.register("tome_pyromancer",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::pyromancer));
    public static final RegistryObject<Item> TOME_FROSTBINDER = ITEMS.register("tome_frostbinder",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::frostbinder));
    public static final RegistryObject<Item> TOME_SKYWALKER = ITEMS.register("tome_skywalker",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::skywalker));
    public static final RegistryObject<Item> TOME_WEBWEAVER = ITEMS.register("tome_webweaver",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::webweaver));
    public static final RegistryObject<Item> TOME_DEATH_BEAM = ITEMS.register("tome_death_beam",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::deathBeam));
    public static final RegistryObject<Item> TOME_HEALING_LIGHT = ITEMS.register("tome_healing_light",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::healingLight));
    public static final RegistryObject<Item> TOME_DASH = ITEMS.register("tome_dash",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::dash));
    public static final RegistryObject<Item> TOME_SINGULARITY = ITEMS.register("tome_singularity",
            () -> new br.com.murilo.liberthia.observation.item.PrebuiltTomeItem(new Item.Properties(),
                br.com.murilo.liberthia.observation.item.PrebuiltTomeItem.Tomes::singularityCombo));

    // r57: LIMINAL DIMENSION ENTRY KEYS
    public static final RegistryObject<Item> DROWNED_COMPASS = ITEMS.register("drowned_compass",
            () -> new br.com.murilo.liberthia.dimension.LiminalEntryItems.DrownedCompassItem(new Item.Properties()));
    public static final RegistryObject<Item> FOLDED_ADDRESS = ITEMS.register("folded_address",
            () -> new br.com.murilo.liberthia.dimension.LiminalEntryItems.FoldedAddressItem(new Item.Properties()));
    public static final RegistryObject<Item> BARK_TOKEN = ITEMS.register("bark_token",
            () -> new br.com.murilo.liberthia.dimension.LiminalEntryItems.BarkTokenItem(new Item.Properties()));

    // r56: 6 NEW LOOM ORES
    public static final RegistryObject<Item> NULL_IRON_SHARD = ITEMS.register("null_iron_shard",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> ABYSSIUM_DUST = ITEMS.register("abyssium_dust",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> BLACK_STAR_CORE = ITEMS.register("black_star_core",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> DISTORTION_CRYSTAL = ITEMS.register("distortion_crystal",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> VOID_GOLD_NUGGET = ITEMS.register("void_gold_nugget",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> EYE_STONE_SHARD = ITEMS.register("eye_stone_shard",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    // ════════════════════════════════════════════════════════════════════════
    // r81: HORROR FRAMEWORK ITEMS — 4 artifacts dos 18 sistemas de horror
    // ════════════════════════════════════════════════════════════════════════

    /** Mirror Mask — Uncanny Valley artifact, parece outro player. */
    public static final RegistryObject<Item> MIRROR_MASK = ITEMS.register("mirror_mask",
            () -> new br.com.murilo.liberthia.cosmic.horror.item.MirrorMaskItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** Fractured Scripture — Cognitive Horror, dá XP em troca de sanidade. */
    public static final RegistryObject<Item> FRACTURED_SCRIPTURE = ITEMS.register("fractured_scripture",
            () -> new br.com.murilo.liberthia.cosmic.horror.item.FracturedScriptureItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    /** Halo of Abaddon — Religious Horror, halo angélico orbitando. */
    public static final RegistryObject<Item> HALO_OF_ABADDON = ITEMS.register("halo_of_abaddon",
            () -> new br.com.murilo.liberthia.cosmic.horror.item.HaloOfAbaddonItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** Veinbound Chestplate — Flesh Horror, regenera HP + sussurra. */
    public static final RegistryObject<Item> VEINBOUND_CHESTPLATE = ITEMS.register("veinbound_chestplate",
            () -> new br.com.murilo.liberthia.cosmic.horror.item.VeinboundChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** r85: Scrying Lens — highlight de blocos através de paredes. */
    public static final RegistryObject<Item> SCRYING_LENS = ITEMS.register("scrying_lens",
            () -> new br.com.murilo.liberthia.magic.scrying.ScryingLensItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r86: RITUAL TABLETS — 8 tablets, um pra cada ritual
    public static final RegistryObject<Item> RITUAL_TABLET_SUNRISE = ITEMS.register("ritual_tablet_sunrise",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "sunrise"));
    public static final RegistryObject<Item> RITUAL_TABLET_MOONFALL = ITEMS.register("ritual_tablet_moonfall",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "moonfall"));
    public static final RegistryObject<Item> RITUAL_TABLET_FLIGHT = ITEMS.register("ritual_tablet_flight",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "flight_zone"));
    public static final RegistryObject<Item> RITUAL_TABLET_HEALING = ITEMS.register("ritual_tablet_healing",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "healing_aura"));
    public static final RegistryObject<Item> RITUAL_TABLET_FORESTATION = ITEMS.register("ritual_tablet_forestation",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "forestation"));
    public static final RegistryObject<Item> RITUAL_TABLET_WARDING = ITEMS.register("ritual_tablet_warding",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "warding"));
    public static final RegistryObject<Item> RITUAL_TABLET_AWAKENING = ITEMS.register("ritual_tablet_awakening",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "awakening"));
    public static final RegistryObject<Item> RITUAL_TABLET_SCRYING = ITEMS.register("ritual_tablet_scrying",
            () -> new br.com.murilo.liberthia.magic.ritual.RitualTabletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "scrying"));

    // r88: Dominion Wand
    public static final RegistryObject<Item> DOMINION_WAND = ITEMS.register("dominion_wand",
            () -> new br.com.murilo.liberthia.automation.DominionWandItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r92: Storage / Travel
    public static final RegistryObject<Item> VOID_JAR = ITEMS.register("void_jar",
            () -> new br.com.murilo.liberthia.storage.VoidJarItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> WARP_SCROLL = ITEMS.register("warp_scroll",
            () -> new br.com.murilo.liberthia.storage.WarpScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), false));
    public static final RegistryObject<Item> STABLE_WARP_SCROLL = ITEMS.register("stable_warp_scroll",
            () -> new br.com.murilo.liberthia.storage.WarpScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), true));

    // r93: 7 School Scrolls
    public static final RegistryObject<Item> SCROLL_FIRE = ITEMS.register("scroll_fire",
            () -> new br.com.murilo.liberthia.magic.scroll.SchoolScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    br.com.murilo.liberthia.magic.school.SpellSchool.FIRE));
    public static final RegistryObject<Item> SCROLL_ICE = ITEMS.register("scroll_ice",
            () -> new br.com.murilo.liberthia.magic.scroll.SchoolScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ICE));
    public static final RegistryObject<Item> SCROLL_LIGHTNING = ITEMS.register("scroll_lightning",
            () -> new br.com.murilo.liberthia.magic.scroll.SchoolScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.LIGHTNING));
    public static final RegistryObject<Item> SCROLL_BLOOD = ITEMS.register("scroll_blood",
            () -> new br.com.murilo.liberthia.magic.scroll.SchoolScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.BLOOD));
    public static final RegistryObject<Item> SCROLL_ELDRITCH = ITEMS.register("scroll_eldritch",
            () -> new br.com.murilo.liberthia.magic.scroll.SchoolScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ELDRITCH));
    public static final RegistryObject<Item> SCROLL_HOLY = ITEMS.register("scroll_holy",
            () -> new br.com.murilo.liberthia.magic.scroll.SchoolScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY));
    public static final RegistryObject<Item> SCROLL_NATURE = ITEMS.register("scroll_nature",
            () -> new br.com.murilo.liberthia.magic.scroll.SchoolScrollItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    br.com.murilo.liberthia.magic.school.SpellSchool.NATURE));

    // r93: 5 Affinity Rings
    public static final RegistryObject<Item> RING_FIREWARP = ITEMS.register("ring_firewarp",
            () -> new br.com.murilo.liberthia.magic.affinity.AffinityRingItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.affinity.AffinityRingItem.Type.FIREWARP));
    public static final RegistryObject<Item> RING_LURKER = ITEMS.register("ring_lurker",
            () -> new br.com.murilo.liberthia.magic.affinity.AffinityRingItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.affinity.AffinityRingItem.Type.LURKER));
    public static final RegistryObject<Item> RING_TELEPORT = ITEMS.register("ring_teleport",
            () -> new br.com.murilo.liberthia.magic.affinity.AffinityRingItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.affinity.AffinityRingItem.Type.TELEPORT));
    public static final RegistryObject<Item> RING_CAPACITY = ITEMS.register("ring_capacity",
            () -> new br.com.murilo.liberthia.magic.affinity.AffinityRingItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.affinity.AffinityRingItem.Type.CAPACITY));
    public static final RegistryObject<Item> RING_BLOODBORN = ITEMS.register("ring_bloodborn",
            () -> new br.com.murilo.liberthia.magic.affinity.AffinityRingItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.affinity.AffinityRingItem.Type.BLOODBORN));

    // r93: Arcane Salvage — drop de wizard mobs
    public static final RegistryObject<Item> ARCANE_SALVAGE = ITEMS.register("arcane_salvage",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // r91: Counterspell — interrompe wizards próximos
    public static final RegistryObject<Item> COUNTERSPELL = ITEMS.register("counterspell",
            () -> new br.com.murilo.liberthia.magic.casting.CounterspellItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r90: 15 Perk Threads
    public static final RegistryObject<Item> PERK_JUMP = ITEMS.register("perk_jump",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "jump"));
    public static final RegistryObject<Item> PERK_STEP_HEIGHT = ITEMS.register("perk_step_height",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "step_height"));
    public static final RegistryObject<Item> PERK_REPAIRING = ITEMS.register("perk_repairing",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "repairing"));
    public static final RegistryObject<Item> PERK_VAMPIRIC = ITEMS.register("perk_vampiric",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "vampiric"));
    public static final RegistryObject<Item> PERK_MAGIC_RESIST = ITEMS.register("perk_magic_resist",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "magic_resist"));
    public static final RegistryObject<Item> PERK_TOUGHNESS = ITEMS.register("perk_toughness",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "toughness"));
    public static final RegistryObject<Item> PERK_FEATHER = ITEMS.register("perk_feather",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "feather"));
    public static final RegistryObject<Item> PERK_GLIDING = ITEMS.register("perk_gliding",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "gliding"));
    public static final RegistryObject<Item> PERK_MAGIC_CAPACITY = ITEMS.register("perk_magic_capacity",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "magic_capacity"));
    public static final RegistryObject<Item> PERK_SPELL_DAMAGE = ITEMS.register("perk_spell_damage",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "spell_damage"));
    public static final RegistryObject<Item> PERK_SATURATION = ITEMS.register("perk_saturation",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "saturation"));
    public static final RegistryObject<Item> PERK_KNOCKBACK_RESIST = ITEMS.register("perk_knockback_resist",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "knockback_resist"));
    public static final RegistryObject<Item> PERK_POTION_DURATION = ITEMS.register("perk_potion_duration",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties(), "potion_duration"));
    public static final RegistryObject<Item> PERK_LOOTING = ITEMS.register("perk_looting",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "looting"));
    public static final RegistryObject<Item> PERK_BONDED = ITEMS.register("perk_bonded",
            () -> new br.com.murilo.liberthia.magic.perk.PerkThreadItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), "bonded"));

    // ════════════════════════════════════════════════════════════════════════
    // r94: STAFF WEAPONS — 7 escolas, com cooldowns variados
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Item> STAFF_FIRE = ITEMS.register("staff_fire",
            () -> new br.com.murilo.liberthia.magic.weapon.StaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.FIRE, 20, 4.0F));
    public static final RegistryObject<Item> STAFF_ICE = ITEMS.register("staff_ice",
            () -> new br.com.murilo.liberthia.magic.weapon.StaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ICE, 18, 3.0F));
    public static final RegistryObject<Item> STAFF_LIGHTNING = ITEMS.register("staff_lightning",
            () -> new br.com.murilo.liberthia.magic.weapon.StaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.LIGHTNING, 30, 4.0F));
    public static final RegistryObject<Item> STAFF_BLOOD = ITEMS.register("staff_blood",
            () -> new br.com.murilo.liberthia.magic.weapon.StaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.BLOOD, 25, 5.0F));
    public static final RegistryObject<Item> STAFF_ELDRITCH = ITEMS.register("staff_eldritch",
            () -> new br.com.murilo.liberthia.magic.weapon.StaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ELDRITCH, 28, 5.0F));
    public static final RegistryObject<Item> STAFF_HOLY = ITEMS.register("staff_holy",
            () -> new br.com.murilo.liberthia.magic.weapon.StaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY, 22, 4.0F));
    public static final RegistryObject<Item> STAFF_NATURE = ITEMS.register("staff_nature",
            () -> new br.com.murilo.liberthia.magic.weapon.StaffItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.school.SpellSchool.NATURE, 16, 3.0F));

    // r94: 5 Upgrade Orbs
    public static final RegistryObject<Item> ORB_MANA_BOOST = ITEMS.register("orb_mana_boost",
            () -> new br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem.Type.MANA_BOOST));
    public static final RegistryObject<Item> ORB_COOLDOWN = ITEMS.register("orb_cooldown",
            () -> new br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem.Type.COOLDOWN_REDUCTION));
    public static final RegistryObject<Item> ORB_SPELL_DAMAGE = ITEMS.register("orb_spell_damage",
            () -> new br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem.Type.SPELL_DAMAGE));
    public static final RegistryObject<Item> ORB_HEALTH = ITEMS.register("orb_health",
            () -> new br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem.Type.HEALTH));
    public static final RegistryObject<Item> ORB_SOURCE_REGEN = ITEMS.register("orb_source_regen",
            () -> new br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem.Type.SOURCE_REGEN));

    // r94: 7 School Focus — crafting reagent
    public static final RegistryObject<Item> FOCUS_FIRE = ITEMS.register("focus_fire",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FOCUS_ICE = ITEMS.register("focus_ice",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FOCUS_LIGHTNING = ITEMS.register("focus_lightning",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FOCUS_BLOOD = ITEMS.register("focus_blood",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> FOCUS_ELDRITCH = ITEMS.register("focus_eldritch",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> FOCUS_HOLY = ITEMS.register("focus_holy",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> FOCUS_NATURE = ITEMS.register("focus_nature",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // r96: Iconic spell items
    public static final RegistryObject<Item> SPECTRAL_HAMMER = ITEMS.register("spectral_hammer",
            () -> new br.com.murilo.liberthia.magic.spells.SpectralHammerItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final RegistryObject<Item> LIGHTNING_LANCE = ITEMS.register("lightning_lance",
            () -> new br.com.murilo.liberthia.magic.spells.LightningLanceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> DEVOUR_JAW = ITEMS.register("devour_jaw",
            () -> new br.com.murilo.liberthia.magic.spells.DevourJawItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r97: JarOfLight + EnchantersGauntlet
    public static final RegistryObject<Item> JAR_OF_LIGHT = ITEMS.register("jar_of_light",
            () -> new br.com.murilo.liberthia.storage.JarOfLightItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ENCHANTERS_GAUNTLET = ITEMS.register("enchanters_gauntlet",
            () -> new br.com.murilo.liberthia.magic.weapon.EnchantersGauntletItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r101: 7 Wizard Chestplates
    public static final RegistryObject<Item> WIZARD_CHEST_FIRE = ITEMS.register("wizard_chest_fire",
            () -> new br.com.murilo.liberthia.magic.armor.WizardChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.FIRE));
    public static final RegistryObject<Item> WIZARD_CHEST_ICE = ITEMS.register("wizard_chest_ice",
            () -> new br.com.murilo.liberthia.magic.armor.WizardChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ICE));
    public static final RegistryObject<Item> WIZARD_CHEST_LIGHTNING = ITEMS.register("wizard_chest_lightning",
            () -> new br.com.murilo.liberthia.magic.armor.WizardChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.LIGHTNING));
    public static final RegistryObject<Item> WIZARD_CHEST_BLOOD = ITEMS.register("wizard_chest_blood",
            () -> new br.com.murilo.liberthia.magic.armor.WizardChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.BLOOD));
    public static final RegistryObject<Item> WIZARD_CHEST_ELDRITCH = ITEMS.register("wizard_chest_eldritch",
            () -> new br.com.murilo.liberthia.magic.armor.WizardChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ELDRITCH));
    public static final RegistryObject<Item> WIZARD_CHEST_HOLY = ITEMS.register("wizard_chest_holy",
            () -> new br.com.murilo.liberthia.magic.armor.WizardChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY));
    public static final RegistryObject<Item> WIZARD_CHEST_NATURE = ITEMS.register("wizard_chest_nature",
            () -> new br.com.murilo.liberthia.magic.armor.WizardChestplateItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.NATURE));

    // r103: Magic Crops
    public static final RegistryObject<Item> MAGE_BLOOM_SEED = ITEMS.register("mage_bloom_seed",
            () -> new net.minecraft.world.item.ItemNameBlockItem(
                    br.com.murilo.liberthia.registry.ModBlocks.MAGE_BLOOM_CROP.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> MAGE_BLOOM_FIBER = ITEMS.register("mage_bloom_fiber",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SOURCE_BERRY = ITEMS.register("source_berry",
            () -> new br.com.murilo.liberthia.magic.crop.SourceBerryItem(
                    br.com.murilo.liberthia.registry.ModBlocks.SOURCE_BERRY_BUSH.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    // r104: Spell Bow
    public static final RegistryObject<Item> SPELL_BOW = ITEMS.register("spell_bow",
            () -> new br.com.murilo.liberthia.magic.weapon.SpellBowItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r108: 3 More Iconic Spells
    public static final RegistryObject<Item> ICE_TOMB_SPELL = ITEMS.register("ice_tomb_spell",
            () -> new br.com.murilo.liberthia.magic.spells.IceTombSpellItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> ASCENSION_SPELL = ITEMS.register("ascension_spell",
            () -> new br.com.murilo.liberthia.magic.spells.AscensionSpellItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final RegistryObject<Item> HEAT_SURGE_SPELL = ITEMS.register("heat_surge_spell",
            () -> new br.com.murilo.liberthia.magic.spells.HeatSurgeSpellItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));

    // r107: 21 Wizard Armor Pieces (7 schools × 3 slots)
    public static final RegistryObject<Item> WIZARD_HELM_FIRE = ITEMS.register("wizard_helm_fire",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.FIRE,
                    net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> WIZARD_LEGS_FIRE = ITEMS.register("wizard_legs_fire",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.FIRE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WIZARD_BOOTS_FIRE = ITEMS.register("wizard_boots_fire",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.FIRE,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> WIZARD_HELM_ICE = ITEMS.register("wizard_helm_ice",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ICE,
                    net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> WIZARD_LEGS_ICE = ITEMS.register("wizard_legs_ice",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ICE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WIZARD_BOOTS_ICE = ITEMS.register("wizard_boots_ice",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ICE,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> WIZARD_HELM_LIGHTNING = ITEMS.register("wizard_helm_lightning",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.LIGHTNING,
                    net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> WIZARD_LEGS_LIGHTNING = ITEMS.register("wizard_legs_lightning",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.LIGHTNING,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WIZARD_BOOTS_LIGHTNING = ITEMS.register("wizard_boots_lightning",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.LIGHTNING,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> WIZARD_HELM_BLOOD = ITEMS.register("wizard_helm_blood",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.BLOOD,
                    net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> WIZARD_LEGS_BLOOD = ITEMS.register("wizard_legs_blood",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.BLOOD,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WIZARD_BOOTS_BLOOD = ITEMS.register("wizard_boots_blood",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.BLOOD,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> WIZARD_HELM_ELDRITCH = ITEMS.register("wizard_helm_eldritch",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ELDRITCH,
                    net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> WIZARD_LEGS_ELDRITCH = ITEMS.register("wizard_legs_eldritch",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ELDRITCH,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WIZARD_BOOTS_ELDRITCH = ITEMS.register("wizard_boots_eldritch",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ELDRITCH,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> WIZARD_HELM_HOLY = ITEMS.register("wizard_helm_holy",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY,
                    net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> WIZARD_LEGS_HOLY = ITEMS.register("wizard_legs_holy",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WIZARD_BOOTS_HOLY = ITEMS.register("wizard_boots_holy",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final RegistryObject<Item> WIZARD_HELM_NATURE = ITEMS.register("wizard_helm_nature",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.NATURE,
                    net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final RegistryObject<Item> WIZARD_LEGS_NATURE = ITEMS.register("wizard_legs_nature",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.NATURE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final RegistryObject<Item> WIZARD_BOOTS_NATURE = ITEMS.register("wizard_boots_nature",
            () -> new br.com.murilo.liberthia.magic.armor.WizardArmorPieceItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC),
                    br.com.murilo.liberthia.magic.school.SpellSchool.NATURE,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS));

    // ════════════════════════════════════════════════════════════════════════
    // r110: BlockItems pra todos os blocks novos (r82-r109) ficarem placeable
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Item> VOLCANIC_SOURCELINK_ITEM = ITEMS.register("volcanic_sourcelink",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.VOLCANIC_SOURCELINK.get(), new Item.Properties()));
    public static final RegistryObject<Item> MYCELIAL_SOURCELINK_ITEM = ITEMS.register("mycelial_sourcelink",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MYCELIAL_SOURCELINK.get(), new Item.Properties()));
    public static final RegistryObject<Item> VITALIC_SOURCELINK_ITEM = ITEMS.register("vitalic_sourcelink",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.VITALIC_SOURCELINK.get(), new Item.Properties()));
    public static final RegistryObject<Item> ALCHEMICAL_SOURCELINK_ITEM = ITEMS.register("alchemical_sourcelink",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ALCHEMICAL_SOURCELINK.get(), new Item.Properties()));
    public static final RegistryObject<Item> RITUAL_BRAZIER_ITEM = ITEMS.register("ritual_brazier",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RITUAL_BRAZIER.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPELL_PRISM_ITEM = ITEMS.register("spell_prism",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPELL_PRISM.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPELL_TURRET_ITEM = ITEMS.register("spell_turret",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPELL_TURRET.get(), new Item.Properties()));
    public static final RegistryObject<Item> SPELL_SENSOR_ITEM = ITEMS.register("spell_sensor",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPELL_SENSOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> MIRROR_WEAVE_ITEM = ITEMS.register("mirror_weave",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MIRROR_WEAVE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SKY_WEAVE_ITEM = ITEMS.register("sky_weave",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SKY_WEAVE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GHOST_WEAVE_ITEM = ITEMS.register("ghost_weave",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GHOST_WEAVE.get(), new Item.Properties()));
    public static final RegistryObject<Item> FALSE_WEAVE_ITEM = ITEMS.register("false_weave",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.FALSE_WEAVE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FIRE_FIRE_ITEM = ITEMS.register("magic_fire_fire",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_FIRE_FIRE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FIRE_ICE_ITEM = ITEMS.register("magic_fire_ice",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_FIRE_ICE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FIRE_LIGHTNING_ITEM = ITEMS.register("magic_fire_lightning",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_FIRE_LIGHTNING.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FIRE_BLOOD_ITEM = ITEMS.register("magic_fire_blood",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_FIRE_BLOOD.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FIRE_ELDRITCH_ITEM = ITEMS.register("magic_fire_eldritch",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_FIRE_ELDRITCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FIRE_HOLY_ITEM = ITEMS.register("magic_fire_holy",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_FIRE_HOLY.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FIRE_NATURE_ITEM = ITEMS.register("magic_fire_nature",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGIC_FIRE_NATURE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGELIGHT_TORCH_ITEM = ITEMS.register("magelight_torch",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGELIGHT_TORCH.get(), new Item.Properties()));
    public static final RegistryObject<Item> MOB_JAR_ITEM = ITEMS.register("mob_jar",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MOB_JAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> POTION_JAR_ITEM = ITEMS.register("potion_jar",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.POTION_JAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCRYER_OCULUS_ITEM = ITEMS.register("scryer_oculus",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SCRYER_OCULUS.get(), new Item.Properties()));
    public static final RegistryObject<Item> REPOSITORY_ITEM = ITEMS.register("repository",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.REPOSITORY.get(), new Item.Properties()));
    public static final RegistryObject<Item> FIRE_WALL_ITEM = ITEMS.register("fire_wall",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.FIRE_WALL.get(), new Item.Properties()));
    public static final RegistryObject<Item> ICE_WALL_ITEM = ITEMS.register("ice_wall",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ICE_WALL.get(), new Item.Properties()));
    public static final RegistryObject<Item> LIGHTNING_WALL_ITEM = ITEMS.register("lightning_wall",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.LIGHTNING_WALL.get(), new Item.Properties()));
    public static final RegistryObject<Item> HOLY_WALL_ITEM = ITEMS.register("holy_wall",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.HOLY_WALL.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHIRLWIND_ITEM = ITEMS.register("whirlwind",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.WHIRLWIND.get(), new Item.Properties()));
    public static final RegistryObject<Item> AUTO_MINER_ITEM = ITEMS.register("auto_miner",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AUTO_MINER.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGE_CAULDRON_ITEM = ITEMS.register("mage_cauldron",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MAGE_CAULDRON.get(), new Item.Properties()));
    public static final RegistryObject<Item> INSCRIPTION_TABLE_ITEM = ITEMS.register("inscription_table",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.INSCRIPTION_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCROLL_FORGE_ITEM = ITEMS.register("scroll_forge",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SCROLL_FORGE.get(), new Item.Properties()));
    // r80 ores BlockItems
    public static final RegistryObject<Item> SOURCESTONE_ORE_ITEM_NEW = ITEMS.register("sourcestone_ore_v2",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SOURCESTONE_ORE.get(), new Item.Properties()));

    // r81: Cosmic Horror entity spawn eggs (registered as ForgeSpawnEggItem)
    public static final RegistryObject<Item> EMPTY_MAN_EGG = ITEMS.register("empty_man_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.EMPTY_MAN, 0x222222, 0xAAAAAA, new Item.Properties()));
    public static final RegistryObject<Item> OBSERVER_EGG = ITEMS.register("observer_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.OBSERVER, 0x333366, 0x9999CC, new Item.Properties()));
    public static final RegistryObject<Item> ABSENCE_EGG = ITEMS.register("absence_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.ABSENCE, 0x000000, 0x222222, new Item.Properties()));
    public static final RegistryObject<Item> REMEMBERED_EGG = ITEMS.register("remembered_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.REMEMBERED, 0x550055, 0xAA66AA, new Item.Properties()));

    // r87 Wizard spawn eggs
    public static final RegistryObject<Item> PYROMANCER_EGG = ITEMS.register("pyromancer_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.PYROMANCER, 0xFF6633, 0xFFAA66, new Item.Properties()));
    public static final RegistryObject<Item> CRYOMANCER_EGG = ITEMS.register("cryomancer_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.CRYOMANCER, 0x66CCFF, 0xAADDFF, new Item.Properties()));
    public static final RegistryObject<Item> ELECTROMANCER_EGG = ITEMS.register("electromancer_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.ELECTROMANCER, 0xFFFF44, 0xFFFFAA, new Item.Properties()));
    public static final RegistryObject<Item> NECROMANCER_EGG = ITEMS.register("necromancer_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.NECROMANCER, 0x990033, 0xCC4477, new Item.Properties()));
    public static final RegistryObject<Item> ELDRITCH_CULTIST_EGG = ITEMS.register("eldritch_cultist_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.ELDRITCH_CULTIST, 0x6633CC, 0x9966DD, new Item.Properties()));
    public static final RegistryObject<Item> APOTHECARIST_EGG = ITEMS.register("apothecarist_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.APOTHECARIST, 0x66CC66, 0xAADDAA, new Item.Properties()));
    public static final RegistryObject<Item> KEEPER_EGG = ITEMS.register("keeper_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.KEEPER, 0xFFEEAA, 0xFFFFEE, new Item.Properties()));
    public static final RegistryObject<Item> ARCHEVOKER_EGG = ITEMS.register("archevoker_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.ARCHEVOKER, 0x6633CC, 0xCC66FF, new Item.Properties()));

    // r95 Familiar spawn eggs
    public static final RegistryObject<Item> WISP_PICKER_EGG = ITEMS.register("wisp_picker_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.WISP_PICKER, 0xFFFFAA, 0xFFFFEE, new Item.Properties()));
    public static final RegistryObject<Item> GROVE_SPRITE_EGG = ITEMS.register("grove_sprite_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.GROVE_SPRITE, 0x33AA33, 0x66CC66, new Item.Properties()));
    public static final RegistryObject<Item> SOUL_REAPER_EGG = ITEMS.register("soul_reaper_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.SOUL_REAPER, 0x440044, 0x880088, new Item.Properties()));
    public static final RegistryObject<Item> WHELP_EGG = ITEMS.register("whelp_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.WHELP, 0xFF6633, 0xFFAA66, new Item.Properties()));
    public static final RegistryObject<Item> CARBUNCLE_EGG = ITEMS.register("carbuncle_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.CARBUNCLE, 0xCCAA66, 0xFFDD99, new Item.Properties()));
    public static final RegistryObject<Item> AMETHYST_GOLEM_EGG = ITEMS.register("amethyst_golem_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.AMETHYST_GOLEM, 0x9966CC, 0xCC99FF, new Item.Properties()));

    // r98 Boss spawn eggs
    public static final RegistryObject<Item> ABYSSAL_LICH_EGG = ITEMS.register("abyssal_lich_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.ABYSSAL_LICH, 0x330044, 0x880088, new Item.Properties()));
    public static final RegistryObject<Item> LICH_STALKER_EGG = ITEMS.register("lich_stalker_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.LICH_STALKER, 0x222244, 0x444466, new Item.Properties()));
    public static final RegistryObject<Item> LICH_HUNTER_EGG = ITEMS.register("lich_hunter_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.LICH_HUNTER, 0x442266, 0x664488, new Item.Properties()));

    // ════════════════════════════════════════════════════════════════════════
    // r111-r112: Universal Spell Scrolls — 50+ feitiços portados do Iron's Spells
    // ════════════════════════════════════════════════════════════════════════

    private static RegistryObject<Item> spell(String spellId, Rarity rarity) {
        return ITEMS.register("spell_" + spellId, () ->
                new br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem(
                        new Item.Properties().rarity(rarity), spellId));
    }

    // ─── FIRE (8) ───
    public static final RegistryObject<Item> SPELL_FIREBALL          = spell("fireball",         Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_BURNING_DASH      = spell("burning_dash",     Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_INFERNO           = spell("inferno",          Rarity.RARE);
    public static final RegistryObject<Item> SPELL_MAGMA_BOMB        = spell("magma_bomb",       Rarity.RARE);
    public static final RegistryObject<Item> SPELL_SUN_BEAM          = spell("sun_beam",         Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_PHOENIX_REBORN    = spell("phoenix_reborn",   Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_CAUTERIZE         = spell("cauterize",        Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_HEAT_WAVE         = spell("heat_wave",        Rarity.RARE);

    // ─── ICE (8) ───
    public static final RegistryObject<Item> SPELL_FROSTBOLT         = spell("frostbolt",        Rarity.COMMON);
    public static final RegistryObject<Item> SPELL_ICE_SPIKE         = spell("ice_spike",        Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_FROST_NOVA        = spell("frost_nova",       Rarity.RARE);
    public static final RegistryObject<Item> SPELL_GLACIAL_STORM     = spell("glacial_storm",    Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_FROST_STEP        = spell("frost_step",       Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_FROZEN_GROUND     = spell("frozen_ground",    Rarity.RARE);
    public static final RegistryObject<Item> SPELL_RAY_OF_FROST      = spell("ray_of_frost",     Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_ICE_LANCE         = spell("ice_lance",        Rarity.RARE);

    // ─── LIGHTNING (8) ───
    public static final RegistryObject<Item> SPELL_LIGHTNING_BOLT    = spell("lightning_bolt",   Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_SPARK_BURST       = spell("spark_burst",      Rarity.COMMON);
    public static final RegistryObject<Item> SPELL_CHAIN_LIGHTNING   = spell("chain_lightning",  Rarity.RARE);
    public static final RegistryObject<Item> SPELL_SHOCK             = spell("shock",            Rarity.COMMON);
    public static final RegistryObject<Item> SPELL_THUNDER_STEP      = spell("thunder_step",     Rarity.RARE);
    public static final RegistryObject<Item> SPELL_STORM_CLOUD       = spell("storm_cloud",      Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_STATIC_FIELD      = spell("static_field",     Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_LIGHTNING_LANCE   = spell("lightning_lance_spell", Rarity.RARE);

    // ─── BLOOD (8) ───
    public static final RegistryObject<Item> SPELL_BLOOD_STEP        = spell("blood_step",       Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_LIFEDRAIN         = spell("lifedrain",        Rarity.RARE);
    public static final RegistryObject<Item> SPELL_HEARTSTOP         = spell("heartstop",        Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_BLOOD_SPEAR       = spell("blood_spear",      Rarity.RARE);
    public static final RegistryObject<Item> SPELL_SANGUINE_BIND     = spell("sanguine_bind",    Rarity.RARE);
    public static final RegistryObject<Item> SPELL_CRIMSON_MIST      = spell("crimson_mist",     Rarity.RARE);
    public static final RegistryObject<Item> SPELL_VAMPIRIC_TOUCH    = spell("vampiric_touch",   Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_BLOOD_PACT        = spell("blood_pact",       Rarity.EPIC);

    // ─── ELDRITCH (6) ───
    public static final RegistryObject<Item> SPELL_VOID_TENTACLE     = spell("void_tentacle",    Rarity.RARE);
    public static final RegistryObject<Item> SPELL_MIND_SPIKE        = spell("mind_spike",       Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_ELDRITCH_BLAST    = spell("eldritch_blast",   Rarity.RARE);
    public static final RegistryObject<Item> SPELL_SOUL_TEAR         = spell("soul_tear",        Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_MADNESS_WAVE      = spell("madness_wave",     Rarity.RARE);
    public static final RegistryObject<Item> SPELL_COSMIC_VOID       = spell("cosmic_void_spell",Rarity.EPIC);

    // ─── HOLY (8) ───
    public static final RegistryObject<Item> SPELL_GREATER_HEAL      = spell("greater_heal",     Rarity.RARE);
    public static final RegistryObject<Item> SPELL_SMITE             = spell("smite",            Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_DIVINE_LIGHT      = spell("divine_light",     Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_SUN_STRIKE        = spell("sun_strike",       Rarity.RARE);
    public static final RegistryObject<Item> SPELL_HOLY_LANCE        = spell("holy_lance_spell", Rarity.RARE);
    public static final RegistryObject<Item> SPELL_HEALING_AURA      = spell("healing_aura",     Rarity.RARE);
    public static final RegistryObject<Item> SPELL_SACRED_GROUND     = spell("sacred_ground",    Rarity.RARE);
    public static final RegistryObject<Item> SPELL_JUDGMENT          = spell("judgment",         Rarity.EPIC);

    // ─── NATURE (6) ───
    public static final RegistryObject<Item> SPELL_VINE_TANGLE       = spell("vine_tangle",      Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_EARTH_WALL        = spell("earth_wall",       Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_STONE_SHARD       = spell("stone_shard",      Rarity.COMMON);
    public static final RegistryObject<Item> SPELL_WISPS_HEAL        = spell("wisps_heal",       Rarity.RARE);
    public static final RegistryObject<Item> SPELL_ROOTS             = spell("roots",            Rarity.COMMON);
    public static final RegistryObject<Item> SPELL_BRAMBLE_STORM     = spell("bramble_storm",    Rarity.EPIC);

    // ─── EVOCATION (4) ───
    public static final RegistryObject<Item> SPELL_MAGIC_MISSILE     = spell("magic_missile",    Rarity.COMMON);
    public static final RegistryObject<Item> SPELL_BONE_SPEAR        = spell("bone_spear",       Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_MAGIC_SHIELD      = spell("magic_shield",     Rarity.RARE);
    public static final RegistryObject<Item> SPELL_SUMMON_VEX        = spell("summon_vex",       Rarity.RARE);

    // ─── r119 MOBILITY (5) — flight + dashes ───
    public static final RegistryObject<Item> SPELL_WINGS_OF_SOURCE   = spell("wings_of_source",  Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_PHASE_DASH        = spell("phase_dash",       Rarity.RARE);
    public static final RegistryObject<Item> SPELL_WIND_STEP         = spell("wind_step",        Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_LEVITATE_SELF     = spell("levitate_self",    Rarity.UNCOMMON);
    public static final RegistryObject<Item> SPELL_BLINK             = spell("blink",            Rarity.UNCOMMON);

    // ─── r119 APOLÃO TIER (4) — supreme >1000 damage spells ───
    public static final RegistryObject<Item> SPELL_SOLAR_APOCALYPSE      = spell("solar_apocalypse",        Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_ELDRITCH_METEOR       = spell("eldritch_meteor",         Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_DIVINE_JUDGMENT_APOLAO = spell("divine_judgment_apolao", Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_APOCALYPSE            = spell("apocalypse",              Rarity.EPIC);

    // ─── r120 VAZIO — feitiço supremo das ametistas ───
    public static final RegistryObject<Item> SPELL_VOID         = spell("void",         Rarity.EPIC);
    public static final RegistryObject<Item> SPELL_VOID_LASER   = spell("void_laser",   Rarity.EPIC);

    /** Reagente especial usado pra craftar Void spell — amethyst destilada. */
    public static final RegistryObject<Item> VOID_REAGENT = ITEMS.register("void_reagent",
            () -> new br.com.murilo.liberthia.magic.glyph.SpiritReagentItem(
                    new Item.Properties().rarity(Rarity.EPIC), "Amethyst Shrines / Ritual"));

    // ════════════════════════════════════════════════════════════════════════
    // r116: Spirit Reagents — drops do Spirit World pra craft glyphs/spells
    // ════════════════════════════════════════════════════════════════════════
    private static RegistryObject<Item> reagent(String id, String dropSource, Rarity rarity) {
        return ITEMS.register(id, () ->
                new br.com.murilo.liberthia.magic.glyph.SpiritReagentItem(
                        new Item.Properties().rarity(rarity), dropSource));
    }

    public static final RegistryObject<Item> WISP_ESSENCE =
            reagent("wisp_essence", "Pale Spirits", Rarity.UNCOMMON);
    public static final RegistryObject<Item> ASTRAL_DUST =
            reagent("astral_dust", "Sourcestone Ore", Rarity.UNCOMMON);
    public static final RegistryObject<Item> WHISPERWOOD_RESIN =
            reagent("whisperwood_resin", "Whisperwood Trees", Rarity.UNCOMMON);
    public static final RegistryObject<Item> MEMORY_SHARD =
            reagent("memory_shard", "Observer / Spirit Ores", Rarity.RARE);
    public static final RegistryObject<Item> ECTOPLASM_STRAND =
            reagent("ectoplasm_strand", "Absence / Cosmic Horror", Rarity.RARE);
    public static final RegistryObject<Item> PHANTOM_INK =
            reagent("phantom_ink", "Empty Man / Remembered", Rarity.RARE);
    public static final RegistryObject<Item> VEIL_FRAGMENT =
            reagent("veil_fragment", "Loom Mobs / Portal Events", Rarity.EPIC);

    // ════════════════════════════════════════════════════════════════════════
    // r139: NOVOS Spirit World drops + ritual ingredients
    // ════════════════════════════════════════════════════════════════════════
    /** Drop de Shade — usado em Decay rituals. */
    public static final RegistryObject<Item> SHADOW_ESSENCE =
            reagent("shadow_essence", "Shade — Spirit World", Rarity.UNCOMMON);
    /** Drop de Loom Watcher — usado em Reflection rituals. */
    public static final RegistryObject<Item> LOOM_EYE =
            reagent("loom_eye", "Loom Watcher — Spirit World", Rarity.RARE);
    /** Drop de Whisper Beast — usado em Silence rituals. */
    public static final RegistryObject<Item> WHISPER_VIAL =
            reagent("whisper_vial", "Whisper Beast — Spirit World", Rarity.RARE);
    /** Drop de Dark Consciousness — usado em Tendril rituals. */
    public static final RegistryObject<Item> DARK_TENDRIL =
            reagent("dark_tendril", "Dark Consciousness — Spirit World", Rarity.RARE);
    /** Drop de Spirit Crystal Ore — gem usada em vários rituals e crafts. */
    public static final RegistryObject<Item> SPIRIT_CRYSTAL =
            reagent("spirit_crystal", "Spirit Crystal Ore — Spirit World", Rarity.RARE);
    /** Drop de Soul Iron Ore — material upgrade. */
    public static final RegistryObject<Item> SOUL_IRON_RAW =
            reagent("soul_iron_raw", "Soul Iron Ore — Spirit World", Rarity.UNCOMMON);
    /** Smelted from Soul Iron Raw — usado em wands e weapons spirit-tier. */
    public static final RegistryObject<Item> SOUL_IRON_INGOT =
            reagent("soul_iron_ingot", "Furnace — Soul Iron Raw", Rarity.UNCOMMON);
    /** Drop de Whisper Petal Bush — usado em Silence/Stealth rituals. */
    public static final RegistryObject<Item> WHISPER_PETAL =
            reagent("whisper_petal", "Whisper Petal Bush — Spirit World", Rarity.UNCOMMON);
    /** Drop de Ghost Mushroom — usado em Light/Heal rituals. */
    public static final RegistryObject<Item> GHOST_MUSHROOM =
            reagent("ghost_mushroom", "Ghost Mushroom Block — Spirit World", Rarity.UNCOMMON);
    /** Sacred Ash — drop de queima ritualística + ingrediente comum. */
    public static final RegistryObject<Item> SACRED_ASH =
            reagent("sacred_ash", "Burn a Witch / Soul Fire Ritual", Rarity.COMMON);

    // ════════════════════════════════════════════════════════════════════════
    // r117: Spirit Robes — armadura mágica (4 peças com Source bonuses)
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Item> SPIRIT_ROBES_HELM = ITEMS.register("spirit_robes_helm",
            () -> new br.com.murilo.liberthia.magic.armor.SpiritRobesItem(
                    net.minecraft.world.item.ArmorItem.Type.HELMET,
                    new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SPIRIT_ROBES_CHEST = ITEMS.register("spirit_robes_chest",
            () -> new br.com.murilo.liberthia.magic.armor.SpiritRobesItem(
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SPIRIT_ROBES_LEGS = ITEMS.register("spirit_robes_legs",
            () -> new br.com.murilo.liberthia.magic.armor.SpiritRobesItem(
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                    new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SPIRIT_ROBES_BOOTS = ITEMS.register("spirit_robes_boots",
            () -> new br.com.murilo.liberthia.magic.armor.SpiritRobesItem(
                    net.minecraft.world.item.ArmorItem.Type.BOOTS,
                    new Item.Properties().rarity(Rarity.RARE)));

    // r118: Glyph Inscriber BlockItem
    public static final RegistryObject<Item> GLYPH_INSCRIBER_ITEM = ITEMS.register("glyph_inscriber",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.GLYPH_INSCRIBER.get(),
                    new Item.Properties().rarity(Rarity.RARE)));

    // r119: Spell Weaver BlockItem
    public static final RegistryObject<Item> SPELL_WEAVER_ITEM = ITEMS.register("spell_weaver",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPELL_WEAVER.get(),
                    new Item.Properties().rarity(Rarity.EPIC)));

    // ════════════════════════════════════════════════════════════════════════
    // r119: Modifier Glyphs — para usar no SpellWeaver
    // ════════════════════════════════════════════════════════════════════════
    private static RegistryObject<Item> modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier mod, Rarity rarity) {
        return ITEMS.register("modifier_" + mod.id, () ->
                new br.com.murilo.liberthia.magic.spell.composition.SpellModifierItem(
                        new Item.Properties().rarity(rarity), mod));
    }

    public static final RegistryObject<Item> MOD_AMPLIFY    = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.AMPLIFY,    Rarity.UNCOMMON);
    public static final RegistryObject<Item> MOD_AOE        = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.AOE,        Rarity.RARE);
    public static final RegistryObject<Item> MOD_PIERCE     = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.PIERCE,     Rarity.UNCOMMON);
    public static final RegistryObject<Item> MOD_MULTISHOT  = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.MULTISHOT,  Rarity.RARE);
    public static final RegistryObject<Item> MOD_CHAIN      = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.CHAIN,      Rarity.RARE);
    public static final RegistryObject<Item> MOD_IGNITE     = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.IGNITE,     Rarity.UNCOMMON);
    public static final RegistryObject<Item> MOD_FREEZE     = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.FREEZE,     Rarity.UNCOMMON);
    public static final RegistryObject<Item> MOD_KNOCKBACK  = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.KNOCKBACK,  Rarity.COMMON);
    public static final RegistryObject<Item> MOD_LIFESTEAL  = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.LIFESTEAL,  Rarity.RARE);
    public static final RegistryObject<Item> MOD_RANGE      = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.RANGE,      Rarity.COMMON);
    public static final RegistryObject<Item> MOD_SUSTAIN    = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.SUSTAIN,    Rarity.UNCOMMON);
    public static final RegistryObject<Item> MOD_PENETRATE  = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.PENETRATE,  Rarity.RARE);
    public static final RegistryObject<Item> MOD_APOLAO     = modifier(br.com.murilo.liberthia.magic.spell.composition.SpellModifier.APOLAO,     Rarity.EPIC);

    // r119: Grimoire item
    public static final RegistryObject<Item> GRIMOIRE_BOOK = ITEMS.register("grimoire_book",
            () -> new br.com.murilo.liberthia.magic.grimoire.GrimoireBookItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // r135: AN-inspired features faltantes
    // r140 fix: renomeado pra mob_capture_jar — conflitava com Storage Jar (mob_jar) do r92
    public static final RegistryObject<Item> MOB_JAR = ITEMS.register("mob_capture_jar",
            () -> new br.com.murilo.liberthia.magic.mobjar.MobJarItem(
                    new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SPELL_MIRROR = ITEMS.register("spell_mirror",
            () -> new br.com.murilo.liberthia.magic.item.SpellMirrorItem(
                    new Item.Properties().rarity(Rarity.RARE)));

    // r135: Agronomic Sourcelink (5° sourcelink) + Drygmy egg
    public static final RegistryObject<Item> AGRONOMIC_SOURCELINK_ITEM = ITEMS.register("agronomic_sourcelink",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AGRONOMIC_SOURCELINK.get(),
                    new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DRYGMY_EGG = ITEMS.register("drygmy_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    ModEntities.DRYGMY, 0x88CC44, 0xCCEE88, new Item.Properties()));

    // r117: Conduit + Transmuter block items
    public static final RegistryObject<Item> SPIRIT_CONDUIT_ITEM = ITEMS.register("spirit_conduit",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SPIRIT_CONDUIT.get(),
                    new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SOURCE_TRANSMUTER_ITEM = ITEMS.register("source_transmuter",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SOURCE_TRANSMUTER.get(),
                    new Item.Properties().rarity(Rarity.RARE)));

    // r148: Spell Factory — item dinâmico que aceita qualquer spell criada via JSON
    public static final RegistryObject<Item> FACTORY_SPELL_SCROLL = ITEMS.register("factory_spell_scroll",
            () -> new br.com.murilo.liberthia.magic.factory.DynamicSpellItem(
                    new Item.Properties()));

    // r154: Glyph de Cooldown Reduction — -10% CD, +15% custo por glyph stacked
    public static final RegistryObject<Item> CDR_GLYPH = ITEMS.register("glyph_cooldown_reduction",
            () -> new br.com.murilo.liberthia.magic.modifier.CooldownReductionGlyphItem(
                    new Item.Properties()));

    // r159: Componentes do Arcane Workbench
    public static final RegistryObject<Item> MAGIC_TABLET = ITEMS.register("magic_tablet",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.MagicTablet(
                    new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_ORB = ITEMS.register("arcane_orb",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.ArcaneOrb(
                    new Item.Properties()));
    public static final RegistryObject<Item> SCHOOL_RUNE_FIRE = ITEMS.register("school_rune_fire",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.SchoolRune(
                    new Item.Properties(), "fire"));
    public static final RegistryObject<Item> SCHOOL_RUNE_ICE = ITEMS.register("school_rune_ice",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.SchoolRune(
                    new Item.Properties(), "ice"));
    public static final RegistryObject<Item> SCHOOL_RUNE_LIGHTNING = ITEMS.register("school_rune_lightning",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.SchoolRune(
                    new Item.Properties(), "lightning"));
    public static final RegistryObject<Item> SCHOOL_RUNE_BLOOD = ITEMS.register("school_rune_blood",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.SchoolRune(
                    new Item.Properties(), "blood"));
    public static final RegistryObject<Item> SCHOOL_RUNE_ELDRITCH = ITEMS.register("school_rune_eldritch",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.SchoolRune(
                    new Item.Properties(), "eldritch"));
    public static final RegistryObject<Item> SCHOOL_RUNE_HOLY = ITEMS.register("school_rune_holy",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.SchoolRune(
                    new Item.Properties(), "holy"));
    public static final RegistryObject<Item> SCHOOL_RUNE_NATURE = ITEMS.register("school_rune_nature",
            () -> new br.com.murilo.liberthia.magic.workbench.ArcaneComponentItems.SchoolRune(
                    new Item.Properties(), "nature"));

    // r150: Spawn eggs pros 8 Wooden Horror variants
    public static final RegistryObject<Item> WOODEN_CHARCOAL_EGG = ITEMS.register("wooden_charcoal_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_CHARCOAL,
                    0x1a1a1a, 0x2a2a2a, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_PALE_OAK_EGG = ITEMS.register("wooden_pale_oak_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_PALE_OAK,
                    0xe8d8b8, 0xc8b888, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_ROTTED_BIRCH_EGG = ITEMS.register("wooden_rotted_birch_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_ROTTED_BIRCH,
                    0x888878, 0x555548, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_BLEEDING_MAPLE_EGG = ITEMS.register("wooden_bleeding_maple_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_BLEEDING_MAPLE,
                    0x8a2828, 0x4a1010, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_MOSSY_EGG = ITEMS.register("wooden_mossy_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_MOSSY,
                    0x4a7a3a, 0x2a4a20, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_FROZEN_PINE_EGG = ITEMS.register("wooden_frozen_pine_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_FROZEN_PINE,
                    0x88c8e0, 0x4a8aa8, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_BURNING_ACACIA_EGG = ITEMS.register("wooden_burning_acacia_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_BURNING_ACACIA,
                    0xd86820, 0xa84810, new Item.Properties()));
    public static final RegistryObject<Item> WOODEN_CURSED_MAHOGANY_EGG = ITEMS.register("wooden_cursed_mahogany_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WOODEN_CURSED_MAHOGANY,
                    0x4a2868, 0x2a1048, new Item.Properties()));

    // ════════════════════════════════════════════════════════════════
    // r164: Magic Accessories — 9 items (3 slot types × 3 effects)
    // ════════════════════════════════════════════════════════════════
    // Mana Regen (0.5/s base, anel = mais barato, cinto = mais forte)
    public static final RegistryObject<Item> RING_MANA_FLOW = ITEMS.register("ring_mana_flow",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties(),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.RING,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.MANA_REGEN,
                    0.5F));
    public static final RegistryObject<Item> GLOVE_MANA_CHANNELER = ITEMS.register("glove_mana_channeler",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties(),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.GLOVE,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.MANA_REGEN,
                    1.0F));
    public static final RegistryObject<Item> BELT_MANA_RESERVOIR = ITEMS.register("belt_mana_reservoir",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.BELT,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.MANA_REGEN,
                    2.0F));

    // Sanity Regen
    public static final RegistryObject<Item> RING_LUCIDITY = ITEMS.register("ring_lucidity",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties(),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.RING,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.SANITY_REGEN,
                    0.2F));
    public static final RegistryObject<Item> GLOVE_LUCID_VEIL = ITEMS.register("glove_lucid_veil",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties(),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.GLOVE,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.SANITY_REGEN,
                    0.5F));
    public static final RegistryObject<Item> BELT_SANCTUM_SASH = ITEMS.register("belt_sanctum_sash",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.BELT,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.SANITY_REGEN,
                    1.0F));

    // Spell Power (% damage)
    public static final RegistryObject<Item> RING_ARCANE_POWER = ITEMS.register("ring_arcane_power",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties(),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.RING,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.SPELL_POWER,
                    0.05F));
    public static final RegistryObject<Item> GLOVE_BATTLEMAGE = ITEMS.register("glove_battlemage",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties(),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.GLOVE,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.SPELL_POWER,
                    0.10F));
    public static final RegistryObject<Item> BELT_ARCHMAGE = ITEMS.register("belt_archmage",
            () -> new br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem(
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE),
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.SlotKind.BELT,
                    br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem.Effect.SPELL_POWER,
                    0.20F));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
