package br.com.murilo.liberthia.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Estado global da <b>Lua do Medo</b> (SavedData no overworld).
 *
 * <ul>
 *   <li>{@code active} — se a Lua do Medo está acontecendo agora.</li>
 *   <li>{@code lastRolledDay} — último dia em que o sorteio de anoitecer rodou
 *       (evita re-sortear toda tick na mesma noite).</li>
 *   <li>{@code forceNext} — o Tambor da Lua do Medo forçou a PRÓXIMA noite.</li>
 * </ul>
 */
public class FearMoonData extends SavedData {

    public static final String NAME = "liberthia_fear_moon";

    private boolean active = false;
    private long lastRolledDay = -1;
    private boolean forceNext = false;
    private int color = 0xC0102A; // cor da lua/céu (vermelho-sangue default)

    public static FearMoonData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(FearMoonData::load, FearMoonData::new, NAME);
    }

    /** Conveniência server-side: a Lua do Medo está ativa neste server? */
    public static boolean isActive(Level level) {
        if (!(level instanceof ServerLevel sl)) return false;
        return get(sl.getServer()).isActive();
    }

    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; setDirty(); }
    public long getLastRolledDay() { return lastRolledDay; }
    public void setLastRolledDay(long d) { lastRolledDay = d; setDirty(); }
    public boolean isForceNext() { return forceNext; }
    public void setForceNext(boolean v) { forceNext = v; setDirty(); }
    public int getColor() { return color; }
    public void setColor(int rgb) { color = rgb & 0xFFFFFF; setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("active", active);
        tag.putLong("lastRolledDay", lastRolledDay);
        tag.putBoolean("forceNext", forceNext);
        tag.putInt("color", color);
        return tag;
    }

    public static FearMoonData load(CompoundTag tag) {
        FearMoonData d = new FearMoonData();
        d.active = tag.getBoolean("active");
        d.lastRolledDay = tag.contains("lastRolledDay") ? tag.getLong("lastRolledDay") : -1;
        d.forceNext = tag.getBoolean("forceNext");
        d.color = tag.contains("color") ? tag.getInt("color") : 0xC0102A;
        return d;
    }
}
