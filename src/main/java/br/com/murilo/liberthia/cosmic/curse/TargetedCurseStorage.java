package br.com.murilo.liberthia.cosmic.curse;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * v0.1.22 r45: Storage de "victim curse" — quem você está amaldiçoando.
 *
 * <p>Quando você usa o {@code WatcherMark} num player, o UUID dele fica
 * gravado no SEU NBT. O server tick olha esse UUID e injeta hallucinations
 * EXTRAS no victim. Você é o "carrier" — você não sente nada, mas faz a
 * vítima ver coisas.
 *
 * <p>NBT key: {@code liberthia.curse_target} (UUID).
 */
public final class TargetedCurseStorage {

    public static final String NBT_KEY = "liberthia.curse_target";
    public static final String NBT_TIMESTAMP = "liberthia.curse_target_since";

    private TargetedCurseStorage() {}

    public static UUID getTargetId(Player carrier) {
        var data = carrier.getPersistentData();
        if (!data.hasUUID(NBT_KEY)) return null;
        return data.getUUID(NBT_KEY);
    }

    public static void setTarget(Player carrier, UUID targetId) {
        if (targetId == null) {
            carrier.getPersistentData().remove(NBT_KEY);
            carrier.getPersistentData().remove(NBT_TIMESTAMP);
        } else {
            carrier.getPersistentData().putUUID(NBT_KEY, targetId);
            carrier.getPersistentData().putLong(NBT_TIMESTAMP,
                    carrier.level().getGameTime());
        }
    }

    public static long getCurseStartTick(Player carrier) {
        return carrier.getPersistentData().getLong(NBT_TIMESTAMP);
    }
}
