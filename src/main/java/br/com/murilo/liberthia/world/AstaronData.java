package br.com.murilo.liberthia.world;

import br.com.murilo.liberthia.cosmic.ICosmicHorror;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * r187 — <b>Estado global de Astaron</b> (SavedData no overworld). Guarda a radiação dimensional
 * (0–100), o alvo de caos (nº de criaturas cósmicas que o mundo "quer" ter) e a cor do céu (RGB).
 * Lido pela Escala de Astaron, comandos /liberthia astaron e o tick handler.
 */
public class AstaronData extends SavedData {
    public static final String NAME = "liberthia_astaron";

    private int radiation = 0;       // 0-100
    private int chaosTarget = 0;     // nº desejado de cósmicos vivos
    private int skyColor = 0xFF0000; // RGB empacotado (vermelho-sangue default)

    public static AstaronData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AstaronData::load, AstaronData::new, NAME);
    }

    public int getRadiation() { return radiation; }
    public void setRadiation(int v) { radiation = Math.max(0, Math.min(100, v)); setDirty(); }
    public int getChaosTarget() { return chaosTarget; }
    public void setChaosTarget(int v) { chaosTarget = Math.max(0, v); setDirty(); }
    public int getSkyColor() { return skyColor; }
    public void setSkyColor(int rgb) { skyColor = rgb & 0xFFFFFF; setDirty(); }

    public void reset() { radiation = 0; chaosTarget = 0; skyColor = 0xFF0000; setDirty(); }

    /** Conta TODAS as criaturas cósmicas (ICosmicHorror) vivas em todas as dimensões. */
    public int countCosmicEntities(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels())
            for (Entity e : level.getEntities().getAll())
                if (e instanceof ICosmicHorror && e.isAlive()) count++;
        return count;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("radiation", radiation);
        tag.putInt("chaosTarget", chaosTarget);
        tag.putInt("skyColor", skyColor);
        return tag;
    }

    public static AstaronData load(CompoundTag tag) {
        AstaronData d = new AstaronData();
        d.setRadiation(tag.getInt("radiation"));   // clamp 0-100
        d.setChaosTarget(tag.getInt("chaosTarget")); // clamp >=0
        d.setSkyColor(tag.contains("skyColor") ? tag.getInt("skyColor") : 0xFF0000);
        return d;
    }
}
