package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AdvancedBatteryBlockEntity extends DarkMatterBatteryBlockEntity {
    public AdvancedBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_ADVANCED.get(), pos, state, Tier.ADVANCED);
    }
}
