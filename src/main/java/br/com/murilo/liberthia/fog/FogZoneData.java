package br.com.murilo.liberthia.fog;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.FogZonesSyncS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Persiste TODAS as zonas de neblina do mundo (de todas as dimensões) num único
 * SavedData no overworld. Sobrevive a reinícios. Qualquer mudança ressincroniza
 * todos os players online.
 */
public final class FogZoneData extends SavedData {

    private static final String DATA_NAME = "liberthia_fog_zones";

    private final List<FogZone> zones = new ArrayList<>();
    private int nextId = 1;

    public FogZoneData() {}

    public static FogZoneData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                FogZoneData::load, FogZoneData::new, DATA_NAME);
    }

    public List<FogZone> getZones() {
        return zones;
    }

    public int nextId() {
        return nextId;
    }

    /** Cria uma zona com TODOS os parâmetros visuais. Retorna o id criado. */
    public int add(MinecraftServer server, String dim, double x, double y, double z,
                   double radius, int color, float density, float opacity,
                   float thickness, double viewDistance, boolean fromItem) {
        int id = nextId++;
        zones.add(new FogZone(id, dim, x, y, z, radius, color, density,
                opacity, thickness, viewDistance, fromItem));
        setDirty();
        syncAll(server);
        return id;
    }

    /** Overload com os defaults visuais (opacidade/intensidade/distância). */
    public int add(MinecraftServer server, String dim, double x, double y, double z,
                   double radius, int color, float density, boolean fromItem) {
        return add(server, dim, x, y, z, radius, color, density,
                FogZone.DEFAULT_OPACITY, FogZone.DEFAULT_THICKNESS, FogZone.DEFAULT_VIEW, fromItem);
    }

    /**
     * Edita UMA propriedade numérica de uma zona por id (densidade, opacidade,
     * transparencia, intensidade, distancia, raio). Retorna a zona atualizada,
     * ou {@code null} se não houver zona com esse id ou a propriedade for inválida.
     */
    public FogZone editById(MinecraftServer server, int id, String prop, double value) {
        for (int i = 0; i < zones.size(); i++) {
            FogZone zn = zones.get(i);
            if (zn.id == id) {
                FogZone updated = zn.withProperty(prop, value);
                if (updated == null) return null;
                zones.set(i, updated);
                setDirty();
                syncAll(server);
                return updated;
            }
        }
        return null;
    }

    /** Troca a cor de uma zona por id. Retorna a zona atualizada ou null. */
    public FogZone editColorById(MinecraftServer server, int id, int color) {
        for (int i = 0; i < zones.size(); i++) {
            FogZone zn = zones.get(i);
            if (zn.id == id) {
                FogZone updated = zn.withColor(color);
                zones.set(i, updated);
                setDirty();
                syncAll(server);
                return updated;
            }
        }
        return null;
    }

    public boolean removeById(MinecraftServer server, int id) {
        boolean removed = zones.removeIf(z -> z.id == id);
        if (removed) {
            setDirty();
            syncAll(server);
        }
        return removed;
    }

    /** Remove a zona mais próxima de (x,y,z) na dimensão, dentro de maxDist. */
    public boolean removeNnear(MinecraftServer server, String dim, double x, double y, double z, double maxDist) {
        FogZone best = null;
        double bestSq = maxDist * maxDist;
        for (FogZone zn : zones) {
            if (!zn.dim.equals(dim)) continue;
            double dsq = zn.distanceSqTo(x, y, z);
            if (dsq <= bestSq) {
                bestSq = dsq;
                best = zn;
            }
        }
        if (best != null) {
            zones.remove(best);
            setDirty();
            syncAll(server);
            return true;
        }
        return false;
    }

    public int clear(MinecraftServer server) {
        int n = zones.size();
        zones.clear();
        setDirty();
        syncAll(server);
        return n;
    }

    // ── Sincronização ────────────────────────────────────────────────────────
    public void syncAll(MinecraftServer server) {
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new FogZonesSyncS2CPacket(new ArrayList<>(zones)));
    }

    public void syncTo(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FogZonesSyncS2CPacket(new ArrayList<>(zones)));
    }

    // ── Persistência ──────────────────────────────────────────────────────────
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (FogZone z : zones) list.add(z.toNbt());
        tag.put("zones", list);
        tag.putInt("nextId", nextId);
        return tag;
    }

    public static FogZoneData load(CompoundTag tag) {
        FogZoneData data = new FogZoneData();
        ListTag list = tag.getList("zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.zones.add(FogZone.fromNbt(list.getCompound(i)));
        }
        data.nextId = Math.max(1, tag.getInt("nextId"));
        return data;
    }
}
