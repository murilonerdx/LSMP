package br.com.murilo.liberthia.capture;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenCapturedScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CapturedPlayerManager {

    private static final Map<UUID, CapturedPlayerState> CAPTURED_PLAYERS = new ConcurrentHashMap<>();

    private CapturedPlayerManager() {
    }

    public static void capture(ServerPlayer target, ServerPlayer captor) {
        CapturedPlayerState state = new CapturedPlayerState(
                target.getUUID(),
                captor.getUUID(),
                target.blockPosition(),
                target.level().getGameTime()
        );

        CAPTURED_PLAYERS.put(target.getUUID(), state);

        target.setInvisible(true);
        target.setInvulnerable(true);
        target.setDeltaMovement(0, 0, 0);

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Integer.MAX_VALUE, 255, false, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Integer.MAX_VALUE, 255, false, false, false));

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> target),
                new OpenCapturedScreenPacket()
        );
    }

    public static void release(ServerPlayer target) {
        CAPTURED_PLAYERS.remove(target.getUUID());

        target.setInvisible(false);
        target.setInvulnerable(false);

        target.removeEffect(MobEffects.BLINDNESS);
        target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        target.removeEffect(MobEffects.WEAKNESS);

        target.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Você escapou do selo."),
                false
        );
    }

    public static boolean isCaptured(ServerPlayer player) {
        return CAPTURED_PLAYERS.containsKey(player.getUUID());
    }

    public static Optional<CapturedPlayerState> get(ServerPlayer player) {
        return Optional.ofNullable(CAPTURED_PLAYERS.get(player.getUUID()));
    }
}