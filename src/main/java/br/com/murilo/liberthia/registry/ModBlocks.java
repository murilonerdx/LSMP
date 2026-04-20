package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.logic.ClearMatterBlock;
import br.com.murilo.liberthia.logic.CorruptedSoilBlock;
import br.com.murilo.liberthia.logic.DarkMatterBlock;
import br.com.murilo.liberthia.logic.DarkMatterFluidBlock;
import br.com.murilo.liberthia.logic.InfectionGrowthBlock;
import net.minecraft.util.valueproviders.UniformInt;
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

    public static final RegistryObject<Block> YELLOW_MATTER_BLOCK = BLOCKS.register("yellow_matter_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(5.0F, 10.0F)
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

    public static final RegistryObject<Block> WHITE_MATTER_BOMB_BLOCK = BLOCKS.register("white_matter_bomb",
            () -> new br.com.murilo.liberthia.block.WhiteMatterBombBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.5F)
                    .sound(SoundType.GLASS)));

    public static final RegistryObject<Block> PURITY_BEACON = BLOCKS.register("purity_beacon",
            () -> new br.com.murilo.liberthia.logic.PurityBeaconBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(5.0F)
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

    // --- Glitch Block ---
    public static final RegistryObject<Block> GLITCH_BLOCK = BLOCKS.register("glitch_block",
            () -> new br.com.murilo.liberthia.logic.GlitchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0F)
                    .lightLevel(state -> 0)
                    .emissiveRendering((s, g, p) -> true)
                    .randomTicks()
                    .sound(SoundType.AMETHYST)));

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

    private ModBlocks() {

    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
