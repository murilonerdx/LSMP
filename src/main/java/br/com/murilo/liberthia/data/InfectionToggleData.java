package br.com.murilo.liberthia.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persiste o estado on/off da infecção no mundo.
 * Salvo no Overworld — sobrevive a reinícios do servidor.
 * Quando desligada: blocos de infecção podem ser colocados mas não proliferam.
 */
public class InfectionToggleData extends SavedData {

    private static final String DATA_NAME = "liberthia_infection_toggle";
    private boolean infectionEnabled = false; // Padrão: desligada

    public InfectionToggleData() {}

    public static InfectionToggleData get(ServerLevel level) {
        // Sempre usa o overworld para consistência
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                InfectionToggleData::load,
                InfectionToggleData::new,
                DATA_NAME
        );
    }

    public boolean isInfectionEnabled() {
        return infectionEnabled;
    }

    public void setInfectionEnabled(boolean enabled) {
        this.infectionEnabled = enabled;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("infectionEnabled", infectionEnabled);
        return tag;
    }

    public static InfectionToggleData load(CompoundTag tag) {
        InfectionToggleData data = new InfectionToggleData();
        data.infectionEnabled = tag.getBoolean("infectionEnabled");
        return data;
    }
}
