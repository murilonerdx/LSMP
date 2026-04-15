package br.com.murilo.liberthia.capability;

import net.minecraft.nbt.CompoundTag;

public interface IInfectionData {
    int getInfection();
    void setInfection(int infection);
    void addInfection(int amount);
    void reduceInfection(int amount);

    int getPermanentHealthPenalty();
    void setPermanentHealthPenalty(int amount);
    void reducePermanentHealthPenalty(int amount);

    int getMaxInfectionReached();
    void setMaxInfectionReached(int amount);

    int getPillTimer();
    void setPillTimer(int ticks);
    boolean canTakePills();

    int getStage();
    boolean isImmune();
    void setImmune(boolean immune);

    String getMutations();
    void setMutations(String mutations);
    boolean hasMutation(String mutationId);
    void addMutation(String mutationId);
    void removeMutation(String mutationId);

    int getSymbiosisTimer();
    void setSymbiosisTimer(int ticks);

    boolean isDirty();
    void setDirty(boolean dirty);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);
}
