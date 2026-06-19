package br.com.murilo.liberthia.client.hud.unified;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

/**
 * r164: Client-side cache of HUD positions (synced from server on login + após
 * cada drag/save). Todos os overlays do mod leem daqui em vez de
 * {@code LiberthiaConfig.CLIENT.xxxX/Y}.
 *
 * <p>Default fallback: se o packet sync ainda não chegou (player só entrou),
 * usa {@link HudId#defaultX}/{@link HudId#defaultY}.
 */
public final class ClientHudPositions {

    private static final Map<HudId, int[]> POSITIONS = new EnumMap<>(HudId.class);
    private static boolean synced = false;

    private ClientHudPositions() {}

    /** Lê do server snapshot e popula o cache local. */
    public static void load(CompoundTag tag) {
        POSITIONS.clear();
        for (HudId hud : HudId.values()) {
            int[] xy;
            if (tag != null && tag.contains(hud.id)) {
                xy = tag.getIntArray(hud.id);
                if (xy.length != 2) xy = new int[] { hud.defaultX, hud.defaultY };
            } else {
                xy = new int[] { hud.defaultX, hud.defaultY };
            }
            POSITIONS.put(hud, xy);
        }
        synced = true;
    }

    public static boolean isSynced() { return synced; }

    /** Posição X efetiva. Resolve âncora right → screenWidth - x. */
    public static int x(HudId hud, int screenWidth) {
        int[] xy = POSITIONS.computeIfAbsent(hud,
                h -> new int[] { h.defaultX, h.defaultY });
        return hud.anchorRight ? screenWidth - xy[0] - hud.width : xy[0];
    }

    /** Posição Y efetiva. Resolve âncora bottom → screenHeight - y. */
    public static int y(HudId hud, int screenHeight) {
        int[] xy = POSITIONS.computeIfAbsent(hud,
                h -> new int[] { h.defaultX, h.defaultY });
        return hud.anchorBottom ? screenHeight - xy[1] - hud.height : xy[1];
    }

    /** Posição X bruta (sem aplicar âncora). Usado no editor pra render preview. */
    public static int rawX(HudId hud) {
        int[] xy = POSITIONS.computeIfAbsent(hud,
                h -> new int[] { h.defaultX, h.defaultY });
        return xy[0];
    }

    public static int rawY(HudId hud) {
        int[] xy = POSITIONS.computeIfAbsent(hud,
                h -> new int[] { h.defaultX, h.defaultY });
        return xy[1];
    }

    /** Atualiza local (após drag no editor). NÃO envia packet — o caller envia. */
    public static void setLocal(HudId hud, int rawX, int rawY) {
        POSITIONS.put(hud, new int[] { rawX, rawY });
    }

    public static void resetLocal(HudId hud) {
        POSITIONS.put(hud, new int[] { hud.defaultX, hud.defaultY });
    }
}
