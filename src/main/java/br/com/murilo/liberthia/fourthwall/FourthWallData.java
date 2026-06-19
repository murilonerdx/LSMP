package br.com.murilo.liberthia.fourthwall;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumMap;
import java.util.Map;

/**
 * Estado persistente (overworld) do sistema de quarta parede:
 * liga/desliga geral (master), por efeito, e intensidade (0=raro,1=médio,2=frequente).
 * Editado pelo {@code /liberthia fourthwall ...} (OP). Default: MASTER OFF
 * (o dono do server liga quando quiser).
 */
public final class FourthWallData extends SavedData {

    private static final String NAME = "liberthia_fourthwall";

    private boolean master = false;
    private int intensity = 1; // 0=low, 1=med, 2=high
    private final Map<FourthWallFeature, Boolean> features = new EnumMap<>(FourthWallFeature.class);

    public FourthWallData() {
        for (FourthWallFeature f : FourthWallFeature.values()) features.put(f, true);
    }

    public static FourthWallData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(FourthWallData::load, FourthWallData::new, NAME);
    }

    public boolean isMaster() { return master; }
    public void setMaster(boolean b) { this.master = b; setDirty(); }

    public int getIntensity() { return intensity; }
    public void setIntensity(int i) { this.intensity = Math.max(0, Math.min(2, i)); setDirty(); }

    /** Ligado de verdade = master ON e o efeito habilitado. */
    public boolean isOn(FourthWallFeature f) { return master && features.getOrDefault(f, false); }
    public boolean rawOn(FourthWallFeature f) { return features.getOrDefault(f, false); }
    public void setFeature(FourthWallFeature f, boolean b) { features.put(f, b); setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("master", master);
        tag.putInt("intensity", intensity);
        CompoundTag ft = new CompoundTag();
        for (Map.Entry<FourthWallFeature, Boolean> e : features.entrySet()) {
            ft.putBoolean(e.getKey().id, e.getValue());
        }
        tag.put("features", ft);
        return tag;
    }

    public static FourthWallData load(CompoundTag tag) {
        FourthWallData d = new FourthWallData();
        d.master = tag.getBoolean("master");
        d.intensity = tag.getInt("intensity");
        CompoundTag ft = tag.getCompound("features");
        for (FourthWallFeature f : FourthWallFeature.values()) {
            if (ft.contains(f.id)) d.features.put(f, ft.getBoolean(f.id));
        }
        return d;
    }
}
