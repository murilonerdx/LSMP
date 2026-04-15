package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmpouleEffectManager {

    private static final Map<UUID, EffectData> EFFECTS = new ConcurrentHashMap<>();

    private AmpouleEffectManager() {}

    public static void applyClear(ServerPlayer target, int durationTicks) {
        EFFECTS.put(target.getUUID(), new EffectData(0, durationTicks));
    }

    public static void applyYellow(ServerPlayer target, int durationTicks) {
        EFFECTS.put(target.getUUID(), new EffectData(1, durationTicks));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (EFFECTS.isEmpty()) return;

        Iterator<Map.Entry<UUID, EffectData>> it = EFFECTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EffectData> entry = it.next();
            EffectData data = entry.getValue();
            data.ticksLeft--;

            if (data.ticksLeft <= 0) {
                it.remove();
                continue;
            }

            ServerPlayer target = findPlayer(entry.getKey());
            if (target == null) {
                it.remove();
                continue;
            }

            if (data.type == 0) {
                tickClear(target, data);
            } else {
                tickYellow(target, data);
            }
        }
    }

    private static void tickClear(ServerPlayer target, EffectData data) {
        // Random teleport every 3 seconds
        if (data.ticksLeft % 60 == 0) {
            double ox = (target.level().random.nextDouble() - 0.5) * 4.0;
            double oz = (target.level().random.nextDouble() - 0.5) * 4.0;
            target.teleportTo(target.getX() + ox, target.getY(), target.getZ() + oz);
        }

        // Intermittent levitation every 5s for 1s
        if (data.ticksLeft % 100 == 0) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 1, false, false, true));
        }
    }

    private static void tickYellow(ServerPlayer target, EffectData data) {
        // Emotional outburst every 5 seconds: 30% chance
        if (data.ticksLeft % 100 == 0 && target.level().random.nextFloat() < 0.30F) {
            if (target.level().random.nextBoolean()) {
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1, false, false, true));
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
            }
        }
    }

    private static ServerPlayer findPlayer(UUID id) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(id);
    }

    private static final class EffectData {
        final int type; // 0=clear, 1=yellow
        int ticksLeft;
        EffectData(int type, int ticks) {
            this.type = type;
            this.ticksLeft = ticks;
        }
    }
}
