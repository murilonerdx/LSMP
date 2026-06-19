package br.com.murilo.liberthia.cosmic.mimic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r178: <b>O Que Imita Sons</b> — grava os sons que VOCÊ faz (quebrar blocos,
 * passos) e os reproduz de novo, <b>de longe</b>, minutos depois. Você ouve "você
 * mesmo" minerando num lugar onde não está. Sem entidade — puro áudio.
 * Gateado em {@code cosmic_horror_enabled} (default OFF).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SoundMimicManager {

    private static final int MAX = 8;
    private static final Map<UUID, Deque<SoundEvent>> MEMORY = new ConcurrentHashMap<>();

    private SoundMimicManager() {}

    private static boolean enabled() {
        return br.com.murilo.liberthia.config.LiberthiaConfig.SERVER.cosmicHorrorEnabled.get();
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!enabled()) return;
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        BlockState st = e.getState();
        record(sp.getUUID(), st.getSoundType().getBreakSound());
    }

    private static void record(UUID id, SoundEvent s) {
        if (s == null) return;
        Deque<SoundEvent> q = MEMORY.computeIfAbsent(id, k -> new ArrayDeque<>());
        q.addLast(s);
        while (q.size() > MAX) q.removeFirst();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 100 != 0) return; // checa a cada 5s
        if (!enabled()) return;
        Deque<SoundEvent> q = MEMORY.get(sp.getUUID());
        if (q == null || q.isEmpty()) return;
        // raro: reproduz um som gravado a 14–26 blocos do player
        if (sp.getRandom().nextFloat() > 0.08F) return;
        SoundEvent[] arr = q.toArray(new SoundEvent[0]);
        SoundEvent s = arr[sp.getRandom().nextInt(arr.length)];
        if (!(sp.level() instanceof ServerLevel sl)) return;
        double ang = sl.random.nextDouble() * Math.PI * 2;
        double d = 14 + sl.random.nextDouble() * 12;
        double x = sp.getX() + Math.cos(ang) * d, z = sp.getZ() + Math.sin(ang) * d;
        sl.playSound(null, x, sp.getY(), z, s, SoundSource.HOSTILE, 0.8F, 0.95F + sl.random.nextFloat() * 0.1F);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        MEMORY.remove(e.getEntity().getUUID());
    }
}
