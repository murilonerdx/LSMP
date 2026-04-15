package br.com.murilo.liberthia.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class InfectionData implements IInfectionData {
    private int infection;
    private int permanentHealthPenalty;
    private int maxInfectionReached;
    private int pillEffectTimer;
    private boolean immune;
    private String mutationsCount = "";
    private int symbiosisTimer;
    private boolean dirty = true;

    @Override
    public int getInfection() {
        return infection;
    }

    @Override
    public void setInfection(int infection) {
        if (immune) {
            this.infection = 0;
            return;
        }
        int clamped = Mth.clamp(infection, 0, 100);
        if (this.infection != clamped) {
            this.infection = clamped;
            this.maxInfectionReached = Math.max(this.maxInfectionReached, clamped);
            this.dirty = true;
        }
    }

    @Override
    public void addInfection(int amount) {
        setInfection(this.infection + amount);
    }

    @Override
    public void reduceInfection(int amount) {
        setInfection(this.infection - amount);
    }

    @Override
    public int getPermanentHealthPenalty() {
        return permanentHealthPenalty;
    }

    @Override
    public void setPermanentHealthPenalty(int amount) {
        if (immune) {
            this.permanentHealthPenalty = 0;
            return;
        }
        int clamped = Mth.clamp(amount, 0, 10);
        if (this.permanentHealthPenalty != clamped) {
            this.permanentHealthPenalty = clamped;
            this.dirty = true;
        }
    }

    @Override
    public void reducePermanentHealthPenalty(int amount) {
        setPermanentHealthPenalty(this.permanentHealthPenalty - amount);
    }

    @Override
    public int getMaxInfectionReached() {
        return maxInfectionReached;
    }

    @Override
    public void setMaxInfectionReached(int amount) {
        this.maxInfectionReached = Mth.clamp(amount, 0, 100);
        this.dirty = true;
    }

    @Override
    public int getPillTimer() {
        return pillEffectTimer;
    }

    @Override
    public void setPillTimer(int ticks) {
        if (this.pillEffectTimer != ticks) {
            this.pillEffectTimer = ticks;
            this.dirty = true;
        }
    }

    @Override
    public boolean canTakePills() {
        return infection < 50;
    }

    @Override
    public int getStage() {
        if (infection >= 75) return 4;
        if (infection >= 50) return 3;
        if (infection >= 25) return 2;
        if (infection > 0) return 1;
        return 0;
    }

    @Override
    public boolean isImmune() {
        return immune;
    }

    @Override
    public void setImmune(boolean immune) {
        this.immune = immune;
        if (immune) {
            this.infection = 0;
            this.permanentHealthPenalty = 0;
            this.pillEffectTimer = 0;
        }
        this.dirty = true;
    }

    @Override
    public String getMutations() {
        return mutationsCount;
    }

    @Override
    public void setMutations(String mutations) {
        this.mutationsCount = mutations;
        this.dirty = true;
    }

    @Override
    public boolean hasMutation(String mutationId) {
        return getMutationSet().contains(mutationId);
    }

    @Override
    public void addMutation(String mutationId) {
        Set<String> set = getMutationSet();
        if (set.add(mutationId)) {
            saveMutationSet(set);
        }
    }

    @Override
    public void removeMutation(String mutationId) {
        Set<String> set = getMutationSet();
        if (set.remove(mutationId)) {
            saveMutationSet(set);
        }
    }

    private Set<String> getMutationSet() {
        if (mutationsCount == null || mutationsCount.isEmpty()) return new HashSet<>();
        return new HashSet<>(Arrays.asList(mutationsCount.split(",")));
    }

    private void saveMutationSet(Set<String> set) {
        this.mutationsCount = set.stream().collect(Collectors.joining(","));
        this.dirty = true;
    }

    @Override
    public int getSymbiosisTimer() {
        return symbiosisTimer;
    }

    @Override
    public void setSymbiosisTimer(int ticks) {
        this.symbiosisTimer = Math.max(0, ticks);
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("infection", infection);
        tag.putInt("permanentHealthPenalty", permanentHealthPenalty);
        tag.putInt("maxInfectionReached", maxInfectionReached);
        tag.putInt("pillEffectTimer", pillEffectTimer);
        tag.putBoolean("immune", immune);
        tag.putString("mutations", mutationsCount);
        tag.putInt("symbiosisTimer", symbiosisTimer);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        infection = Mth.clamp(tag.getInt("infection"), 0, 100);
        permanentHealthPenalty = Mth.clamp(tag.getInt("permanentHealthPenalty"), 0, 10);
        maxInfectionReached = Mth.clamp(tag.getInt("maxInfectionReached"), 0, 100);
        pillEffectTimer = Math.max(0, tag.getInt("pillEffectTimer"));
        immune = tag.getBoolean("immune");
        mutationsCount = tag.getString("mutations");
        symbiosisTimer = Math.max(0, tag.getInt("symbiosisTimer"));
        dirty = true;
    }
}
