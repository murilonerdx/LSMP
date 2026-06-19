package br.com.murilo.liberthia.freeze;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton que registra quais players estão "congelados" — incapazes de se
 * mover, atacar, abrir inventário, interagir. Cada tentativa de movimento
 * causa dano crescente. Estado armazenado em memória (não persiste em restart
 * — voltar a freezar é responsabilidade do operador).
 *
 * Thread-safe: ConcurrentHashMap pra suportar acesso de event bus (main thread)
 * + HTTP handler (thread pool).
 */
public final class FreezeManager {
    private static final Map<UUID, FrozenState> STATES = new ConcurrentHashMap<>();

    private FreezeManager() {}

    public static final class FrozenState {
        public final double anchorX, anchorY, anchorZ;
        public final float anchorYaw, anchorPitch;
        public final long frozenSinceTick;
        /** Contador de tentativas de movimento — dano cresce com cada tentativa. */
        public int moveAttempts = 0;
        /** Última tick em que o player tentou se mexer. Usado pra decay do contador. */
        public long lastAttemptTick = 0L;

        FrozenState(double x, double y, double z, float yaw, float pitch, long tick) {
            this.anchorX = x; this.anchorY = y; this.anchorZ = z;
            this.anchorYaw = yaw; this.anchorPitch = pitch;
            this.frozenSinceTick = tick;
        }
    }

    public static void freeze(ServerPlayer player) {
        STATES.put(player.getUUID(), new FrozenState(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.serverLevel().getGameTime()
        ));
    }

    public static void unfreeze(UUID uuid) {
        STATES.remove(uuid);
    }

    public static boolean isFrozen(UUID uuid) {
        return STATES.containsKey(uuid);
    }

    public static FrozenState getState(UUID uuid) {
        return STATES.get(uuid);
    }

    public static Vec3 getAnchor(UUID uuid) {
        FrozenState s = STATES.get(uuid);
        return s == null ? null : new Vec3(s.anchorX, s.anchorY, s.anchorZ);
    }

    public static int frozenCount() {
        return STATES.size();
    }
}
