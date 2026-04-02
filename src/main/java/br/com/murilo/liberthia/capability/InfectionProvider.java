package br.com.murilo.liberthia.capability;

import br.com.murilo.liberthia.registry.ModCapabilities;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class InfectionProvider implements ICapabilitySerializable<CompoundTag> {
    private final InfectionData data = new InfectionData();
    private final LazyOptional<IInfectionData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        return ModCapabilities.INFECTION.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }
}
