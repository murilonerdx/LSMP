package br.com.murilo.liberthia.client.hud.unified;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * r164: Server-side storage of per-player HUD positions.
 *
 * <p>NBT layout (no {@code player.getPersistentData()}):
 * <pre>
 *   liberthia.hud_positions: {
 *     sanity_hud:    [int x, int y],
 *     mana_bar:      [int x, int y],
 *     ...
 *   }
 * </pre>
 *
 * <p>Persiste entre restart do server porque {@code player.getPersistentData()}
 * é salvo no playerdata/uuid.dat oficial do MC.
 */
public final class HudPositionsData {

    private static final String NBT_ROOT = "liberthia.hud_positions";

    private HudPositionsData() {}

    /** Garante que existe a CompoundTag root e retorna ela. */
    private static CompoundTag root(Player p) {
        var pd = p.getPersistentData();
        if (!pd.contains(NBT_ROOT)) pd.put(NBT_ROOT, new CompoundTag());
        return pd.getCompound(NBT_ROOT);
    }

    /** Retorna [x, y] do HUD (ou defaults se não setou). */
    public static int[] get(Player p, HudId hud) {
        var r = root(p);
        if (!r.contains(hud.id)) {
            return new int[] { hud.defaultX, hud.defaultY };
        }
        int[] xy = r.getIntArray(hud.id);
        if (xy.length != 2) return new int[] { hud.defaultX, hud.defaultY };
        return xy;
    }

    public static void set(Player p, HudId hud, int x, int y) {
        var pd = p.getPersistentData();
        if (!pd.contains(NBT_ROOT)) pd.put(NBT_ROOT, new CompoundTag());
        CompoundTag r = pd.getCompound(NBT_ROOT);
        r.putIntArray(hud.id, new int[] { x, y });
        pd.put(NBT_ROOT, r);
    }

    /** Reseta a posição de um HUD pro default. */
    public static void reset(Player p, HudId hud) {
        var pd = p.getPersistentData();
        if (!pd.contains(NBT_ROOT)) return;
        CompoundTag r = pd.getCompound(NBT_ROOT);
        r.remove(hud.id);
        pd.put(NBT_ROOT, r);
    }

    /** Reseta todos. */
    public static void resetAll(Player p) {
        p.getPersistentData().remove(NBT_ROOT);
    }

    /** Serializa todos os HUDs num CompoundTag pra envio em packet. */
    public static CompoundTag snapshot(Player p) {
        CompoundTag out = new CompoundTag();
        var r = root(p);
        for (HudId hud : HudId.values()) {
            int[] xy = r.contains(hud.id) ? r.getIntArray(hud.id)
                                          : new int[] { hud.defaultX, hud.defaultY };
            out.putIntArray(hud.id, xy);
        }
        return out;
    }
}
