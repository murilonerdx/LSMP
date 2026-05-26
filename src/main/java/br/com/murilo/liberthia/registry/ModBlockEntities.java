package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.block.entity.*;
import br.com.murilo.liberthia.blockentity.RitualPedestalBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LiberthiaMod.MODID);

    public static final RegistryObject<BlockEntityType<PurificationBenchBlockEntity>> PURIFICATION_BENCH = BLOCK_ENTITIES.register("purification_bench",
            () -> BlockEntityType.Builder.of(PurificationBenchBlockEntity::new, ModBlocks.PURIFICATION_BENCH.get()).build(null));

    public static final RegistryObject<BlockEntityType<RitualPedestalBlockEntity>> RITUAL_PEDESTAL =
            BLOCK_ENTITIES.register("ritual_pedestal",
                    () -> BlockEntityType.Builder.of(
                            RitualPedestalBlockEntity::new,
                            ModBlocks.RITUAL_PEDESTAL.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<DarkMatterForgeBlockEntity>> DARK_MATTER_FORGE = BLOCK_ENTITIES.register("dark_matter_forge",
            () -> BlockEntityType.Builder.of(DarkMatterForgeBlockEntity::new, ModBlocks.DARK_MATTER_FORGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MatterInfuserBlockEntity>> MATTER_INFUSER = BLOCK_ENTITIES.register("matter_infuser",
            () -> BlockEntityType.Builder.of(MatterInfuserBlockEntity::new, ModBlocks.MATTER_INFUSER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE = BLOCK_ENTITIES.register("research_table",
            () -> BlockEntityType.Builder.of(ResearchTableBlockEntity::new, ModBlocks.RESEARCH_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ContainmentChamberBlockEntity>> CONTAINMENT_CHAMBER = BLOCK_ENTITIES.register("containment_chamber",
            () -> BlockEntityType.Builder.of(ContainmentChamberBlockEntity::new, ModBlocks.CONTAINMENT_CHAMBER.get()).build(null));

    public static final RegistryObject<BlockEntityType<MatterTransmuterBlockEntity>> MATTER_TRANSMUTER = BLOCK_ENTITIES.register("matter_transmuter",
            () -> BlockEntityType.Builder.of(MatterTransmuterBlockEntity::new, ModBlocks.MATTER_TRANSMUTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.DarkMatterAlchemizerBlockEntity>> DARK_MATTER_ALCHEMIZER = BLOCK_ENTITIES.register("dark_matter_alchemizer",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.DarkMatterAlchemizerBlockEntity::new, ModBlocks.DARK_MATTER_ALCHEMIZER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.DarkMatterGeneratorBlockEntity>> DARK_MATTER_GENERATOR = BLOCK_ENTITIES.register("dark_matter_generator",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.DarkMatterGeneratorBlockEntity::new, ModBlocks.DARK_MATTER_GENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.EnergyCableBlockEntity>> ENERGY_CABLE = BLOCK_ENTITIES.register("energy_cable",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.EnergyCableBlockEntity::new, ModBlocks.ENERGY_CABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity>> ITEM_PIPE = BLOCK_ENTITIES.register("item_pipe",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity::new,
                    ModBlocks.ITEM_PIPE.get(),
                    ModBlocks.ITEM_EXTRACTOR.get(),
                    ModBlocks.ITEM_INSERTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<DimensionalChestBlockEntity>> DIMENSIONAL_CHEST =
            BLOCK_ENTITIES.register("dimensional_chest",
                    () -> BlockEntityType.Builder.of(DimensionalChestBlockEntity::new,
                            ModBlocks.DIMENSIONAL_CHEST.get()).build(null));

    public static final RegistryObject<BlockEntityType<MatterRefinerBlockEntity>> MATTER_REFINER =
            BLOCK_ENTITIES.register("matter_refiner",
                    () -> BlockEntityType.Builder.of(MatterRefinerBlockEntity::new,
                            ModBlocks.MATTER_REFINER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.MatterPurifierBlockEntity>> MATTER_PURIFIER =
            BLOCK_ENTITIES.register("matter_purifier",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.MatterPurifierBlockEntity::new,
                            ModBlocks.MATTER_PURIFIER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.MatterPillBrewerBlockEntity>> MATTER_PILL_BREWER =
            BLOCK_ENTITIES.register("matter_pill_brewer",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.MatterPillBrewerBlockEntity::new,
                            ModBlocks.MATTER_PILL_BREWER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.DarkMatterChestBlockEntity>> DARK_MATTER_CHEST = BLOCK_ENTITIES.register("dark_matter_chest",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.DarkMatterChestBlockEntity::new, ModBlocks.DARK_MATTER_CHEST.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.FragmentedGeneratorBlockEntity>> FRAGMENTED_GENERATOR = BLOCK_ENTITIES.register("fragmented_generator",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.FragmentedGeneratorBlockEntity::new, ModBlocks.FRAGMENTED_GENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.LaserEmitterBlockEntity>> LASER_EMITTER = BLOCK_ENTITIES.register("laser_emitter",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.LaserEmitterBlockEntity::new, ModBlocks.LASER_EMITTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.CrystallizerBlockEntity>> CRYSTALLIZER = BLOCK_ENTITIES.register("crystallizer",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.CrystallizerBlockEntity::new, ModBlocks.CRYSTALLIZER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.AutoFarmerBlockEntity>> AUTO_FARMER = BLOCK_ENTITIES.register("auto_farmer",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.AutoFarmerBlockEntity::new, ModBlocks.AUTO_FARMER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.DimensionalExtractorBlockEntity>> DIMENSIONAL_EXTRACTOR = BLOCK_ENTITIES.register("dimensional_extractor",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.DimensionalExtractorBlockEntity::new, ModBlocks.DIMENSIONAL_EXTRACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.MatterAnalyzerBlockEntity>> MATTER_ANALYZER = BLOCK_ENTITIES.register("matter_analyzer",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.MatterAnalyzerBlockEntity::new, ModBlocks.MATTER_ANALYZER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.WirelessChargerBlockEntity>> WIRELESS_CHARGER = BLOCK_ENTITIES.register("wireless_charger",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.WirelessChargerBlockEntity::new, ModBlocks.WIRELESS_CHARGER.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.BasicBatteryBlockEntity>> BATTERY_BASIC = BLOCK_ENTITIES.register("dm_battery_basic",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.BasicBatteryBlockEntity::new, ModBlocks.BATTERY_BASIC.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.AdvancedBatteryBlockEntity>> BATTERY_ADVANCED = BLOCK_ENTITIES.register("dm_battery_advanced",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.AdvancedBatteryBlockEntity::new, ModBlocks.BATTERY_ADVANCED.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.QuantumBatteryBlockEntity>> BATTERY_QUANTUM = BLOCK_ENTITIES.register("dm_battery_quantum",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.QuantumBatteryBlockEntity::new, ModBlocks.BATTERY_QUANTUM.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.InfectionHeartBlockEntity>> INFECTION_HEART = BLOCK_ENTITIES.register("infection_heart",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.InfectionHeartBlockEntity::new, ModBlocks.INFECTION_HEART.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.BloodCauldronBlockEntity>> BLOOD_CAULDRON = BLOCK_ENTITIES.register("blood_cauldron",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.BloodCauldronBlockEntity::new, ModBlocks.BLOOD_CAULDRON.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.CommandPylonBlockEntity>> COMMAND_PYLON = BLOCK_ENTITIES.register("command_pylon",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.CommandPylonBlockEntity::new, ModBlocks.COMMAND_PYLON.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.BloodSacrificialBowlBlockEntity>> BLOOD_SACRIFICIAL_BOWL = BLOCK_ENTITIES.register("blood_sacrificial_bowl",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.BloodSacrificialBowlBlockEntity::new, ModBlocks.BLOOD_SACRIFICIAL_BOWL.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity>> GOLDEN_BLOOD_BOWL = BLOCK_ENTITIES.register("golden_blood_bowl",
            () -> BlockEntityType.Builder.of(br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity::new, ModBlocks.GOLDEN_BLOOD_BOWL.get()).build(null));

    // --- Matter Extraction Network ---
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.MatterExtractorBlockEntity>> MATTER_EXTRACTOR =
            BLOCK_ENTITIES.register("matter_extractor",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.MatterExtractorBlockEntity::new,
                            ModBlocks.MATTER_EXTRACTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.MatterTankBlockEntity>> MATTER_TANK =
            BLOCK_ENTITIES.register("matter_tank",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.MatterTankBlockEntity::new,
                            ModBlocks.MATTER_TANK.get()).build(null));

    /**
     * 1 BlockEntityType compartilhado entre as 3 variantes de pipe (dark/clear/yellow).
     * Pode usar o mesmo BE porque ele lê o tipo do bloco via BlockState (allowedFluid
     * vem do MatterPipeBlock no momento do new). Igual ao pattern do ItemPipe que
     * compartilha BE com 3 blocos.
     */
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.MatterPipeBlockEntity>> MATTER_PIPE =
            BLOCK_ENTITIES.register("matter_pipe",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.MatterPipeBlockEntity::new,
                            ModBlocks.MATTER_PIPE_DARK.get(),
                            ModBlocks.MATTER_PIPE_CLEAR.get(),
                            ModBlocks.MATTER_PIPE_YELLOW.get()).build(null));

    // v0.1.22 r24: Spirit Altar — ritual block (5s animation → Spirit World)
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.SpiritAltarBlockEntity>> SPIRIT_ALTAR =
            BLOCK_ENTITIES.register("spirit_altar",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.SpiritAltarBlockEntity::new,
                            ModBlocks.SPIRIT_ALTAR.get()).build(null));

    // v0.1.22 r27: Dimensional Antenna — cross-dim chat broadcast
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.DimensionalAntennaBlockEntity>> DIMENSIONAL_ANTENNA =
            BLOCK_ENTITIES.register("dimensional_antenna",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.DimensionalAntennaBlockEntity::new,
                            ModBlocks.DIMENSIONAL_ANTENNA.get()).build(null));

    // v0.1.22 r28: Quantum Terminal — receptor de mensagens cross-dim
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.QuantumTerminalBlockEntity>> QUANTUM_TERMINAL =
            BLOCK_ENTITIES.register("quantum_terminal",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.QuantumTerminalBlockEntity::new,
                            ModBlocks.QUANTUM_TERMINAL.get()).build(null));

    // v0.1.22 r32: Ritual Circle — coração do sistema occult
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.occult.RitualCircleBlockEntity>> RITUAL_CIRCLE =
            BLOCK_ENTITIES.register("ritual_circle",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.occult.RitualCircleBlockEntity::new,
                            ModBlocks.RITUAL_CIRCLE.get()).build(null));

    // v0.1.22 r32: Spirit Miner — automação alimentada por Foliot
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.occult.SpiritMinerBlockEntity>> SPIRIT_MINER =
            BLOCK_ENTITIES.register("spirit_miner",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.occult.SpiritMinerBlockEntity::new,
                            ModBlocks.SPIRIT_MINER.get()).build(null));

    // r62: Source Jar — block storage de Source mana
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.observation.source.SourceJarTile>> SOURCE_JAR =
            BLOCK_ENTITIES.register("source_jar",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.observation.source.SourceJarTile::new,
                            ModBlocks.SOURCE_JAR.get()).build(null));

    // r69: Scribes Table — tile com inventário pra crafting de spell parchments
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.block.entity.ScribesTableBlockEntity>> SCRIBES_TABLE =
            BLOCK_ENTITIES.register("scribes_table",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.block.entity.ScribesTableBlockEntity::new,
                            ModBlocks.SCRIBES_TABLE.get()).build(null));

    // r72: Rune Block tile — armazena spell recipe
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.observation.block.RuneBlockEntity>> RUNE_BLOCK =
            BLOCK_ENTITIES.register("rune_block",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.observation.block.RuneBlockEntity::new,
                            ModBlocks.RUNE_BLOCK.get()).build(null));

    // r73: Imbuement Table tile — inventário 4 slots pra encantar espadas
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.observation.block.ImbuementBlockEntity>> IMBUEMENT_TABLE =
            BLOCK_ENTITIES.register("imbuement_table",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.observation.block.ImbuementBlockEntity::new,
                            ModBlocks.IMBUEMENT_TABLE.get()).build(null));

    // r74: Source Relay tile — tick a cada 2s transferindo source
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.observation.block.SourceRelayBlock.SourceRelayTile>> SOURCE_RELAY =
            BLOCK_ENTITIES.register("source_relay",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.observation.block.SourceRelayBlock.SourceRelayTile::new,
                            ModBlocks.SOURCE_RELAY.get()).build(null));

    // r77: Spell Binding Pedestal tile — 3-slot inventário Book + Parchment → Output
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.observation.block.SpellBindingPedestalBlockEntity>> SPELL_BINDING_PEDESTAL =
            BLOCK_ENTITIES.register("spell_binding_pedestal",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.observation.block.SpellBindingPedestalBlockEntity::new,
                            ModBlocks.SPELL_BINDING_PEDESTAL.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
