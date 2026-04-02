package br.com.murilo.liberthia.capability;

import net.minecraft.nbt.CompoundTag;

public class MatterEnergyData implements IMatterEnergy {
    private static final int MAX_ENERGY = 1000;

    private int darkEnergy = 0;
    private int clearEnergy = 0;
    private int yellowEnergy = 0;
    private boolean stabilized = false;
    private boolean dirty = false;

    @Override
    public int getDarkEnergy() { return darkEnergy; }

    @Override
    public void setDarkEnergy(int value) {
        darkEnergy = Math.max(0, Math.min(value, MAX_ENERGY));
        dirty = true;
    }

    @Override
    public void addDarkEnergy(int amount) {
        setDarkEnergy(darkEnergy + amount);
    }

    @Override
    public int getClearEnergy() { return clearEnergy; }

    @Override
    public void setClearEnergy(int value) {
        clearEnergy = Math.max(0, Math.min(value, MAX_ENERGY));
        dirty = true;
    }

    @Override
    public void addClearEnergy(int amount) {
        setClearEnergy(clearEnergy + amount);
    }

    @Override
    public int getYellowEnergy() { return yellowEnergy; }

    @Override
    public void setYellowEnergy(int value) {
        yellowEnergy = Math.max(0, Math.min(value, MAX_ENERGY));
        dirty = true;
    }

    @Override
    public void addYellowEnergy(int amount) {
        setYellowEnergy(yellowEnergy + amount);
    }

    @Override
    public int getMaxEnergy() { return MAX_ENERGY; }

    @Override
    public boolean isStabilized() { return stabilized; }

    @Override
    public void setStabilized(boolean stabilized) {
        this.stabilized = stabilized;
        dirty = true;
    }

    @Override
    public void decay() {
        if (!stabilized) {
            if (darkEnergy > 0) setDarkEnergy(darkEnergy - 1);
            if (clearEnergy > 0) setClearEnergy(clearEnergy - 1);
            if (yellowEnergy > 0) setYellowEnergy(yellowEnergy - 1);
        }
    }

    @Override
    public boolean isDirty() { return dirty; }

    @Override
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("darkEnergy", darkEnergy);
        tag.putInt("clearEnergy", clearEnergy);
        tag.putInt("yellowEnergy", yellowEnergy);
        tag.putBoolean("stabilized", stabilized);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        darkEnergy = tag.getInt("darkEnergy");
        clearEnergy = tag.getInt("clearEnergy");
        yellowEnergy = tag.getInt("yellowEnergy");
        stabilized = tag.getBoolean("stabilized");
    }
}
