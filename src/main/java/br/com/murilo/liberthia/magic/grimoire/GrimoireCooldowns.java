package br.com.murilo.liberthia.magic.grimoire;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * r173: Cooldown <b>POR FEITIÇO</b> do Grimório (não por item).
 *
 * <p>Antes o cooldown era amarrado ao item Grimório inteiro — castar UM feitiço
 * travava TODOS. Agora cada {@code spellId} tem seu próprio cooldown por player;
 * trocar o slot ativo pra outro feitiço permite castá-lo na hora.
 *
 * <p>Transiente (em memória do server, igual aos cooldowns vanilla de item).
 */
public final class GrimoireCooldowns {

    private GrimoireCooldowns() {}

    /** player UUID → (spellId → gameTime em que o cooldown expira). */
    private static final Map<UUID, Map<String, Long>> MAP = new HashMap<>();

    public static boolean isOnCooldown(ServerPlayer sp, String spellId) {
        Map<String, Long> m = MAP.get(sp.getUUID());
        if (m == null) return false;
        Long expire = m.get(spellId);
        return expire != null && sp.level().getGameTime() < expire;
    }

    public static void set(ServerPlayer sp, String spellId, int ticks) {
        if (ticks <= 0) return;
        MAP.computeIfAbsent(sp.getUUID(), k -> new HashMap<>())
                .put(spellId, sp.level().getGameTime() + ticks);
    }

    /** Ticks restantes (0 se livre). */
    public static int remaining(ServerPlayer sp, String spellId) {
        Map<String, Long> m = MAP.get(sp.getUUID());
        if (m == null) return 0;
        Long expire = m.get(spellId);
        if (expire == null) return 0;
        return (int) Math.max(0, expire - sp.level().getGameTime());
    }
}
