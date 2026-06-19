package br.com.murilo.liberthia.block.entity.arcane;

import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * r184 — receitas em código (não data-driven) das máquinas Arcanas, por {@link ArcaneMachineType}.
 * Bootstrap PREGUIÇOSO (1ª chamada de find, pós-registro) p/ os RegistryObjects já estarem prontos.
 * Cadeias multi-etapa: o output de uma máquina é input da próxima.
 */
public final class ArcaneRecipeRegistry {
    private ArcaneRecipeRegistry() {}

    private static final Map<ArcaneMachineType, List<ArcaneRecipe>> RECIPES = new EnumMap<>(ArcaneMachineType.class);
    private static boolean booted = false;

    public static void add(ArcaneMachineType t, List<ItemStack> in, List<ItemStack> out, int time) {
        RECIPES.computeIfAbsent(t, k -> new ArrayList<>()).add(new ArcaneRecipe(in, out, time));
    }

    @Nullable
    public static ArcaneRecipe find(ArcaneMachineType type, ItemStackHandler inv) {
        if (!booted) bootstrap();
        List<ArcaneRecipe> list = RECIPES.get(type);
        if (list == null) return null;
        for (ArcaneRecipe r : list) if (r.matches(inv, type.inputSlots, type.outputSlots)) return r;
        return null;
    }

    private static ItemStack S(net.minecraft.world.level.ItemLike it, int n) { return new ItemStack(it, n); }

    private static synchronized void bootstrap() {
        if (booted) return;
        booted = true;
        // ── Cadeia da Armadura Bastião (difícil, multi-etapa) ──
        add(ArcaneMachineType.METAL_PRESS,
                List.of(S(ModTech.STEEL_INGOT.get(), 2)),
                List.of(S(ModTech.HARDENED_PLATE.get(), 1)), 0);
        add(ArcaneMachineType.ARCANE_INFUSER,
                List.of(S(ModTech.HARDENED_PLATE.get(), 1), S(ModItems.DARK_MATTER_SHARD.get(), 1)),
                List.of(S(ModTech.WARDED_PLATE.get(), 1)), 0);
        add(ArcaneMachineType.TECH_ASSEMBLER,
                List.of(S(ModTech.WARDED_PLATE.get(), 1), S(ModTech.ARCANE_ALLOY.get(), 1), S(ModTech.CONTROL_CIRCUIT.get(), 1)),
                List.of(S(ModTech.WARDED_MODULE.get(), 1)), 0);
        // ── Coleta de magia → energia/liga ──
        add(ArcaneMachineType.MANA_CONDENSER,
                List.of(S(Items.AMETHYST_SHARD, 2)),
                List.of(S(ModTech.MANA_CAPACITOR.get(), 1)), 0);
        add(ArcaneMachineType.CRYSTAL_SMELTER,
                List.of(S(ModTech.MANA_CAPACITOR.get(), 1), S(ModItems.DARK_MATTER_INGOT.get(), 1)),
                List.of(S(ModTech.ARCANE_ALLOY.get(), 1)), 0);

        // ── r188: cadeia avançada (16 máquinas novas) ──
        add(ArcaneMachineType.QUANTUM_PULVERIZER,
                List.of(S(ModTech.ENERGIZED_STEEL.get(), 1)), List.of(S(ModTech.QUANTUM_DUST.get(), 2)), 0);
        add(ArcaneMachineType.CRYSTALLIZATION_CHAMBER,
                List.of(S(ModTech.QUANTUM_DUST.get(), 1)), List.of(S(ModTech.PHOTON_CRYSTAL.get(), 1)), 0);
        add(ArcaneMachineType.COIL_WINDER,
                List.of(S(ModTech.COPPER_COIL.get(), 1), S(ModTech.PHOTON_CRYSTAL.get(), 1)), List.of(S(ModTech.RESONANCE_COIL.get(), 1)), 0);
        add(ArcaneMachineType.MATTER_CONDENSER_T,
                List.of(S(ModItems.DARK_MATTER_SHARD.get(), 4), S(ModTech.QUANTUM_DUST.get(), 2)), List.of(S(ModTech.DARK_MATTER_CELL_CORE.get(), 1)), 0);
        add(ArcaneMachineType.NANO_ASSEMBLER,
                List.of(S(ModTech.RESONANCE_COIL.get(), 1), S(ModTech.DARK_MATTER_CELL_CORE.get(), 1)), List.of(S(ModTech.ASSEMBLED_MATRIX.get(), 1)), 0);
        add(ArcaneMachineType.ENERGY_DISTILLER,
                List.of(S(Items.REDSTONE_BLOCK, 1)), List.of(S(ModTech.ENERGY_ESSENCE.get(), 2)), 0);
        add(ArcaneMachineType.FLUX_FORGE,
                List.of(S(ModTech.STEEL_PLATE.get(), 1), S(ModTech.ENERGY_ESSENCE.get(), 1)), List.of(S(ModTech.VOID_ALLOY_INGOT.get(), 1)), 0);
        add(ArcaneMachineType.MANA_CRYSTALLIZER,
                List.of(S(Items.AMETHYST_SHARD, 1)), List.of(S(ModTech.MANA_CRYSTAL.get(), 1)), 0);
        add(ArcaneMachineType.ARCANE_CIRCUIT_PRINTER,
                List.of(S(ModTech.MANA_CRYSTAL.get(), 1), S(ModTech.GOLD_DUST.get(), 1)), List.of(S(ModTech.ENERGIZED_CIRCUIT.get(), 1)), 0);
        add(ArcaneMachineType.DARK_ALLOY_SMELTER,
                List.of(S(ModTech.VOID_ALLOY_INGOT.get(), 1), S(ModItems.DARK_MATTER_INGOT.get(), 1)), List.of(S(ModTech.ARCANE_ALLOY.get(), 1)), 0);
        add(ArcaneMachineType.PHOTON_INFUSER,
                List.of(S(ModTech.PHOTON_CRYSTAL.get(), 1), S(ModTech.ENERGIZED_STEEL.get(), 1)), List.of(S(ModTech.ENERGY_ESSENCE.get(), 3)), 0);
        add(ArcaneMachineType.MATTER_REPLICATOR,
                List.of(S(ModItems.DARK_MATTER_INGOT.get(), 1)), List.of(S(ModItems.DARK_MATTER_INGOT.get(), 2)), 0);
        add(ArcaneMachineType.CRYSTAL_GROWER,
                List.of(S(Items.AMETHYST_SHARD, 1)), List.of(S(ModTech.PHOTON_CRYSTAL.get(), 1)), 0);
        add(ArcaneMachineType.ESSENCE_COMPRESSOR,
                List.of(S(ModTech.ENERGY_ESSENCE.get(), 4)), List.of(S(ModTech.ENERGIZED_STEEL.get(), 2)), 0);
        add(ArcaneMachineType.RUNE_ETCHER,
                List.of(S(ModTech.MANA_CRYSTAL.get(), 1), S(ModTech.STEEL_PLATE.get(), 1)), List.of(S(ModTech.WARDED_MODULE.get(), 1)), 0);
        add(ArcaneMachineType.SINGULARITY_PRESS,
                List.of(S(ModTech.DARK_MATTER_CELL_CORE.get(), 1), S(ModTech.ASSEMBLED_MATRIX.get(), 1)), List.of(S(ModTech.CRYSTALIZED_DARK_MATTER_ITEM.get(), 1)), 0);

        // ── r189: +18 máquinas (fecha os 40) ──
        add(ArcaneMachineType.ARCANE_COLLECTOR, List.of(S(Items.AMETHYST_CLUSTER, 1)), List.of(S(ModTech.MANA_CRYSTAL.get(), 4)), 0);
        add(ArcaneMachineType.MANA_REACTOR, List.of(S(ModTech.MANA_CRYSTAL.get(), 1), S(Items.REDSTONE, 1)), List.of(S(ModTech.ENERGY_ESSENCE.get(), 2)), 0);
        add(ArcaneMachineType.CRYSTAL_RESONATOR, List.of(S(ModTech.PHOTON_CRYSTAL.get(), 1)), List.of(S(ModTech.MANA_CRYSTAL.get(), 2)), 0);
        add(ArcaneMachineType.ENDER_CONDENSER, List.of(S(Items.ENDER_PEARL, 1)), List.of(S(ModTech.ENERGY_ESSENCE.get(), 3)), 0);
        add(ArcaneMachineType.SOUL_EXTRACTOR, List.of(S(Items.SOUL_SAND, 1)), List.of(S(ModTech.ENERGY_ESSENCE.get(), 1)), 0);
        add(ArcaneMachineType.BLAZE_REACTOR, List.of(S(Items.BLAZE_ROD, 1)), List.of(S(ModTech.ENERGY_ESSENCE.get(), 4)), 0);
        add(ArcaneMachineType.MATTER_FABRICATOR, List.of(S(ModTech.ENERGY_ESSENCE.get(), 8), S(ModItems.DARK_MATTER_SHARD.get(), 1)), List.of(S(ModItems.DARK_MATTER_INGOT.get(), 1)), 0);
        add(ArcaneMachineType.CIRCUIT_ASSEMBLER, List.of(S(ModTech.GOLD_DUST.get(), 1), S(Items.REDSTONE, 1)), List.of(S(ModTech.CONTROL_CIRCUIT.get(), 1)), 0);
        add(ArcaneMachineType.PLATE_PRESS, List.of(S(ModTech.STEEL_INGOT.get(), 1)), List.of(S(ModTech.STEEL_PLATE.get(), 1)), 0);
        add(ArcaneMachineType.WIRE_DRAWER, List.of(S(ModTech.COPPER_DUST.get(), 1)), List.of(S(ModTech.COPPER_COIL.get(), 1)), 0);
        add(ArcaneMachineType.GEM_POLISHER, List.of(S(Items.DIAMOND, 1)), List.of(S(ModTech.PHOTON_CRYSTAL.get(), 2)), 0);
        add(ArcaneMachineType.INGOT_FORMER, List.of(S(ModTech.QUANTUM_DUST.get(), 2)), List.of(S(ModTech.VOID_ALLOY_INGOT.get(), 1)), 0);
        add(ArcaneMachineType.ARCANE_SYNTHESIZER, List.of(S(ModTech.MANA_CRYSTAL.get(), 1), S(ModTech.PHOTON_CRYSTAL.get(), 1), S(ModTech.ENERGY_ESSENCE.get(), 1)), List.of(S(ModTech.ASSEMBLED_MATRIX.get(), 1)), 0);
        add(ArcaneMachineType.FLUX_DYNAMO, List.of(S(ModTech.RESONANCE_COIL.get(), 1), S(ModTech.ENERGY_ESSENCE.get(), 1)), List.of(S(ModTech.ENERGIZED_CIRCUIT.get(), 1)), 0);
        add(ArcaneMachineType.SHARD_SPLITTER, List.of(S(ModItems.DARK_MATTER_INGOT.get(), 1)), List.of(S(ModItems.DARK_MATTER_SHARD.get(), 9)), 0);
        add(ArcaneMachineType.COSMIC_DISTILLER, List.of(S(ModTech.CRYSTALIZED_DARK_MATTER_ITEM.get(), 1), S(ModTech.ENERGY_ESSENCE.get(), 1)), List.of(S(ModTech.DARK_MATTER_CELL_CORE.get(), 1)), 0);
        add(ArcaneMachineType.RUNE_INSCRIBER, List.of(S(ModTech.MANA_CRYSTAL.get(), 1), S(ModTech.STEEL_PLATE.get(), 1)), List.of(S(ModTech.WARDED_PLATE.get(), 1)), 0);
        add(ArcaneMachineType.SINGULARITY_CORE_FORGE, List.of(S(ModTech.DARK_MATTER_CELL_CORE.get(), 2), S(ModTech.ASSEMBLED_MATRIX.get(), 1)), List.of(S(ModTech.CRYSTALIZED_DARK_MATTER_ITEM.get(), 1)), 0);
    }
}
