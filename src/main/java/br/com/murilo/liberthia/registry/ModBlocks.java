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

    private ModBlocks() {

    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
