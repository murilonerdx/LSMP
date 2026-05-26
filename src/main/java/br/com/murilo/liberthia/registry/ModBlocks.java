package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.block.RitualPedestalBlock;
import br.com.murilo.liberthia.logic.ClearMatterBlock;
import br.com.murilo.liberthia.logic.CorruptedSoilBlock;
import br.com.murilo.liberthia.logic.DarkMatterBlock;
import br.com.murilo.liberthia.logic.DarkMatterFluidBlock;
import br.com.murilo.liberthia.logic.InfectionGrowthBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LiberthiaMod.MODID);

    public static final RegistryObject<Block> DARK_MATTER_BLOCK = BLOCKS.register("dark_matter_block",
            () -> new DarkMatterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 0)
                    .emissiveRendering((s, g, p) -> true)
                    .randomTicks()
                    .sound(SoundType.AMETHYST)));

    public static final RegistryObject<Block> CLEAR_MATTER_BLOCK = BLOCKS.register("clear_matter_block",
            () -> new ClearMatterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(3.0F, 6.0F)
                    .lightLevel(state -> 11)
                    .randomTicks()
                    .sound(SoundType.GLASS)));

    public static final RegistryObject<Block> RITUAL_PEDESTAL =
            registerBlock("ritual_pedestal",
                    () -> new RitualPedestalBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(4.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)
                            .noOcclusion()));

    public static final RegistryObject<Block> BONE_RUNE =
            registerBlock("bone_rune",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(2.0F)
                            .sound(SoundType.BONE_BLOCK)));

    public static final RegistryObject<Block> GOLD_RUNE =
            registerBlock("gold_rune",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.GOLD)
                            .strength(2.0F)
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> DIAMOND_RUNE =
            registerBlock("diamond_rune",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DIAMOND)
                            .strength(2.0F)
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> NETHERITE_RUNE =
            registerBlock("netherite_rune",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(3.0F, 10.0F)
                            .sound(SoundType.NETHERITE_BLOCK)));

    public static final RegistryObject<Block> YELLOW_MATTER_BLOCK = BLOCKS.register("yellow_matter_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(5.0F, 10.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 8)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> CORRUPTED_SOIL = BLOCKS.register("corrupted_soil",
            () -> new CorruptedSoilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6F)
                    .randomTicks()
                    .sound(SoundType.GRAVEL)));

    public static final RegistryObject<Block> DARK_MATTER_ORE = BLOCKS.register("dark_matter_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 2)
                    .sound(SoundType.STONE),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> DEEPSLATE_DARK_MATTER_ORE = BLOCKS.register("deepslate_dark_matter_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 2)
                    .sound(SoundType.DEEPSLATE),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<DarkMatterFluidBlock> DARK_MATTER_FLUID_BLOCK = BLOCKS.register("dark_matter_fluid",
            () -> new DarkMatterFluidBlock(ModFluids.DARK_MATTER,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .noCollission()
                            .strength(100.0F)
                            .lightLevel(state -> 3)
                            .randomTicks()
                            .replaceable()
                            .noLootTable()));

    public static final RegistryObject<LiquidBlock> CLEAR_MATTER_FLUID_BLOCK = BLOCKS.register("clear_matter_fluid",
            () -> new LiquidBlock(ModFluids.CLEAR_MATTER,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.ICE)
                            .noCollission()
                            .strength(100.0F)
                            .lightLevel(state -> 6)
                            .replaceable()
                            .noLootTable()));

    public static final RegistryObject<LiquidBlock> BLOOD_FLUID_BLOCK = BLOCKS.register("blood_fluid",
            () -> new LiquidBlock(ModFluids.BLOOD,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .noCollission()
                            .strength(100.0F)
                            .lightLevel(state -> 3)
                            .replaceable()
                            .noLootTable()));

    public static final RegistryObject<LiquidBlock> YELLOW_MATTER_FLUID_BLOCK = BLOCKS.register("yellow_matter_fluid",
            () -> new LiquidBlock(ModFluids.YELLOW_MATTER,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.GOLD)
                            .noCollission()
                            .strength(100.0F)
                            .lightLevel(state -> 8)
                            .replaceable()
                            .noLootTable()));

    public static final RegistryObject<Block> INFECTION_GROWTH = BLOCKS.register("infection_growth",
            () -> new InfectionGrowthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.2F)
                    .noCollission()
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> WHITE_MATTER_ORE = BLOCKS.register("white_matter_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 5)
                    .sound(SoundType.STONE),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> PURIFICATION_BENCH = BLOCKS.register("purification_bench",
            () -> new br.com.murilo.liberthia.block.PurificationBenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    // REMOVIDO v0.1.13: WHITE_MATTER_BOMB_BLOCK

    public static final RegistryObject<Block> PURITY_BEACON = BLOCKS.register("purity_beacon",
            () -> new br.com.murilo.liberthia.logic.PurityBeaconBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 15)
                    .sound(SoundType.GLASS)));

    public static final RegistryObject<Block> BLOOD_FOUNTAIN = BLOCKS.register("blood_fountain",
            () -> new br.com.murilo.liberthia.logic.BloodFountainBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .sound(SoundType.SLIME_BLOCK)));

    // --- Workbenches ---
    public static final RegistryObject<Block> DARK_MATTER_FORGE = BLOCKS.register("dark_matter_forge",
            () -> new br.com.murilo.liberthia.block.DarkMatterForgeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(br.com.murilo.liberthia.block.DarkMatterForgeBlock.LIT) ? 8 : 0)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> MATTER_INFUSER = BLOCKS.register("matter_infuser",
            () -> new br.com.murilo.liberthia.block.MatterInfuserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(4.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 6)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> RESEARCH_TABLE = BLOCKS.register("research_table",
            () -> new br.com.murilo.liberthia.block.ResearchTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));

    public static final RegistryObject<Block> CONTAINMENT_CHAMBER = BLOCKS.register("containment_chamber",
            () -> new br.com.murilo.liberthia.block.ContainmentChamberBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(5.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 4)
                    .sound(SoundType.GLASS)));

    // --- New Infection Blocks ---
    public static final RegistryObject<Block> CORRUPTED_STONE = BLOCKS.register("corrupted_stone",
            () -> new br.com.murilo.liberthia.logic.CorruptedStoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .randomTicks()
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> INFECTION_VEIN = BLOCKS.register("infection_vein",
            () -> new br.com.murilo.liberthia.logic.InfectionVeinBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F)
                    .lightLevel(state -> 3)
                    .randomTicks()
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> SPORE_BLOOM = BLOCKS.register("spore_bloom",
            () -> new br.com.murilo.liberthia.logic.SporeBloomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.3F)
                    .noCollission()
                    .randomTicks()
                    .sound(SoundType.GRASS)));

    public static final RegistryObject<Block> CORRUPTED_LOG = BLOCKS.register("corrupted_log",
            () -> new br.com.murilo.liberthia.logic.CorruptedLogBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0F)
                    .randomTicks()
                    .sound(SoundType.WOOD)));

    // --- White Matter TNT ---
    public static final RegistryObject<Block> WHITE_MATTER_TNT = BLOCKS.register("white_matter_tnt",
            () -> new br.com.murilo.liberthia.logic.WhiteMatterTNTBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.0F)
                    .lightLevel(state -> 8)
                    .sound(SoundType.GRASS)));

    public static final RegistryObject<Block> MATTER_TRANSMUTER = BLOCKS.register("matter_transmuter",
            () -> new br.com.murilo.liberthia.block.MatterTransmuterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(4.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 5)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> DARK_MATTER_ALCHEMIZER = BLOCKS.register("dark_matter_alchemizer",
            () -> new br.com.murilo.liberthia.block.DarkMatterAlchemizerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> DARK_MATTER_GENERATOR = BLOCKS.register("dark_matter_generator",
            () -> new br.com.murilo.liberthia.block.DarkMatterGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 6)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> ENERGY_CABLE = BLOCKS.register("energy_cable",
            () -> new br.com.murilo.liberthia.block.EnergyCableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.6F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> ITEM_PIPE = BLOCKS.register("item_pipe",
            () -> new br.com.murilo.liberthia.block.ItemPipeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.6F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> ITEM_EXTRACTOR = BLOCKS.register("item_extractor",
            () -> new br.com.murilo.liberthia.block.ItemExtractorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.6F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> ITEM_INSERTER = BLOCKS.register("item_inserter",
            () -> new br.com.murilo.liberthia.block.ItemInserterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.6F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> DIMENSIONAL_CHEST = BLOCKS.register("dimensional_chest",
            () -> new br.com.murilo.liberthia.block.DimensionalChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 7)));

    public static final RegistryObject<Block> MATTER_REFINER = BLOCKS.register("matter_refiner",
            () -> new br.com.murilo.liberthia.block.MatterRefinerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 6)));

    // --- Cadeia de Refinação ---
    public static final RegistryObject<Block> DARK_MATTER_CHEST = BLOCKS.register("dark_matter_chest",
            () -> new br.com.murilo.liberthia.block.DarkMatterChestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.WOOD)));

    public static final RegistryObject<Block> FRAGMENTED_GENERATOR = BLOCKS.register("fragmented_generator",
            () -> new br.com.murilo.liberthia.block.FragmentedGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(4.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 5)));

    public static final RegistryObject<Block> LASER_EMITTER = BLOCKS.register("laser_emitter",
            () -> new br.com.murilo.liberthia.block.LaserEmitterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(5.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 8)));

    public static final RegistryObject<Block> CRYSTALLIZER = BLOCKS.register("crystallizer",
            () -> new br.com.murilo.liberthia.block.CrystallizerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(s -> 10).noOcclusion()));

    public static final RegistryObject<Block> AUTO_FARMER = BLOCKS.register("auto_farmer",
            () -> new br.com.murilo.liberthia.block.AutoFarmerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(4.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 4)));

    public static final RegistryObject<Block> DIMENSIONAL_EXTRACTOR = BLOCKS.register("dimensional_extractor",
            () -> new br.com.murilo.liberthia.block.DimensionalExtractorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(4.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 6)));

    public static final RegistryObject<Block> MATTER_ANALYZER = BLOCKS.register("matter_analyzer",
            () -> new br.com.murilo.liberthia.block.MatterAnalyzerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(3.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 6)));

    public static final RegistryObject<Block> WIRELESS_CHARGER = BLOCKS.register("wireless_charger",
            () -> new br.com.murilo.liberthia.block.WirelessChargerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(3.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(s -> 8)));

    // --- Baterias de Matéria Escura ---
    public static final RegistryObject<Block> BATTERY_BASIC = BLOCKS.register("dm_battery_basic",
            () -> new br.com.murilo.liberthia.block.DarkMatterBatteryBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .strength(4.0F).requiresCorrectToolForDrops()
                            .sound(SoundType.METAL).lightLevel(s -> 4),
                    br.com.murilo.liberthia.block.entity.BasicBatteryBlockEntity::new,
                    () -> ModBlockEntities.BATTERY_BASIC.get()));

    public static final RegistryObject<Block> BATTERY_ADVANCED = BLOCKS.register("dm_battery_advanced",
            () -> new br.com.murilo.liberthia.block.DarkMatterBatteryBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .strength(5.0F).requiresCorrectToolForDrops()
                            .sound(SoundType.METAL).lightLevel(s -> 7),
                    br.com.murilo.liberthia.block.entity.AdvancedBatteryBlockEntity::new,
                    () -> ModBlockEntities.BATTERY_ADVANCED.get()));

    public static final RegistryObject<Block> BATTERY_QUANTUM = BLOCKS.register("dm_battery_quantum",
            () -> new br.com.murilo.liberthia.block.DarkMatterBatteryBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                            .strength(6.0F).requiresCorrectToolForDrops()
                            .sound(SoundType.METAL).lightLevel(s -> 12),
                    br.com.murilo.liberthia.block.entity.QuantumBatteryBlockEntity::new,
                    () -> ModBlockEntities.BATTERY_QUANTUM.get()));

    /**
     * Matter Purifier — bloco com GUI que aceita 1 ingot de matter raw
     * (dark/clear/yellow) e gera o ingot purificado correspondente.
     * Precisa de energia. Usado pra craftar as armaduras de matter.
     */
    public static final RegistryObject<Block> MATTER_PURIFIER = registerBlock("matter_purifier",
            () -> new br.com.murilo.liberthia.block.MatterPurifierBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(5.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(s -> 8)));

    /**
     * v0.1.44: Matter Pill Brewer — bloco alquímico que fabrica pílulas a partir
     * de ingots purificados + glass bottle. Sem energia, processo 3s/lote.
     * Gera 3 pílulas por lote do tipo correspondente (DM/CM/YM).
     */
    public static final RegistryObject<Block> MATTER_PILL_BREWER = registerBlock("matter_pill_brewer",
            () -> new br.com.murilo.liberthia.block.MatterPillBrewerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .strength(3.0F, 5.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(s -> 6)));

    /**
     * Host Plush — pelúcia decorativa 3D que vai no chão. Bloco rotacionável
     * com hitbox compacta, water-loggable. Modelo Blockbench em
     * {@code assets/liberthia/models/item/host_plush.json} (referenciado pelo
     * blockstate).
     */
    public static final RegistryObject<Block> HOST_PLUSH = registerBlock("host_plush",
            () -> new br.com.murilo.liberthia.block.HostPlushBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.3F, 0.5F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, java.util.function.Supplier<T> supplier) {
        RegistryObject<T> block = BLOCKS.register(name, supplier);

        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));

        return block;
    }

    // ==================================================================
    // Matter Extraction Network — Matter Extractor (central),
    // Matter Tank (storage) e Matter Pipe (transport, 3 variantes).
    // Sistema multiblock inspirado em Create: tanks empilháveis com nível
    // visual + canos coloridos por tipo + extractor que drena matter de
    // player próximo. Ver MatterExtractorBlockEntity pra mecânica detalhada.
    // ==================================================================
    public static final RegistryObject<Block> MATTER_EXTRACTOR = registerBlock("matter_extractor",
            () -> new br.com.murilo.liberthia.block.MatterExtractorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(5.0F, 8.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(s -> 8)));

    public static final RegistryObject<Block> MATTER_TANK = registerBlock("matter_tank",
            () -> new br.com.murilo.liberthia.block.MatterTankBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.ICE)
                            .strength(2.0F, 4.0F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .lightLevel(s -> 4)));

    public static final RegistryObject<Block> MATTER_PIPE_DARK = registerBlock("matter_pipe_dark",
            () -> new br.com.murilo.liberthia.block.MatterPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(0.8F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    () -> ModFluids.DARK_MATTER.get()));

    public static final RegistryObject<Block> MATTER_PIPE_CLEAR = registerBlock("matter_pipe_clear",
            () -> new br.com.murilo.liberthia.block.MatterPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.ICE)
                            .strength(0.8F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    () -> ModFluids.CLEAR_MATTER.get()));

    public static final RegistryObject<Block> MATTER_PIPE_YELLOW = registerBlock("matter_pipe_yellow",
            () -> new br.com.murilo.liberthia.block.MatterPipeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.GOLD)
                            .strength(0.8F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion(),
                    () -> ModFluids.YELLOW_MATTER.get()));

    // REMOVIDO v0.1.13: GLITCH_BLOCK

    // --- Wormhole Block ---
    public static final RegistryObject<Block> WORMHOLE_BLOCK = BLOCKS.register("wormhole_block",
            () -> new br.com.murilo.liberthia.logic.WormholeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(5.0F, 12.0F)
                    .noOcclusion()
                    .lightLevel(state -> 10)
                    .emissiveRendering((s, g, p) -> true)
                    .sound(SoundType.AMETHYST)));

    // --- Scarred Terrain (F9) ---
    public static final RegistryObject<Block> SCARRED_EARTH = BLOCKS.register("scarred_earth",
            () -> new br.com.murilo.liberthia.logic.ScarredEarthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(0.6F)
                    .sound(SoundType.GRAVEL)));

    public static final RegistryObject<Block> SCARRED_STONE = BLOCKS.register("scarred_stone",
            () -> new br.com.murilo.liberthia.logic.ScarredStoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    // --- Quarantine Ward (F3) ---
    public static final RegistryObject<Block> QUARANTINE_WARD = BLOCKS.register("quarantine_ward",
            () -> new br.com.murilo.liberthia.logic.QuarantineWardBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(5.0F, 10.0F)
                    .lightLevel(state -> 7)
                    .randomTicks()
                    .sound(SoundType.METAL)));

    // --- Unstable Matter (F8) ---
    public static final RegistryObject<Block> UNSTABLE_MATTER = BLOCKS.register("unstable_matter",
            () -> new br.com.murilo.liberthia.logic.UnstableMatterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5F)
                    .lightLevel(state -> 5 + state.getValue(br.com.murilo.liberthia.logic.UnstableMatterBlock.AGE))
                    .emissiveRendering((s, g, p) -> true)
                    .noOcclusion()
                    .sound(SoundType.GLASS)));

    // --- Infection Heart (F7) ---
    public static final RegistryObject<Block> INFECTION_HEART = BLOCKS.register("infection_heart",
            () -> new br.com.murilo.liberthia.logic.InfectionHeartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(10.0F, 20.0F)
                    .lightLevel(state -> 12)
                    .emissiveRendering((s, g, p) -> true)
                    .sound(SoundType.SCULK_CATALYST)));

    // --- Blood Ritual / Proliferation ---
    public static final RegistryObject<Block> CHALK_SYMBOL = BLOCKS.register("chalk_symbol",
            () -> new br.com.murilo.liberthia.logic.ChalkSymbolBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.1F)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.SAND)));

    public static final RegistryObject<Block> BLOOD_ALTAR = BLOCKS.register("blood_altar",
            () -> new br.com.murilo.liberthia.logic.BloodAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(6.0F, 20.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 7)
                    .randomTicks()
                    .sound(SoundType.SCULK_CATALYST)));

    public static final RegistryObject<Block> LIVING_FLESH = BLOCKS.register("living_flesh",
            () -> new br.com.murilo.liberthia.logic.LivingFleshBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.2F)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> ORDER_SHRINE = BLOCKS.register("order_shrine",
            () -> new br.com.murilo.liberthia.logic.OrderShrineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ)
                    .strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 10)
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> BLOOD_CAULDRON = BLOCKS.register("blood_cauldron",
            () -> new br.com.murilo.liberthia.logic.BloodCauldronBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 4)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Block> HEART_OF_FLESH = BLOCKS.register("heart_of_flesh",
            () -> new br.com.murilo.liberthia.logic.HeartOfFleshBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F)
                    .lightLevel(state -> 7)
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> FLESH_MOTHER = BLOCKS.register("flesh_mother",
            () -> new br.com.murilo.liberthia.logic.FleshMotherBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(4.0F, 10.0F)
                    .lightLevel(state -> 5)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> BLOOD_INFECTION_BLOCK = BLOCKS.register("blood_infection_block",
            () -> new br.com.murilo.liberthia.logic.BloodInfectionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .lightLevel(state -> 4)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> BLOOD_INFESTATION_BLOCK = BLOCKS.register("blood_infestation_block",
            () -> new br.com.murilo.liberthia.logic.BloodInfestationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.5F)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> BLOOD_VOLCANO = BLOCKS.register("blood_volcano",
            () -> new br.com.murilo.liberthia.logic.BloodVolcanoBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(8.0F, 30.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 14)
                    .emissiveRendering((s, g, p) -> true)
                    .randomTicks()
                    .sound(SoundType.NETHERRACK)));

    public static final RegistryObject<Block> ATTACKING_FLESH = BLOCKS.register("attacking_flesh",
            () -> new br.com.murilo.liberthia.logic.AttackingFleshBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F)
                    .lightLevel(state -> 3)
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)));

    // --- T7: attacking blocks with evolving AGE ---
    public static final RegistryObject<Block> WITHERING_EYE = BLOCKS.register("withering_eye",
            () -> new br.com.murilo.liberthia.logic.WitheringEyeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F)
                    .lightLevel(state -> 8 + state.getValue(br.com.murilo.liberthia.logic.WitheringEyeBlock.AGE) * 2)
                    .emissiveRendering((s, g, p) -> true)
                    .randomTicks()
                    .sound(SoundType.NETHERRACK)));

    public static final RegistryObject<Block> VENOM_GEYSER = BLOCKS.register("venom_geyser",
            () -> new br.com.murilo.liberthia.logic.VenomGeyserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.5F)
                    .lightLevel(state -> 4 + state.getValue(br.com.murilo.liberthia.logic.VenomGeyserBlock.AGE))
                    .randomTicks()
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> LIGHTNING_COIL = BLOCKS.register("lightning_coil",
            () -> new br.com.murilo.liberthia.logic.LightningCoilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F)
                    .lightLevel(state -> 10 + state.getValue(br.com.murilo.liberthia.logic.LightningCoilBlock.AGE))
                    .emissiveRendering((s, g, p) -> true)
                    .randomTicks()
                    .sound(SoundType.METAL)));

    // --- T9: more attacking blocks ---
    public static final RegistryObject<Block> THORN_BRIAR = BLOCKS.register("thorn_briar",
            () -> new br.com.murilo.liberthia.logic.ThornBriarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.4F)
                    .noOcclusion()
                    .sound(SoundType.SWEET_BERRY_BUSH)));

    public static final RegistryObject<Block> LIGHTNING_NODE = BLOCKS.register("lightning_node",
            () -> new br.com.murilo.liberthia.logic.LightningNodeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(3.5F)
                    .lightLevel(state -> 8 + state.getValue(br.com.murilo.liberthia.logic.LightningNodeBlock.AGE) * 2)
                    .emissiveRendering((s, g, p) -> true)
                    .randomTicks()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> SCREAMING_SOUL = BLOCKS.register("screaming_soul",
            () -> new br.com.murilo.liberthia.logic.ScreamingSoulBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0F)
                    .lightLevel(state -> 6)
                    .randomTicks()
                    .sound(SoundType.SOUL_SOIL)));

    public static final RegistryObject<Block> MAGNETIC_PYLON = BLOCKS.register("magnetic_pylon",
            () -> new br.com.murilo.liberthia.logic.MagneticPylonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0F)
                    .lightLevel(state -> 7)
                    .randomTicks()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> BLOOD_SPIKE = BLOCKS.register("blood_spike",
            () -> new br.com.murilo.liberthia.logic.BloodSpikeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .lightLevel(state -> 4)
                    .noOcclusion()
                    .sound(SoundType.SLIME_BLOCK)));

    // --- Blood terrain variants (infected dirt/sand/stone) ---
    public static final RegistryObject<Block> BLOOD_DIRT = BLOCKS.register("blood_dirt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)));

    public static final RegistryObject<Block> BLOOD_SAND = BLOCKS.register("blood_sand",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.SAND)));

    public static final RegistryObject<Block> BLOOD_STONE = BLOCKS.register("blood_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.8F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    // --- Blood ores ---
    public static final RegistryObject<Block> BLOOD_COAL_ORE = BLOCKS.register("blood_coal_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(0, 2)));

    public static final RegistryObject<Block> BLOOD_IRON_ORE = BLOCKS.register("blood_iron_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(0, 0)));

    public static final RegistryObject<Block> BLOOD_GOLD_ORE = BLOCKS.register("blood_gold_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(0, 0)));

    public static final RegistryObject<Block> BLOOD_DIAMOND_ORE = BLOCKS.register("blood_diamond_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> BLOOD_REDSTONE_ORE = BLOCKS.register("blood_redstone_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 6)
                    .sound(SoundType.STONE),
                    UniformInt.of(1, 5)));

    public static final RegistryObject<Block> BLOOD_LAPIS_ORE = BLOCKS.register("blood_lapis_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(2, 5)));

    public static final RegistryObject<Block> BLOOD_EMERALD_ORE = BLOCKS.register("blood_emerald_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE),
                    UniformInt.of(3, 7)));

    // --- Occultism-port: Chalk Glyph + Blood Fire/Torch ---
    public static final RegistryObject<Block> CHALK_GLYPH = BLOCKS.register("chalk_glyph",
            () -> new br.com.murilo.liberthia.logic.ChalkGlyphBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0F).noCollission().noOcclusion()
                    .instabreak()
                    .sound(SoundType.WOOL)));

    public static final RegistryObject<Block> BLOOD_FIRE = BLOCKS.register("blood_fire",
            () -> new br.com.murilo.liberthia.logic.BloodFireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0F).noCollission().noOcclusion()
                    .instabreak()
                    .lightLevel(s -> 12)
                    .sound(SoundType.WOOL)));

    public static final RegistryObject<Block> BLOOD_TORCH = BLOCKS.register("blood_torch",
            () -> new br.com.murilo.liberthia.logic.BloodTorchBlock(BlockBehaviour.Properties.of()
                    .noCollission()
                    .instabreak()
                    .lightLevel(s -> 14)
                    .sound(SoundType.WOOD)));

    // --- Blood-themed attacking blocks ---
    public static final RegistryObject<Block> HEMORRHAGE_SPIKE = BLOCKS.register("hemorrhage_spike",
            () -> new br.com.murilo.liberthia.logic.HemorrhageSpikeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F).noOcclusion()
                    .sound(SoundType.GRAVEL)));

    public static final RegistryObject<Block> SANGUINE_SNARE = BLOCKS.register("sanguine_snare",
            () -> new br.com.murilo.liberthia.logic.SanguineSnareBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.4F).noOcclusion()
                    .sound(SoundType.SLIME_BLOCK)));

    public static final RegistryObject<Block> VEIL_OF_MADNESS = BLOCKS.register("veil_of_madness",
            () -> new br.com.murilo.liberthia.logic.VeilOfMadnessBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F)
                    .lightLevel(s -> 6)
                    .randomTicks()
                    .sound(SoundType.SOUL_SOIL)));

    public static final RegistryObject<Block> PHANTOM_PORTAL = BLOCKS.register("phantom_portal",
            () -> new br.com.murilo.liberthia.logic.PhantomPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.0F).noOcclusion()
                    .lightLevel(s -> 8)
                    .sound(SoundType.AMETHYST)));

    // --- Ritual bowls (Occultism-inspired) ---
    public static final RegistryObject<Block> BLOOD_SACRIFICIAL_BOWL = BLOCKS.register("blood_sacrificial_bowl",
            () -> new br.com.murilo.liberthia.logic.BloodSacrificialBowlBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final RegistryObject<Block> GOLDEN_BLOOD_BOWL = BLOCKS.register("golden_blood_bowl",
            () -> new br.com.murilo.liberthia.logic.GoldenBloodBowlBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0F, 8.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 6)
                    .noOcclusion()));

    // --- Command Pylon (programmable command block) ---
    public static final RegistryObject<Block> COMMAND_PYLON = BLOCKS.register("command_pylon",
            () -> new br.com.murilo.liberthia.logic.CommandPylonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(s -> 9)
                    .sound(SoundType.METAL)
                    .noLootTable()));

    // ==================================================================
    // Sanguine Wood — Occultism-otherworld-style alternate wood family.
    // ==================================================================
    private static BlockBehaviour.Properties woodProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(2.0F, 3.0F)
                .sound(SoundType.WOOD);
    }

    public static final RegistryObject<Block> SANGUINE_LOG = BLOCKS.register("sanguine_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(woodProps()));
    public static final RegistryObject<Block> SANGUINE_WOOD = BLOCKS.register("sanguine_wood",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(woodProps()));
    public static final RegistryObject<Block> STRIPPED_SANGUINE_LOG = BLOCKS.register("stripped_sanguine_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(woodProps()));
    public static final RegistryObject<Block> STRIPPED_SANGUINE_WOOD = BLOCKS.register("stripped_sanguine_wood",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(woodProps()));
    public static final RegistryObject<Block> SANGUINE_PLANKS = BLOCKS.register("sanguine_planks",
            () -> new Block(woodProps()));
    public static final RegistryObject<Block> SANGUINE_LEAVES = BLOCKS.register("sanguine_leaves",
            () -> new net.minecraft.world.level.block.LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((s,g,p,e) -> false)
                    .isSuffocating((s,g,p) -> false)
                    .isViewBlocking((s,g,p) -> false)));
    public static final RegistryObject<Block> SANGUINE_SAPLING = BLOCKS.register("sanguine_sapling",
            () -> new br.com.murilo.liberthia.logic.SanguineSaplingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0F).noCollission()
                    .randomTicks()
                    .sound(SoundType.GRASS)));
    public static final RegistryObject<Block> SANGUINE_STAIRS = BLOCKS.register("sanguine_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(
                    () -> SANGUINE_PLANKS.get().defaultBlockState(), woodProps()));
    public static final RegistryObject<Block> SANGUINE_SLAB = BLOCKS.register("sanguine_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(woodProps()));
    public static final RegistryObject<Block> SANGUINE_FENCE = BLOCKS.register("sanguine_fence",
            () -> new net.minecraft.world.level.block.FenceBlock(woodProps()));
    public static final RegistryObject<Block> SANGUINE_FENCE_GATE = BLOCKS.register("sanguine_fence_gate",
            () -> new net.minecraft.world.level.block.FenceGateBlock(woodProps(), ModWoodTypes.SANGUINE));
    public static final RegistryObject<Block> SANGUINE_BUTTON = BLOCKS.register("sanguine_button",
            () -> new net.minecraft.world.level.block.ButtonBlock(
                    BlockBehaviour.Properties.of().noCollission().strength(0.5F).sound(SoundType.WOOD),
                    ModWoodTypes.SANGUINE_SET, 30, true));
    public static final RegistryObject<Block> SANGUINE_PRESSURE_PLATE = BLOCKS.register("sanguine_pressure_plate",
            () -> new net.minecraft.world.level.block.PressurePlateBlock(
                    net.minecraft.world.level.block.PressurePlateBlock.Sensitivity.EVERYTHING,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission()
                            .strength(0.5F).sound(SoundType.WOOD),
                    ModWoodTypes.SANGUINE_SET));
    public static final RegistryObject<Block> SANGUINE_DOOR = BLOCKS.register("sanguine_door",
            () -> new net.minecraft.world.level.block.DoorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F)
                            .sound(SoundType.WOOD).noOcclusion(),
                    ModWoodTypes.SANGUINE_SET));
    public static final RegistryObject<Block> SANGUINE_TRAPDOOR = BLOCKS.register("sanguine_trapdoor",
            () -> new net.minecraft.world.level.block.TrapDoorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F)
                            .sound(SoundType.WOOD).noOcclusion()
                            .isValidSpawn((s,g,p,e) -> false),
                    ModWoodTypes.SANGUINE_SET));

    // ==================================================================
    // Blood Tree — wood family separada com sapling que CRESCE de verdade
    // (TreeGrower + configured_feature em datapack JSON). Tronco vermelho-
    // vinho escuro, folhas com paleta sangue. Inclui família completa de
    // wood blocks. Crescimento por bone meal ou random tick natural.
    // ==================================================================
    private static BlockBehaviour.Properties bloodWoodProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(2.0F, 3.0F)
                .sound(SoundType.WOOD);
    }

    public static final RegistryObject<Block> BLOOD_LOG = BLOCKS.register("blood_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(bloodWoodProps()));
    public static final RegistryObject<Block> STRIPPED_BLOOD_LOG = BLOCKS.register("stripped_blood_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(bloodWoodProps()));
    public static final RegistryObject<Block> BLOOD_PLANKS = BLOCKS.register("blood_planks",
            () -> new Block(bloodWoodProps()));
    public static final RegistryObject<Block> BLOOD_LEAVES = BLOCKS.register("blood_leaves",
            () -> new br.com.murilo.liberthia.block.BloodLeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((s,g,p,e) -> false)
                    .isSuffocating((s,g,p) -> false)
                    .isViewBlocking((s,g,p) -> false)));
    public static final RegistryObject<Block> BLOOD_SAPLING = BLOCKS.register("blood_sapling",
            () -> new br.com.murilo.liberthia.block.BloodSaplingBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0F).noCollission()
                    .randomTicks()
                    .sound(SoundType.GRASS)));
    public static final RegistryObject<Block> BLOOD_STAIRS = BLOCKS.register("blood_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(
                    () -> BLOOD_PLANKS.get().defaultBlockState(), bloodWoodProps()));
    public static final RegistryObject<Block> BLOOD_SLAB = BLOCKS.register("blood_slab",
            () -> new net.minecraft.world.level.block.SlabBlock(bloodWoodProps()));
    public static final RegistryObject<Block> BLOOD_FENCE = BLOCKS.register("blood_fence",
            () -> new net.minecraft.world.level.block.FenceBlock(bloodWoodProps()));
    public static final RegistryObject<Block> BLOOD_FENCE_GATE = BLOCKS.register("blood_fence_gate",
            () -> new net.minecraft.world.level.block.FenceGateBlock(bloodWoodProps(),
                    net.minecraft.world.level.block.state.properties.WoodType.OAK));
    public static final RegistryObject<Block> BLOOD_DOOR = BLOCKS.register("blood_door",
            () -> new net.minecraft.world.level.block.DoorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F)
                            .sound(SoundType.WOOD).noOcclusion(),
                    net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
    public static final RegistryObject<Block> BLOOD_TRAPDOOR = BLOCKS.register("blood_trapdoor",
            () -> new net.minecraft.world.level.block.TrapDoorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0F)
                            .sound(SoundType.WOOD).noOcclusion()
                            .isValidSpawn((s,g,p,e) -> false),
                    net.minecraft.world.level.block.state.properties.BlockSetType.OAK));

    // ============================================================
    // INFECTED VARIANT BLOCKS — v0.1.13
    // 12 blocos novos: 4 variantes (dirt/sand/stone/grass) × 3 matérias
    // Cada bloco aplica efeito de status + ganha matéria ao pisar
    // ============================================================
    private static BlockBehaviour.Properties dirtBase() {
        return BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.GRAVEL);
    }
    private static BlockBehaviour.Properties sandBase() {
        return BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.SAND);
    }
    private static BlockBehaviour.Properties stoneBase() {
        return BlockBehaviour.Properties.of().strength(1.5F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }
    private static BlockBehaviour.Properties grassBase() {
        return BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.GRASS).randomTicks();
    }

    // --- DARK MATTER (DM) ---
    public static final RegistryObject<Block> DM_INFECTED_DIRT = BLOCKS.register("dm_infected_dirt",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK, dirtBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK,
                    net.minecraft.world.effect.MobEffects.WITHER, 60, 0, 0.5f));
    public static final RegistryObject<Block> DM_INFECTED_SAND = BLOCKS.register("dm_infected_sand",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK, sandBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK,
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 0, 0.4f));
    public static final RegistryObject<Block> DM_INFECTED_STONE = BLOCKS.register("dm_infected_stone",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK, stoneBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK,
                    net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0, 0.3f));
    public static final RegistryObject<Block> DM_INFECTED_GRASS = BLOCKS.register("dm_infected_grass",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK, grassBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.DARK,
                    net.minecraft.world.effect.MobEffects.HUNGER, 80, 0, 0.6f));

    // --- WHITE MATTER (WM) ---
    public static final RegistryObject<Block> WM_BLEACHED_DIRT = BLOCKS.register("wm_bleached_dirt",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE, dirtBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE,
                    net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0, 0.5f));
    public static final RegistryObject<Block> WM_BLEACHED_SAND = BLOCKS.register("wm_bleached_sand",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE, sandBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE,
                    net.minecraft.world.effect.MobEffects.LEVITATION, 30, 0, 0.4f));
    public static final RegistryObject<Block> WM_BLEACHED_STONE = BLOCKS.register("wm_bleached_stone",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE, stoneBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE,
                    net.minecraft.world.effect.MobEffects.GLOWING, 100, 0, 0.3f));
    public static final RegistryObject<Block> WM_BLEACHED_GRASS = BLOCKS.register("wm_bleached_grass",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE, grassBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.WHITE,
                    net.minecraft.world.effect.MobEffects.NIGHT_VISION, 200, 0, 0.6f));

    // --- YELLOW MATTER (YM) ---
    public static final RegistryObject<Block> YM_UNSTABLE_DIRT = BLOCKS.register("ym_unstable_dirt",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW, dirtBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW,
                    net.minecraft.world.effect.MobEffects.HUNGER, 100, 0, 0.5f));
    public static final RegistryObject<Block> YM_UNSTABLE_SAND = BLOCKS.register("ym_unstable_sand",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW, sandBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW,
                    net.minecraft.world.effect.MobEffects.CONFUSION, 80, 0, 0.4f));
    public static final RegistryObject<Block> YM_UNSTABLE_STONE = BLOCKS.register("ym_unstable_stone",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW, stoneBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW,
                    net.minecraft.world.effect.MobEffects.UNLUCK, 200, 0, 0.3f));
    public static final RegistryObject<Block> YM_UNSTABLE_GRASS = BLOCKS.register("ym_unstable_grass",
            () -> new br.com.murilo.liberthia.block.InfectedVariantBlock(
                    br.com.murilo.liberthia.block.InfectedVariantBlock.propsFor(
                            br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW, grassBase()),
                    br.com.murilo.liberthia.block.InfectedVariantBlock.MatterType.YELLOW,
                    net.minecraft.world.effect.MobEffects.WEAKNESS, 100, 0, 0.6f));

    // ────────────────────────────────────────────────────────────────────
    // v0.1.22 r24: SPIRIT WORLD — altar de ritual + bloco de pedra espiritual
    // ────────────────────────────────────────────────────────────────────

    /** Spirit Altar — ritual block (5s animation → Spirit World). */
    public static final RegistryObject<Block> SPIRIT_ALTAR = BLOCKS.register("spirit_altar",
            () -> new br.com.murilo.liberthia.block.SpiritAltarBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                            .strength(15.0F, 1200.0F)
                            .lightLevel(s -> 10)
                            .noOcclusion()));

    /** Spirit Stone — bloco "místico" pra construções rituais. */
    public static final RegistryObject<Block> SPIRIT_STONE = BLOCKS.register("spirit_stone",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.DEEPSLATE)
                            .strength(4.5F, 12.0F)
                            .lightLevel(s -> 3)));

    // ────────────────────────────────────────────────────────────────────
    // v0.1.22 r27: DIMENSIONAL ANTENNA — comunicação cross-dim
    // ────────────────────────────────────────────────────────────────────
    public static final RegistryObject<Block> DIMENSIONAL_ANTENNA = BLOCKS.register("dimensional_antenna",
            () -> new br.com.murilo.liberthia.block.DimensionalAntennaBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK)
                            .strength(3.0F, 15.0F)
                            .lightLevel(s -> 7)
                            .noOcclusion()));

    // ────────────────────────────────────────────────────────────────────
    // v0.1.22 r28: QUANTUM TERMINAL — computador receptor de mensagens
    // ────────────────────────────────────────────────────────────────────
    public static final RegistryObject<Block> QUANTUM_TERMINAL = BLOCKS.register("quantum_terminal",
            () -> new br.com.murilo.liberthia.block.QuantumTerminalBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
                            .strength(3.5F, 12.0F)
                            .lightLevel(s -> 6)
                            .noOcclusion()));

    // ════════════════════════════════════════════════════════════════════════
    // v0.1.22 r29: SPIRIT WORLD BLOCKS — identidade visual da dim espiritual
    // ════════════════════════════════════════════════════════════════════════

    /** Spirit Grass — chão místico da spirit world. */
    public static final RegistryObject<Block> SPIRIT_GRASS_BLOCK = BLOCKS.register("spirit_grass_block",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                            .lightLevel(s -> 2)));

    /** Spirit Dirt — terra etérea. */
    public static final RegistryObject<Block> SPIRIT_DIRT = BLOCKS.register("spirit_dirt",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.DIRT)));

    /** Ethereal Stone — pedra principal do spirit world. */
    public static final RegistryObject<Block> ETHEREAL_STONE = BLOCKS.register("ethereal_stone",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.STONE)
                            .lightLevel(s -> 1)));

    public static final RegistryObject<Block> ETHEREAL_STONE_BRICKS = BLOCKS.register("ethereal_stone_bricks",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.STONE_BRICKS)
                            .lightLevel(s -> 1)));

    /** Soul Brick — tijolo de almas. */
    public static final RegistryObject<Block> SOUL_BRICK = BLOCKS.register("soul_brick",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.SOUL_SAND)
                            .strength(3.0F, 6.0F)
                            .lightLevel(s -> 4)));

    /** Halo Marble — mármore branco-dourado celestial. */
    public static final RegistryObject<Block> HALO_MARBLE = BLOCKS.register("halo_marble",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK)
                            .lightLevel(s -> 3)));

    public static final RegistryObject<Block> HALO_MARBLE_BRICKS = BLOCKS.register("halo_marble_bricks",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.QUARTZ_BRICKS)
                            .lightLevel(s -> 3)));

    /** Dream Glass — vidro translúcido roxo. */
    public static final RegistryObject<Block> DREAM_GLASS = BLOCKS.register("dream_glass",
            () -> new net.minecraft.world.level.block.GlassBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.GLASS)
                            .lightLevel(s -> 5)));

    /** Whisperwood Log — madeira fantasma. */
    public static final RegistryObject<Block> WHISPERWOOD_LOG = BLOCKS.register("whisperwood_log",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.OAK_LOG)
                            .lightLevel(s -> 2)));

    public static final RegistryObject<Block> WHISPERWOOD_PLANKS = BLOCKS.register("whisperwood_planks",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                            .lightLevel(s -> 1)));

    // v0.1.24 r80: usa custom WhisperwoodLeavesBlock pra impedir decay
    // (pattern do BloodLeavesBlock — folhas viram persistent=true sempre)
    public static final RegistryObject<Block> WHISPERWOOD_LEAVES = BLOCKS.register("whisperwood_leaves",
            () -> new br.com.murilo.liberthia.block.WhisperwoodLeavesBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.OAK_LEAVES)
                            .lightLevel(s -> 6)));

    /** Astral Lantern — lanterna emite luz mística. */
    public static final RegistryObject<Block> ASTRAL_LANTERN = BLOCKS.register("astral_lantern",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.SEA_LANTERN)
                            .lightLevel(s -> 15)
                            .noOcclusion()));

    /** Crystal Spirit Ore — ore que dropa cristais espirituais. */
    public static final RegistryObject<Block> CRYSTAL_SPIRIT_ORE = BLOCKS.register("crystal_spirit_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK)
                            .strength(4.0F, 4.0F)
                            .lightLevel(s -> 8)
                            .requiresCorrectToolForDrops(),
                    net.minecraft.util.valueproviders.UniformInt.of(2, 5)));

    /** Sanctum Ward — dome protetor (placeholder block). */
    public static final RegistryObject<Block> SANCTUM_WARD = BLOCKS.register("sanctum_ward",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.QUARTZ_BLOCK)
                            .strength(50.0F, 1200.0F)
                            .lightLevel(s -> 14)
                            .noOcclusion()));

    /** Holy Censer — bloco que emite fog curativo. */
    public static final RegistryObject<Block> HOLY_CENSER = BLOCKS.register("holy_censer",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.GOLD_BLOCK)
                            .strength(2.5F, 6.0F)
                            .lightLevel(s -> 10)
                            .noOcclusion()));

    // ════════════════════════════════════════════════════════════════════════
    // v0.1.22 r32: OCCULT SYSTEM — Chalk Marks (5 colors), Candles (5),
    // Ritual Circle, Spirit Miner
    // ════════════════════════════════════════════════════════════════════════

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties chalkProps() {
        return net.minecraft.world.level.block.state.BlockBehaviour.Properties
                .of()
                .noCollission()
                .noOcclusion()
                .instabreak()
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
    }
    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties candleProps(int lightWhenLit) {
        return net.minecraft.world.level.block.state.BlockBehaviour.Properties
                .copy(net.minecraft.world.level.block.Blocks.CANDLE)
                .noOcclusion();
    }

    public static final RegistryObject<Block> CHALK_MARK_WHITE = BLOCKS.register("chalk_mark_white",
            () -> {
                var b = new br.com.murilo.liberthia.occult.ChalkMarkBlock(chalkProps(), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.WHITE);
                br.com.murilo.liberthia.occult.ChalkMarkBlock.register(br.com.murilo.liberthia.occult.OccultItems.ChalkColor.WHITE, b);
                return b;
            });
    public static final RegistryObject<Block> CHALK_MARK_GOLDEN = BLOCKS.register("chalk_mark_golden",
            () -> {
                var b = new br.com.murilo.liberthia.occult.ChalkMarkBlock(chalkProps(), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.GOLDEN);
                br.com.murilo.liberthia.occult.ChalkMarkBlock.register(br.com.murilo.liberthia.occult.OccultItems.ChalkColor.GOLDEN, b);
                return b;
            });
    public static final RegistryObject<Block> CHALK_MARK_PURPLE = BLOCKS.register("chalk_mark_purple",
            () -> {
                var b = new br.com.murilo.liberthia.occult.ChalkMarkBlock(chalkProps(), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.PURPLE);
                br.com.murilo.liberthia.occult.ChalkMarkBlock.register(br.com.murilo.liberthia.occult.OccultItems.ChalkColor.PURPLE, b);
                return b;
            });
    public static final RegistryObject<Block> CHALK_MARK_RED = BLOCKS.register("chalk_mark_red",
            () -> {
                var b = new br.com.murilo.liberthia.occult.ChalkMarkBlock(chalkProps(), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.RED);
                br.com.murilo.liberthia.occult.ChalkMarkBlock.register(br.com.murilo.liberthia.occult.OccultItems.ChalkColor.RED, b);
                return b;
            });
    public static final RegistryObject<Block> CHALK_MARK_BLACK = BLOCKS.register("chalk_mark_black",
            () -> {
                var b = new br.com.murilo.liberthia.occult.ChalkMarkBlock(chalkProps(), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.BLACK);
                br.com.murilo.liberthia.occult.ChalkMarkBlock.register(br.com.murilo.liberthia.occult.OccultItems.ChalkColor.BLACK, b);
                return b;
            });

    public static final RegistryObject<Block> CANDLE_WHITE_OCCULT = BLOCKS.register("candle_occult_white",
            () -> new br.com.murilo.liberthia.occult.CandleBlock(candleProps(12), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.WHITE));
    public static final RegistryObject<Block> CANDLE_GOLDEN_OCCULT = BLOCKS.register("candle_occult_golden",
            () -> new br.com.murilo.liberthia.occult.CandleBlock(candleProps(15), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.GOLDEN));
    public static final RegistryObject<Block> CANDLE_PURPLE_OCCULT = BLOCKS.register("candle_occult_purple",
            () -> new br.com.murilo.liberthia.occult.CandleBlock(candleProps(11), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.PURPLE));
    public static final RegistryObject<Block> CANDLE_RED_OCCULT = BLOCKS.register("candle_occult_red",
            () -> new br.com.murilo.liberthia.occult.CandleBlock(candleProps(12), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.RED));
    public static final RegistryObject<Block> CANDLE_BLACK_OCCULT = BLOCKS.register("candle_occult_black",
            () -> new br.com.murilo.liberthia.occult.CandleBlock(candleProps(8), br.com.murilo.liberthia.occult.OccultItems.ChalkColor.BLACK));

    public static final RegistryObject<Block> RITUAL_CIRCLE = BLOCKS.register("ritual_circle",
            () -> new br.com.murilo.liberthia.occult.RitualCircleBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(s -> 7)
                            .noOcclusion()));

    public static final RegistryObject<Block> SPIRIT_MINER = BLOCKS.register("spirit_miner",
            () -> new br.com.murilo.liberthia.occult.SpiritMinerBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.IRON_BLOCK)
                            .strength(4.0F, 12.0F)
                            .lightLevel(s -> 4)));

    // ════════════════════════════════════════════════════════════════════════
    // v0.1.22 r33: LOOM PORTAL — bloco do portal pra dim Loom
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Block> LOOM_PORTAL = BLOCKS.register("loom_portal",
            () -> new br.com.murilo.liberthia.loom.LoomPortalBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.NETHER_PORTAL)
                            .lightLevel(s -> 11)
                            .noCollission()
                            .noOcclusion()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                            .strength(-1.0F)));

    // ════════════════════════════════════════════════════════════════════════
    // r34: LOOM ORES — minérios exclusivos da dimensão Loom
    // ════════════════════════════════════════════════════════════════════════
    public static final RegistryObject<Block> LOOM_STONE = BLOCKS.register("loom_stone",
            () -> new net.minecraft.world.level.block.Block(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.END_STONE)
                            .strength(2.0F, 9.0F)));

    public static final RegistryObject<Block> VOIDITE_ORE = BLOCKS.register("loom_voidite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE)
                            .strength(4.5F, 6.0F)
                            .lightLevel(s -> 2),
                    net.minecraft.util.valueproviders.UniformInt.of(2, 5)));

    public static final RegistryObject<Block> RIFTITE_ORE = BLOCKS.register("loom_riftite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE)
                            .strength(5.0F, 8.0F)
                            .lightLevel(s -> 5),
                    net.minecraft.util.valueproviders.UniformInt.of(3, 7)));

    public static final RegistryObject<Block> UMBRAL_ORE = BLOCKS.register("loom_umbral_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE)
                            .strength(3.5F, 5.0F)
                            .lightLevel(s -> 7),
                    net.minecraft.util.valueproviders.UniformInt.of(2, 4)));

    // r62: Source Jar — block storage de Source/mana cosmico
    public static final RegistryObject<Block> SOURCE_JAR = BLOCKS.register("source_jar",
            () -> new br.com.murilo.liberthia.observation.source.SourceJarBlock());

    // r69: Scribes Table — mesa de crafting de spell parchments
    public static final RegistryObject<Block> SCRIBES_TABLE = BLOCKS.register("scribes_table",
            () -> new br.com.murilo.liberthia.block.ScribesTableBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.LECTERN)
                            .strength(2.5F, 5.0F)
                            .lightLevel(s -> 4)
                            .noOcclusion()));

    // r72: Rune Block — desenhada com Chalk, ativada ao pisar
    public static final RegistryObject<Block> RUNE_BLOCK = BLOCKS.register("rune_block",
            () -> new br.com.murilo.liberthia.observation.block.RuneBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.WHITE_CARPET)
                            .strength(0.1F, 0.1F)
                            .lightLevel(s -> 6)
                            .noCollission()
                            .noOcclusion()));

    // r73: Imbuement Table — bloco dedicado pra encantar espadas com feitiços
    public static final RegistryObject<Block> IMBUEMENT_TABLE = BLOCKS.register("imbuement_table",
            () -> new br.com.murilo.liberthia.observation.block.ImbuementBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE)
                            .strength(5.0F, 1200.0F)
                            .lightLevel(s -> 8)
                            .noOcclusion()));

    // r74: Sourcestone Ore — minério na Spirit World (cosmetic, drops shard)
    public static final RegistryObject<Block> SOURCESTONE_ORE = BLOCKS.register("sourcestone_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.STONE)
                            .strength(3.0F, 3.0F)
                            .lightLevel(s -> 4)
                            .requiresCorrectToolForDrops(),
                    net.minecraft.util.valueproviders.UniformInt.of(1, 3)));

    // r74: Spirit Gem Ore — drops Soul Fragment
    public static final RegistryObject<Block> SPIRIT_GEM_ORE = BLOCKS.register("spirit_gem_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.DEEPSLATE)
                            .strength(4.5F, 4.5F)
                            .lightLevel(s -> 6)
                            .requiresCorrectToolForDrops(),
                    net.minecraft.util.valueproviders.UniformInt.of(2, 4)));

    // r74: Mana Berry crop
    public static final RegistryObject<Block> MANA_BERRY_BUSH = BLOCKS.register("mana_berry_bush",
            () -> new br.com.murilo.liberthia.observation.block.ManaBerryBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)
                            .lightLevel(s -> 4)
                            .randomTicks()
                            .noCollission()));

    // r74: Source Relay
    public static final RegistryObject<Block> SOURCE_RELAY = BLOCKS.register("source_relay",
            () -> new br.com.murilo.liberthia.observation.block.SourceRelayBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.LODESTONE)
                            .strength(3.5F, 3.5F)
                            .lightLevel(s -> 7)));

    // r77: Spell Binding Pedestal — combina Book + Parchment → Book imprintado
    public static final RegistryObject<Block> SPELL_BINDING_PEDESTAL = BLOCKS.register("spell_binding_pedestal",
            () -> new br.com.murilo.liberthia.observation.block.SpellBindingPedestalBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties
                            .copy(net.minecraft.world.level.block.Blocks.LECTERN)
                            .strength(3.0F, 6.0F)
                            .lightLevel(s -> 8)
                            .noOcclusion()));

    private ModBlocks() {

    }

    public static void register(IEventBus eventBus) {
        ModWoodTypes.bootstrap();
        BLOCKS.register(eventBus);
    }
}
