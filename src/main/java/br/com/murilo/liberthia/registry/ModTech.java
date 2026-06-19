package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * r180c — <b>Liberthia Tech</b>: linha de tecnologia estilo Powah/Mekanism (energia,
 * máquinas, ferramentas, materiais). Usa DeferredRegisters PRÓPRIOS (separados de
 * ModItems/ModBlocks, que já são gigantes) apontando pros mesmos registries do Forge.
 * Registrado no mod bus via {@link #register} (chamado em LiberthiaMod).
 *
 * <p>Meta: 40 features. Construído em lotes (ver creative tab "tech").
 */
public final class ModTech {

    private ModTech() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LiberthiaMod.MODID);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LiberthiaMod.MODID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LiberthiaMod.MODID);

    private static Item.Properties props() { return new Item.Properties(); }

    private static BlockBehaviour.Properties metal(float hard) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                .strength(hard, hard + 1.0F).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    // ──────────── Lote 1: materiais base ────────────
    public static final RegistryObject<Item> STEEL_INGOT =
            ITEMS.register("steel_ingot", () -> new Item(props()));
    public static final RegistryObject<Item> STEEL_PLATE =
            ITEMS.register("steel_plate", () -> new Item(props()));
    public static final RegistryObject<Item> ENERGIZED_STEEL =
            ITEMS.register("energized_steel", () -> new Item(props()));
    public static final RegistryObject<Item> CONTROL_CIRCUIT =
            ITEMS.register("control_circuit", () -> new Item(props()));

    // ──────────── Lote 2: dusts (ore doubling) + componente ────────────
    // r186 — Núcleo de Voo Global (upgrade do Farol de Voo)
    public static final RegistryObject<Item> BEACON_FLIGHT_UPGRADE =
            ITEMS.register("beacon_flight_upgrade", () -> new br.com.murilo.liberthia.item.tech.BeaconFlightUpgradeItem());

    public static final RegistryObject<Item> IRON_DUST = ITEMS.register("iron_dust", () -> new Item(props()));
    public static final RegistryObject<Item> GOLD_DUST = ITEMS.register("gold_dust", () -> new Item(props()));
    public static final RegistryObject<Item> COPPER_DUST = ITEMS.register("copper_dust", () -> new Item(props()));
    public static final RegistryObject<Item> STEEL_DUST = ITEMS.register("steel_dust", () -> new Item(props()));
    public static final RegistryObject<Item> COPPER_COIL = ITEMS.register("copper_coil", () -> new Item(props()));

    public static final RegistryObject<Block> STEEL_BLOCK =
            BLOCKS.register("steel_block", () -> new Block(metal(5.0F)));
    public static final RegistryObject<Item> STEEL_BLOCK_ITEM =
            ITEMS.register("steel_block", () -> new BlockItem(STEEL_BLOCK.get(), props()));

    // ──────────── Lote 3: energia (células FE + geradores) — integra com EnergyNetwork ────────────
    public static final RegistryObject<Block> ENERGY_CELL_BASIC = BLOCKS.register("energy_cell_basic",
            () -> new br.com.murilo.liberthia.block.EnergyCellBlock(metal(4.0F)));
    public static final RegistryObject<Block> ENERGY_CELL_ADVANCED = BLOCKS.register("energy_cell_advanced",
            () -> new br.com.murilo.liberthia.block.EnergyCellBlock(metal(4.5F)));
    public static final RegistryObject<Block> ENERGY_CELL_ULTIMATE = BLOCKS.register("energy_cell_ultimate",
            () -> new br.com.murilo.liberthia.block.EnergyCellBlock(metal(5.0F)));
    public static final RegistryObject<Block> SOLAR_PANEL = BLOCKS.register("solar_panel",
            () -> new br.com.murilo.liberthia.block.TechGeneratorBlock(metal(3.0F).lightLevel(s -> 4)));
    public static final RegistryObject<Block> THERMO_GENERATOR = BLOCKS.register("thermo_generator",
            () -> new br.com.murilo.liberthia.block.TechGeneratorBlock(metal(4.0F).lightLevel(s -> 7)));
    public static final RegistryObject<Item> ENERGY_CELL_BASIC_ITEM = ITEMS.register("energy_cell_basic", () -> new BlockItem(ENERGY_CELL_BASIC.get(), props()));
    public static final RegistryObject<Item> ENERGY_CELL_ADVANCED_ITEM = ITEMS.register("energy_cell_advanced", () -> new BlockItem(ENERGY_CELL_ADVANCED.get(), props()));
    public static final RegistryObject<Item> ENERGY_CELL_ULTIMATE_ITEM = ITEMS.register("energy_cell_ultimate", () -> new BlockItem(ENERGY_CELL_ULTIMATE.get(), props()));
    public static final RegistryObject<Item> SOLAR_PANEL_ITEM = ITEMS.register("solar_panel", () -> new BlockItem(SOLAR_PANEL.get(), props()));
    public static final RegistryObject<Item> THERMO_GENERATOR_ITEM = ITEMS.register("thermo_generator", () -> new BlockItem(THERMO_GENERATOR.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.EnergyCellBlockEntity>> ENERGY_CELL_BE =
            BLOCK_ENTITIES.register("energy_cell", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.EnergyCellBlockEntity::new,
                    ENERGY_CELL_BASIC.get(), ENERGY_CELL_ADVANCED.get(), ENERGY_CELL_ULTIMATE.get()).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.TechGeneratorBlockEntity>> TECH_GENERATOR_BE =
            BLOCK_ENTITIES.register("tech_generator", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.TechGeneratorBlockEntity::new,
                    SOLAR_PANEL.get(), THERMO_GENERATOR.get()).build(null));

    // ──────────── Lote 4: máquinas (Mekanism-style, right-click + FE) ────────────
    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
            () -> new br.com.murilo.liberthia.block.TechMachineBlock(br.com.murilo.liberthia.block.TechMachineBlock.Type.CRUSHER, metal(4.0F)));
    public static final RegistryObject<Block> ENERGIZED_SMELTER = BLOCKS.register("energized_smelter",
            () -> new br.com.murilo.liberthia.block.TechMachineBlock(br.com.murilo.liberthia.block.TechMachineBlock.Type.SMELTER, metal(4.0F).lightLevel(s -> 8)));
    public static final RegistryObject<Block> COMPRESSOR = BLOCKS.register("compressor",
            () -> new br.com.murilo.liberthia.block.TechMachineBlock(br.com.murilo.liberthia.block.TechMachineBlock.Type.COMPRESSOR, metal(4.0F)));
    public static final RegistryObject<Block> ALLOY_SMELTER = BLOCKS.register("alloy_smelter",
            () -> new br.com.murilo.liberthia.block.TechMachineBlock(br.com.murilo.liberthia.block.TechMachineBlock.Type.ALLOY, metal(4.0F).lightLevel(s -> 7)));
    public static final RegistryObject<Block> SAWMILL = BLOCKS.register("sawmill",
            () -> new br.com.murilo.liberthia.block.TechMachineBlock(br.com.murilo.liberthia.block.TechMachineBlock.Type.SAWMILL, metal(3.5F)));
    public static final RegistryObject<Item> CRUSHER_ITEM = ITEMS.register("crusher", () -> new BlockItem(CRUSHER.get(), props()));
    public static final RegistryObject<Item> ENERGIZED_SMELTER_ITEM = ITEMS.register("energized_smelter", () -> new BlockItem(ENERGIZED_SMELTER.get(), props()));
    public static final RegistryObject<Item> COMPRESSOR_ITEM = ITEMS.register("compressor", () -> new BlockItem(COMPRESSOR.get(), props()));
    public static final RegistryObject<Item> ALLOY_SMELTER_ITEM = ITEMS.register("alloy_smelter", () -> new BlockItem(ALLOY_SMELTER.get(), props()));
    public static final RegistryObject<Item> SAWMILL_ITEM = ITEMS.register("sawmill", () -> new BlockItem(SAWMILL.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.TechMachineBlockEntity>> TECH_MACHINE_BE =
            BLOCK_ENTITIES.register("tech_machine", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.TechMachineBlockEntity::new,
                    CRUSHER.get(), ENERGIZED_SMELTER.get(), COMPRESSOR.get(), ALLOY_SMELTER.get(), SAWMILL.get()).build(null));

    // ──────────── Lote 5: ferramentas energizadas (FE-powered) ────────────
    public static final RegistryObject<Item> ENERGIZED_PICKAXE = ITEMS.register("energized_pickaxe", br.com.murilo.liberthia.item.tech.EnergizedPickaxeItem::new);
    public static final RegistryObject<Item> ENERGIZED_AXE = ITEMS.register("energized_axe", br.com.murilo.liberthia.item.tech.EnergizedAxeItem::new);
    public static final RegistryObject<Item> ENERGIZED_SHOVEL = ITEMS.register("energized_shovel", br.com.murilo.liberthia.item.tech.EnergizedShovelItem::new);
    public static final RegistryObject<Item> ENERGIZED_SWORD = ITEMS.register("energized_sword", br.com.murilo.liberthia.item.tech.EnergizedSwordItem::new);
    public static final RegistryObject<Item> ENERGIZED_DRILL = ITEMS.register("energized_drill", br.com.murilo.liberthia.item.tech.EnergizedDrillItem::new);
    public static final RegistryObject<Item> PAXEL = ITEMS.register("paxel", br.com.murilo.liberthia.item.tech.PaxelItem::new);

    // ──────────── Lote 6: power armor (FE absorve dano) ────────────
    public static final RegistryObject<Item> POWER_HELMET = ITEMS.register("power_helmet",
            () -> new br.com.murilo.liberthia.item.tech.PowerArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, 200_000));
    public static final RegistryObject<Item> POWER_CHESTPLATE = ITEMS.register("power_chestplate",
            () -> new br.com.murilo.liberthia.item.tech.PowerArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 400_000));
    public static final RegistryObject<Item> POWER_LEGGINGS = ITEMS.register("power_leggings",
            () -> new br.com.murilo.liberthia.item.tech.PowerArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 300_000));
    public static final RegistryObject<Item> POWER_BOOTS = ITEMS.register("power_boots",
            () -> new br.com.murilo.liberthia.item.tech.PowerArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, 200_000));

    // ──────────── Lote 7: utilidade (FE) ────────────
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery", br.com.murilo.liberthia.item.tech.BatteryItem::new);
    public static final RegistryObject<Item> ITEM_MAGNET = ITEMS.register("item_magnet", br.com.murilo.liberthia.item.tech.ItemMagnetItem::new);
    public static final RegistryObject<Item> TECH_WRENCH = ITEMS.register("tech_wrench", br.com.murilo.liberthia.item.tech.TechWrenchItem::new);
    public static final RegistryObject<Item> JETPACK = ITEMS.register("jetpack", br.com.murilo.liberthia.item.tech.JetpackItem::new);
    public static final RegistryObject<Block> CHARGER = BLOCKS.register("charger",
            () -> new br.com.murilo.liberthia.block.ChargerBlock(metal(4.0F).lightLevel(s -> 6)));
    public static final RegistryObject<Item> CHARGER_ITEM = ITEMS.register("charger", () -> new BlockItem(CHARGER.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.ChargerBlockEntity>> CHARGER_BE =
            BLOCK_ENTITIES.register("charger", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.ChargerBlockEntity::new, CHARGER.get()).build(null));

    // ──────────── Integração: Reator de Matéria Escura (centerpiece animado) ────────────
    public static final RegistryObject<Block> MATTER_REACTOR = BLOCKS.register("matter_reactor",
            () -> new br.com.murilo.liberthia.block.MatterReactorBlock(metal(5.0F)
                    .lightLevel(s -> s.getValue(br.com.murilo.liberthia.block.MatterReactorBlock.LIT) ? 12 : 3)));
    public static final RegistryObject<Item> MATTER_REACTOR_ITEM = ITEMS.register("matter_reactor", () -> new BlockItem(MATTER_REACTOR.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.MatterReactorBlockEntity>> MATTER_REACTOR_BE =
            BLOCK_ENTITIES.register("matter_reactor", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.MatterReactorBlockEntity::new, MATTER_REACTOR.get()).build(null));

    // ──────────── Lote 8 final: componentes + geradores de combustível + portable charger ────────────
    public static final RegistryObject<Item> REINFORCED_PLATE = ITEMS.register("reinforced_plate", () -> new Item(props()));
    public static final RegistryObject<Item> ENERGIZED_CIRCUIT = ITEMS.register("energized_circuit", () -> new Item(props()));
    public static final RegistryObject<Item> PORTABLE_CHARGER = ITEMS.register("portable_charger", br.com.murilo.liberthia.item.tech.PortableChargerItem::new);
    public static final RegistryObject<Block> FURNATOR = BLOCKS.register("furnator",
            () -> new br.com.murilo.liberthia.block.FuelGeneratorBlock(metal(4.0F)
                    .lightLevel(s -> s.getValue(br.com.murilo.liberthia.block.FuelGeneratorBlock.LIT) ? 13 : 0)));
    public static final RegistryObject<Block> MAGMATOR = BLOCKS.register("magmator",
            () -> new br.com.murilo.liberthia.block.FuelGeneratorBlock(metal(4.0F)
                    .lightLevel(s -> s.getValue(br.com.murilo.liberthia.block.FuelGeneratorBlock.LIT) ? 13 : 3)));
    public static final RegistryObject<Item> FURNATOR_ITEM = ITEMS.register("furnator", () -> new BlockItem(FURNATOR.get(), props()));
    public static final RegistryObject<Item> MAGMATOR_ITEM = ITEMS.register("magmator", () -> new BlockItem(MAGMATOR.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.FuelGeneratorBlockEntity>> FUEL_GENERATOR_BE =
            BLOCK_ENTITIES.register("fuel_generator", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.FuelGeneratorBlockEntity::new, FURNATOR.get(), MAGMATOR.get()).build(null));

    // ════════════ TECNO-ARCANO (anti-magia, FE) — tier: um puxa o outro ════════════
    public static final RegistryObject<Item> WARDED_CORE = ITEMS.register("warded_core", () -> new Item(props()));            // T1
    public static final RegistryObject<Item> ARCANE_DISRUPTOR = ITEMS.register("arcane_disruptor", () -> new Item(props()));  // T2
    public static final RegistryObject<Item> NULL_MATRIX = ITEMS.register("null_matrix", () -> new Item(props()));            // T3
    // Armadura Bastião (FE)
    public static final RegistryObject<Item> WARDED_HELMET = ITEMS.register("warded_helmet",
            () -> new br.com.murilo.liberthia.item.tech.WardedArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, 200_000));
    public static final RegistryObject<Item> WARDED_CHESTPLATE = ITEMS.register("warded_chestplate",
            () -> new br.com.murilo.liberthia.item.tech.WardedArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 400_000));
    public static final RegistryObject<Item> WARDED_LEGGINGS = ITEMS.register("warded_leggings",
            () -> new br.com.murilo.liberthia.item.tech.WardedArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 300_000));
    public static final RegistryObject<Item> WARDED_BOOTS = ITEMS.register("warded_boots",
            () -> new br.com.murilo.liberthia.item.tech.WardedArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, 200_000));
    public static final RegistryObject<Item> WARDED_SHIELD = ITEMS.register("warded_shield",
            br.com.murilo.liberthia.item.tech.WardedShieldItem::new);
    // Selos (blocos consumidores de FE)
    public static final RegistryObject<Block> MANA_SUPPRESSOR = BLOCKS.register("mana_suppressor",
            () -> new br.com.murilo.liberthia.block.ManaSuppressorBlock(metal(4.0F)
                    .lightLevel(s -> s.getValue(br.com.murilo.liberthia.block.ManaSuppressorBlock.LIT) ? 10 : 2)));
    public static final RegistryObject<Block> ARCANE_SENTINEL = BLOCKS.register("arcane_sentinel",
            () -> new br.com.murilo.liberthia.block.ArcaneSentinelBlock(metal(4.0F)
                    .lightLevel(s -> s.getValue(br.com.murilo.liberthia.block.ArcaneSentinelBlock.LIT) ? 12 : 2)));
    public static final RegistryObject<Item> MANA_SUPPRESSOR_ITEM = ITEMS.register("mana_suppressor", () -> new BlockItem(MANA_SUPPRESSOR.get(), props()));
    public static final RegistryObject<Item> ARCANE_SENTINEL_ITEM = ITEMS.register("arcane_sentinel", () -> new BlockItem(ARCANE_SENTINEL.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.ManaSuppressorBlockEntity>> MANA_SUPPRESSOR_BE =
            BLOCK_ENTITIES.register("mana_suppressor", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.ManaSuppressorBlockEntity::new, MANA_SUPPRESSOR.get()).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.ArcaneSentinelBlockEntity>> ARCANE_SENTINEL_BE =
            BLOCK_ENTITIES.register("arcane_sentinel", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.ArcaneSentinelBlockEntity::new, ARCANE_SENTINEL.get()).build(null));

    // ════════════ Motores de Entropia / Matéria Escura (espalham + revert por JSON) ════════════
    public static final RegistryObject<Block> CRYSTALIZED_DARK_MATTER = BLOCKS.register("crystalized_dark_matter",
            () -> new Block(metal(4.0F).lightLevel(s -> 6)));
    public static final RegistryObject<Item> CRYSTALIZED_DARK_MATTER_ITEM = ITEMS.register("crystalized_dark_matter", () -> new BlockItem(CRYSTALIZED_DARK_MATTER.get(), props()));
    public static final RegistryObject<Block> ENTROPY_CORE = BLOCKS.register("entropy_core",
            () -> new br.com.murilo.liberthia.block.SpreaderEngineBlock(metal(6.0F).lightLevel(s -> 7)));
    public static final RegistryObject<Block> BLACK_MATTER_ENGINE = BLOCKS.register("black_matter_engine",
            () -> new br.com.murilo.liberthia.block.SpreaderEngineBlock(metal(6.0F).lightLevel(s -> 4)));
    public static final RegistryObject<Item> ENTROPY_CORE_ITEM = ITEMS.register("entropy_core", () -> new BlockItem(ENTROPY_CORE.get(), props()));
    public static final RegistryObject<Item> BLACK_MATTER_ENGINE_ITEM = ITEMS.register("black_matter_engine", () -> new BlockItem(BLACK_MATTER_ENGINE.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.SpreaderEngineBlockEntity>> SPREADER_ENGINE_BE =
            BLOCK_ENTITIES.register("spreader_engine", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.SpreaderEngineBlockEntity::new, ENTROPY_CORE.get(), BLACK_MATTER_ENGINE.get()).build(null));

    // ════════════ Framework de máquinas Arcanas (onda 1: cadeia multi-etapa) ════════════
    // intermediários (feitos POR máquina, não no craft)
    public static final RegistryObject<Item> HARDENED_PLATE = ITEMS.register("hardened_plate", () -> new Item(props()));
    public static final RegistryObject<Item> WARDED_PLATE = ITEMS.register("warded_plate", () -> new Item(props()));
    public static final RegistryObject<Item> MANA_CAPACITOR = ITEMS.register("mana_capacitor", () -> new Item(props()));
    public static final RegistryObject<Item> ARCANE_ALLOY = ITEMS.register("arcane_alloy", () -> new Item(props()));
    public static final RegistryObject<Item> WARDED_MODULE = ITEMS.register("warded_module", () -> new Item(props()));
    // máquinas (ArcaneMachineBlock por Type)
    private static RegistryObject<Block> machine(String id, br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType t, int light) {
        RegistryObject<Block> b = BLOCKS.register(id,
                () -> new br.com.murilo.liberthia.block.ArcaneMachineBlock(t, metal(4.0F).lightLevel(s -> light)));
        ITEMS.register(id, () -> new BlockItem(b.get(), props()));
        return b;
    }
    public static final RegistryObject<Block> METAL_PRESS = machine("metal_press", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.METAL_PRESS, 4);
    public static final RegistryObject<Block> ARCANE_INFUSER = machine("arcane_infuser", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.ARCANE_INFUSER, 8);
    public static final RegistryObject<Block> TECH_ASSEMBLER = machine("tech_assembler", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.TECH_ASSEMBLER, 6);
    public static final RegistryObject<Block> MANA_CONDENSER = machine("mana_condenser", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.MANA_CONDENSER, 7);
    public static final RegistryObject<Block> CRYSTAL_SMELTER = machine("crystal_smelter", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.CRYSTAL_SMELTER, 8);

    // r188 — 8 itens intermediários da cadeia avançada
    public static final RegistryObject<Item> QUANTUM_DUST = ITEMS.register("quantum_dust", () -> new Item(props()));
    public static final RegistryObject<Item> PHOTON_CRYSTAL = ITEMS.register("photon_crystal", () -> new Item(props()));
    public static final RegistryObject<Item> RESONANCE_COIL = ITEMS.register("resonance_coil", () -> new Item(props()));
    public static final RegistryObject<Item> DARK_MATTER_CELL_CORE = ITEMS.register("dark_matter_cell_core", () -> new Item(props()));
    public static final RegistryObject<Item> ASSEMBLED_MATRIX = ITEMS.register("assembled_matrix", () -> new Item(props()));
    public static final RegistryObject<Item> ENERGY_ESSENCE = ITEMS.register("energy_essence", () -> new Item(props()));
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal", () -> new Item(props()));
    public static final RegistryObject<Item> VOID_ALLOY_INGOT = ITEMS.register("void_alloy_ingot", () -> new Item(props()));

    // r188 — 16 máquinas novas (UI genérica ArcaneMachine)
    private static final String AMT = "br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType";
    public static final RegistryObject<Block> QUANTUM_PULVERIZER = machine("quantum_pulverizer", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.QUANTUM_PULVERIZER, 6);
    public static final RegistryObject<Block> CRYSTALLIZATION_CHAMBER = machine("crystallization_chamber", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.CRYSTALLIZATION_CHAMBER, 9);
    public static final RegistryObject<Block> COIL_WINDER = machine("coil_winder", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.COIL_WINDER, 5);
    public static final RegistryObject<Block> MATTER_CONDENSER_T = machine("matter_condenser_t", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.MATTER_CONDENSER_T, 6);
    public static final RegistryObject<Block> NANO_ASSEMBLER = machine("nano_assembler", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.NANO_ASSEMBLER, 10);
    public static final RegistryObject<Block> ENERGY_DISTILLER = machine("energy_distiller", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.ENERGY_DISTILLER, 7);
    public static final RegistryObject<Block> FLUX_FORGE = machine("flux_forge", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.FLUX_FORGE, 8);
    public static final RegistryObject<Block> MANA_CRYSTALLIZER = machine("mana_crystallizer", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.MANA_CRYSTALLIZER, 8);
    public static final RegistryObject<Block> ARCANE_CIRCUIT_PRINTER = machine("arcane_circuit_printer", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.ARCANE_CIRCUIT_PRINTER, 7);
    public static final RegistryObject<Block> DARK_ALLOY_SMELTER = machine("dark_alloy_smelter", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.DARK_ALLOY_SMELTER, 6);
    public static final RegistryObject<Block> PHOTON_INFUSER = machine("photon_infuser", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.PHOTON_INFUSER, 10);
    public static final RegistryObject<Block> MATTER_REPLICATOR = machine("matter_replicator", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.MATTER_REPLICATOR, 9);
    public static final RegistryObject<Block> CRYSTAL_GROWER = machine("crystal_grower", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.CRYSTAL_GROWER, 8);
    public static final RegistryObject<Block> ESSENCE_COMPRESSOR = machine("essence_compressor", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.ESSENCE_COMPRESSOR, 7);
    public static final RegistryObject<Block> RUNE_ETCHER = machine("rune_etcher", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.RUNE_ETCHER, 7);
    public static final RegistryObject<Block> SINGULARITY_PRESS = machine("singularity_press", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.SINGULARITY_PRESS, 12);

    // r189 — +18 máquinas (fecha 40 blocos com UI)
    public static final RegistryObject<Block> ARCANE_COLLECTOR = machine("arcane_collector", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.ARCANE_COLLECTOR, 8);
    public static final RegistryObject<Block> MANA_REACTOR = machine("mana_reactor", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.MANA_REACTOR, 8);
    public static final RegistryObject<Block> CRYSTAL_RESONATOR = machine("crystal_resonator", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.CRYSTAL_RESONATOR, 8);
    public static final RegistryObject<Block> ENDER_CONDENSER = machine("ender_condenser", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.ENDER_CONDENSER, 7);
    public static final RegistryObject<Block> SOUL_EXTRACTOR = machine("soul_extractor", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.SOUL_EXTRACTOR, 6);
    public static final RegistryObject<Block> BLAZE_REACTOR = machine("blaze_reactor", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.BLAZE_REACTOR, 9);
    public static final RegistryObject<Block> MATTER_FABRICATOR = machine("matter_fabricator", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.MATTER_FABRICATOR, 9);
    public static final RegistryObject<Block> CIRCUIT_ASSEMBLER = machine("circuit_assembler", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.CIRCUIT_ASSEMBLER, 6);
    public static final RegistryObject<Block> PLATE_PRESS = machine("plate_press", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.PLATE_PRESS, 4);
    public static final RegistryObject<Block> WIRE_DRAWER = machine("wire_drawer", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.WIRE_DRAWER, 4);
    public static final RegistryObject<Block> GEM_POLISHER = machine("gem_polisher", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.GEM_POLISHER, 8);
    public static final RegistryObject<Block> INGOT_FORMER = machine("ingot_former", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.INGOT_FORMER, 6);
    public static final RegistryObject<Block> ARCANE_SYNTHESIZER = machine("arcane_synthesizer", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.ARCANE_SYNTHESIZER, 10);
    public static final RegistryObject<Block> FLUX_DYNAMO = machine("flux_dynamo", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.FLUX_DYNAMO, 9);
    public static final RegistryObject<Block> SHARD_SPLITTER = machine("shard_splitter", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.SHARD_SPLITTER, 6);
    public static final RegistryObject<Block> COSMIC_DISTILLER = machine("cosmic_distiller", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.COSMIC_DISTILLER, 9);
    public static final RegistryObject<Block> RUNE_INSCRIBER = machine("rune_inscriber", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.RUNE_INSCRIBER, 7);
    public static final RegistryObject<Block> SINGULARITY_CORE_FORGE = machine("singularity_core_forge", br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType.SINGULARITY_CORE_FORGE, 12);

    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineBlockEntity>> ARCANE_MACHINE_BE =
            BLOCK_ENTITIES.register("arcane_machine", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineBlockEntity::new,
                    METAL_PRESS.get(), ARCANE_INFUSER.get(), TECH_ASSEMBLER.get(), MANA_CONDENSER.get(), CRYSTAL_SMELTER.get(),
                    QUANTUM_PULVERIZER.get(), CRYSTALLIZATION_CHAMBER.get(), COIL_WINDER.get(), MATTER_CONDENSER_T.get(),
                    NANO_ASSEMBLER.get(), ENERGY_DISTILLER.get(), FLUX_FORGE.get(), MANA_CRYSTALLIZER.get(),
                    ARCANE_CIRCUIT_PRINTER.get(), DARK_ALLOY_SMELTER.get(), PHOTON_INFUSER.get(), MATTER_REPLICATOR.get(),
                    CRYSTAL_GROWER.get(), ESSENCE_COMPRESSOR.get(), RUNE_ETCHER.get(), SINGULARITY_PRESS.get(),
                    ARCANE_COLLECTOR.get(), MANA_REACTOR.get(), CRYSTAL_RESONATOR.get(), ENDER_CONDENSER.get(),
                    SOUL_EXTRACTOR.get(), BLAZE_REACTOR.get(), MATTER_FABRICATOR.get(), CIRCUIT_ASSEMBLER.get(),
                    PLATE_PRESS.get(), WIRE_DRAWER.get(), GEM_POLISHER.get(), INGOT_FORMER.get(),
                    ARCANE_SYNTHESIZER.get(), FLUX_DYNAMO.get(), SHARD_SPLITTER.get(), COSMIC_DISTILLER.get(),
                    RUNE_INSCRIBER.get(), SINGULARITY_CORE_FORGE.get()).build(null));

    // ════════════ Onda 2: blocos de energia/anti-magia com efeito (FE consumer + GUI) ════════════
    public static final RegistryObject<Block> FLIGHT_BEACON = BLOCKS.register("flight_beacon",
            () -> new br.com.murilo.liberthia.block.FlightBeaconBlock(metal(4.0F).lightLevel(s -> s.getValue(br.com.murilo.liberthia.block.FlightBeaconBlock.LIT) ? 14 : 4)));
    public static final RegistryObject<Item> FLIGHT_BEACON_ITEM = ITEMS.register("flight_beacon", () -> new BlockItem(FLIGHT_BEACON.get(), props()));
    public static final RegistryObject<Block> ARCANE_TURRET = BLOCKS.register("arcane_turret",
            () -> new br.com.murilo.liberthia.block.ArcaneTurretBlock(metal(4.5F).lightLevel(s -> s.getValue(br.com.murilo.liberthia.block.ArcaneTurretBlock.LIT) ? 8 : 2)));
    public static final RegistryObject<Item> ARCANE_TURRET_ITEM = ITEMS.register("arcane_turret", () -> new BlockItem(ARCANE_TURRET.get(), props()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.FlightBeaconBlockEntity>> FLIGHT_BEACON_BE =
            BLOCK_ENTITIES.register("flight_beacon", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.FlightBeaconBlockEntity::new, FLIGHT_BEACON.get()).build(null));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<br.com.murilo.liberthia.block.entity.ArcaneTurretBlockEntity>> ARCANE_TURRET_BE =
            BLOCK_ENTITIES.register("arcane_turret", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    br.com.murilo.liberthia.block.entity.ArcaneTurretBlockEntity::new, ARCANE_TURRET.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
