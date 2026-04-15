package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
public final class HorusEffectManager {

    private static final Map<UUID, HorusData> TARGETS = new ConcurrentHashMap<>();

    private HorusEffectManager() {}

    public static void apply(ServerPlayer target, int durationTicks) {
        TARGETS.put(target.getUUID(), new HorusData(durationTicks));
        // Initial darkness
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false, false));
    }

    public static void remove(UUID targetId) {
        TARGETS.remove(targetId);
    }

    public static boolean isAffected(UUID targetId) {
        return TARGETS.containsKey(targetId);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (TARGETS.isEmpty()) return;

        Iterator<Map.Entry<UUID, HorusData>> it = TARGETS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, HorusData> entry = it.next();
            HorusData data = entry.getValue();
            data.ticksLeft--;

            if (data.ticksLeft <= 0) {
                it.remove();
                continue;
            }

            // Find player
            ServerPlayer target = findPlayer(entry.getKey());
            if (target == null) {
                it.remove();
                continue;
            }

            // Reapply darkness periodically
            if (data.ticksLeft % 100 == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, false));
            }

            // Whisper sound every 3 seconds
            if (data.ticksLeft % 60 == 0) {
                target.serverLevel().playSound(null, target.blockPosition(),
                        ModSounds.DARK_WHISPER.get(), SoundSource.HOSTILE, 0.6F, 0.5F);
            }

            // Portal particles every second
            if (data.ticksLeft % 20 == 0) {
                ServerLevel level = target.serverLevel();
                double x = target.getX();
                double y = target.getY() + 1.0;
                double z = target.getZ();
                for (int i = 0; i < 8; i++) {
                    double ox = (level.random.nextDouble() - 0.5) * 2.0;
                    double oy = (level.random.nextDouble() - 0.5) * 2.0;
                    double oz = (level.random.nextDouble() - 0.5) * 2.0;
                    level.sendParticles(ParticleTypes.PORTAL, x + ox, y + oy, z + oz,
                            1, 0, 0, 0, 0.1);
                }
            }
        }
    }

    private static ServerPlayer findPlayer(UUID id) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(id);
    }

    private static final class HorusData {
        int ticksLeft;
        HorusData(int ticks) { this.ticksLeft = ticks; }
    }
}
