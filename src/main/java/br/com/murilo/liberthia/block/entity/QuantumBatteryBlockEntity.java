package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class QuantumBatteryBlockEntity extends DarkMatterBatteryBlockEntity {
    public QuantumBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_QUANTUM.get(), pos, state, Tier.QUANTUM);
    }
}
