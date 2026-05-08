package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BasicBatteryBlockEntity extends DarkMatterBatteryBlockEntity {
    public BasicBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_BASIC.get(), pos, state, Tier.BASIC);
    }
}
