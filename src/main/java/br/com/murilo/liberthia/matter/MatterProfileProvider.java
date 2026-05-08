package br.com.murilo.liberthia.matter;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider que anexa {@link MatterProfile} a um Player. Tem que persistir
 * (implementa ICapabilitySerializable).
 */
public class MatterProfileProvider implements ICapabilitySerializable<Tag> {

    public static final Capability<MatterProfile> CAP =
            net.minecraftforge.common.capabilities.CapabilityManager.get(new CapabilityToken<>() {});

    private final MatterProfile profile = new MatterProfile();
    private final LazyOptional<MatterProfile> lazy = LazyOptional.of(() -> profile);

    @NotNull @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CAP ? lazy.cast() : LazyOptional.empty();
    }

    @Override
    public Tag serializeNBT() {
        return profile.serializeNBT();
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        if (nbt instanceof CompoundTag ct) profile.deserializeNBT(ct);
    }

    public void invalidate() { lazy.invalidate(); }
}
