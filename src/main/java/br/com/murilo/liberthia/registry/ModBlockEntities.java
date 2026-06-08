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

    // r180: Motor de Entropia
    public static final RegistryObject<BlockEntityType<EntropyEngineBlockEntity>> ENTROPY_ENGINE =
            BLOCK_ENTITIES.register("entropy_engine",
                    () -> BlockEntityType.Builder.of(EntropyEngineBlockEntity::new, ModBlocks.ENTROPY_ENGINE.get()).build(null));

    // r180b: Âncora de Realidade
    public static final RegistryObject<BlockEntityType<RealityAnchorBlockEntity>> REALITY_ANCHOR =
            BLOCK_ENTITIES.register("reality_anchor",
                    () -> BlockEntityType.Builder.of(RealityAnchorBlockEntity::new, ModBlocks.REALITY_ANCHOR.get()).build(null));

    // r180b: Raio Estabilizador
    public static final RegistryObject<BlockEntityType<StabilizerBeamBlockEntity>> STABILIZER_BEAM =
            BLOCK_ENTITIES.register("stabilizer_beam",
                    () -> BlockEntityType.Builder.of(StabilizerBeamBlockEntity::new, ModBlocks.STABILIZER_BEAM.get()).build(null));

    // r180b: Reator de Sanidade
    public static final RegistryObject<BlockEntityType<SanityReactorBlockEntity>> SANITY_REACTOR =
            BLOCK_ENTITIES.register("sanity_reactor",
                    () -> BlockEntityType.Builder.of(SanityReactorBlockEntity::new, ModBlocks.SANITY_REACTOR.get()).build(null));

    // r180b: Sifão de Fenda
    public static final RegistryObject<BlockEntityType<RiftSiphonBlockEntity>> RIFT_SIPHON =
            BLOCK_ENTITIES.register("rift_siphon",
                    () -> BlockEntityType.Builder.of(RiftSiphonBlockEntity::new, ModBlocks.RIFT_SIPHON.get()).build(null));

    // r180: Pilar Protetor (anti-magia)
    public static final RegistryObject<BlockEntityType<WardPillarBlockEntity>> WARD_PILLAR =
            BLOCK_ENTITIES.register("ward_pillar",
                    () -> BlockEntityType.Builder.of(WardPillarBlockEntity::new, ModBlocks.WARD_PILLAR.get()).build(null));

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

    public static final RegistryObject<BlockEntityType<MatterTesterBlockEntity>> MATTER_TESTER = BLOCK_ENTITIES.register("matter_tester",
            () -> BlockEntityType.Builder.of(MatterTesterBlockEntity::new, ModBlocks.MATTER_TESTER.get()).build(null));

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

    // r97: Repository BE
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.storage.RepositoryBlockEntity>> REPOSITORY =
            BLOCK_ENTITIES.register("repository",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.storage.RepositoryBlockEntity::new,
                            ModBlocks.REPOSITORY.get()).build(null));

    // r164: AUTO_MINER BE removido.
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.automation.MageCauldronBlock.Tile>> MAGE_CAULDRON =
            BLOCK_ENTITIES.register("mage_cauldron",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.automation.MageCauldronBlock.Tile::new,
                            ModBlocks.MAGE_CAULDRON.get()).build(null));

    // r92: Mob Jar + Potion Jar BE
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.storage.MobJarBlockEntity>> MOB_JAR =
            BLOCK_ENTITIES.register("mob_jar",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.storage.MobJarBlockEntity::new,
                            ModBlocks.MOB_JAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.storage.PotionJarBlockEntity>> POTION_JAR =
            BLOCK_ENTITIES.register("potion_jar",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.storage.PotionJarBlockEntity::new,
                            ModBlocks.POTION_JAR.get()).build(null));

    // Computador (bloco) — armazena relatórios + login + HD
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.storage.ComputerBlockEntity>> COMPUTER_BLOCK =
            BLOCK_ENTITIES.register("computer_block",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.storage.ComputerBlockEntity::new,
                            ModBlocks.COMPUTER_BLOCK.get()).build(null));

    // Impressora — slot de papel + lê o computador adjacente
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.storage.PrinterBlockEntity>> PRINTER =
            BLOCK_ENTITIES.register("printer",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.storage.PrinterBlockEntity::new,
                            ModBlocks.PRINTER.get()).build(null));

    // r172: Tear de Threads
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.thread.ThreadLoomBlockEntity>> THREAD_LOOM =
            BLOCK_ENTITIES.register("thread_loom",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.thread.ThreadLoomBlockEntity::new,
                            ModBlocks.THREAD_LOOM.get()).build(null));

    // r174: Infusor de Orbs
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.orb.OrbInfuserBlockEntity>> ORB_INFUSER =
            BLOCK_ENTITIES.register("orb_infuser",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.orb.OrbInfuserBlockEntity::new,
                            ModBlocks.ORB_INFUSER.get()).build(null));

    // r88: Spell Turret BE
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.automation.SpellTurretBlockEntity>> SPELL_TURRET =
            BLOCK_ENTITIES.register("spell_turret",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.automation.SpellTurretBlockEntity::new,
                            ModBlocks.SPELL_TURRET.get()).build(null));

    // r164: RITUAL_BRAZIER BE e os 5 SOURCELINK BEs removidos.

    // r118: Glyph Inscriber
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.glyph.inscriber.GlyphInscriberBlockEntity>> GLYPH_INSCRIBER =
            BLOCK_ENTITIES.register("glyph_inscriber",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.glyph.inscriber.GlyphInscriberBlockEntity::new,
                            ModBlocks.GLYPH_INSCRIBER.get()).build(null));

    // r119: Spell Weaver
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.spell.weaver.SpellWeaverBlockEntity>> SPELL_WEAVER =
            BLOCK_ENTITIES.register("spell_weaver",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.spell.weaver.SpellWeaverBlockEntity::new,
                            ModBlocks.SPELL_WEAVER.get()).build(null));

    // r117: Spirit Conduit + Source Transmuter
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.conduit.SpiritConduitBlockEntity>> SPIRIT_CONDUIT =
            BLOCK_ENTITIES.register("spirit_conduit",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.conduit.SpiritConduitBlockEntity::new,
                            ModBlocks.SPIRIT_CONDUIT.get()).build(null));

    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.conduit.SourceTransmuterBlockEntity>> SOURCE_TRANSMUTER =
            BLOCK_ENTITIES.register("source_transmuter",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.conduit.SourceTransmuterBlockEntity::new,
                            ModBlocks.SOURCE_TRANSMUTER.get()).build(null));

    // r138: Lay Line — gera Source ambiente
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.layline.LayLineBlock.Tile>> LAY_LINE =
            BLOCK_ENTITIES.register("lay_line",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.layline.LayLineBlock.Tile::new,
                            ModBlocks.LAY_LINE.get()).build(null));

    // r138: Spell Mutator — combina 2 scrolls em hibrido
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.spell.mutator.SpellMutatorBlockEntity>> SPELL_MUTATOR =
            BLOCK_ENTITIES.register("spell_mutator",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.spell.mutator.SpellMutatorBlockEntity::new,
                            ModBlocks.SPELL_MUTATOR.get()).build(null));

    // r155 Phase 2: Arcane Workbench — base scroll + 7 mod glyphs → composed scroll
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.workbench.ArcaneWorkbenchBlockEntity>> ARCANE_WORKBENCH =
            BLOCK_ENTITIES.register("arcane_workbench",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.workbench.ArcaneWorkbenchBlockEntity::new,
                            ModBlocks.ARCANE_WORKBENCH.get()).build(null));

    // r164: Inscription Table — Menu + recipes panel
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.scribe.InscriptionTableBlockEntity>> INSCRIPTION_TABLE =
            BLOCK_ENTITIES.register("inscription_table",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.scribe.InscriptionTableBlockEntity::new,
                            ModBlocks.INSCRIPTION_TABLE.get()).build(null));

    // r165: Scroll Forge — EntityBlock com GUI (Focus → Scroll)
    public static final RegistryObject<BlockEntityType<br.com.murilo.liberthia.magic.scribe.ScrollForgeBlockEntity>> SCROLL_FORGE =
            BLOCK_ENTITIES.register("scroll_forge",
                    () -> BlockEntityType.Builder.of(
                            br.com.murilo.liberthia.magic.scribe.ScrollForgeBlockEntity::new,
                            ModBlocks.SCROLL_FORGE.get()).build(null));

    // r185 — Forja de Fendas
    public static final RegistryObject<BlockEntityType<RiftForgeBlockEntity>> RIFT_FORGE =
            BLOCK_ENTITIES.register("rift_forge",
                    () -> BlockEntityType.Builder.of(RiftForgeBlockEntity::new, ModBlocks.RIFT_FORGE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
