package br.com.murilo.liberthia.magic.casting;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.24 r91: <b>RecastData</b> — Iron's Spells recast pattern.
 *
 * <p>Quando um player casta certo spell (ex.: Blood Step), ele ganha N
 * "follow-up casts" disponíveis por uma janela curta. Cada follow-up
 * cast usa essas charges em vez de Source novo.
 *
 * <h2>Estado</h2>
 * <ul>
 *   <li>{@code spellId} — qual spell está com recasts</li>
 *   <li>{@code remaining} — quantas charges restam</li>
 *   <li>{@code expireTick} — tick em que charges expiram</li>
 * </ul>
 */
public final class RecastData {

    public final String spellId;
    public int remaining;
    public final long expireTick;

    public RecastData(String spellId, int charges, long expireTick) {
        this.spellId = spellId;
        this.remaining = charges;
        this.expireTick = expireTick;
    }

    /** Em memória, por player. */
    private static final Map<UUID, RecastData> ACTIVE = new HashMap<>();

    public static void grant(Player player, String spellId, int charges, int durationTicks) {
        long expire = player.level().getGameTime() + durationTicks;
        ACTIVE.put(player.getUUID(), new RecastData(spellId, charges, expire));
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5Recast §b" + charges + " §5disponíveis"), true);
        }
    }

    public static RecastData get(Player player) {
        return ACTIVE.get(player.getUUID());
    }

    /** Consome 1 charge. Returns true se ok. */
    public static boolean consume(Player player, String spellId) {
        RecastData data = ACTIVE.get(player.getUUID());
        if (data == null) return false;
        if (!data.spellId.equals(spellId)) return false;
        if (player.level().getGameTime() > data.expireTick) {
            ACTIVE.remove(player.getUUID());
            return false;
        }
        data.remaining--;
        if (data.remaining <= 0) {
            ACTIVE.remove(player.getUUID());
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal(
                        "§7§o✦ Recasts esgotados"), true);
            }
        }
        return true;
    }

    /** Limpa expired automaticamente. */
    public static void cleanup(Player player) {
        RecastData data = ACTIVE.get(player.getUUID());
        if (data != null && player.level().getGameTime() > data.expireTick) {
            ACTIVE.remove(player.getUUID());
        }
    }

    public static int getRemainingCharges(Player player) {
        RecastData data = ACTIVE.get(player.getUUID());
        if (data == null) return 0;
        if (player.level().getGameTime() > data.expireTick) {
            ACTIVE.remove(player.getUUID());
            return 0;
        }
        return data.remaining;
    }
}
