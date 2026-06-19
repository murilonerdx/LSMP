package br.com.murilo.liberthia.observation.source;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r61: <b>Source Tick Handler</b> — regenera Source quando o player
 * está §lobservando§r (parado + olhar fixo).
 *
 * <h2>Regen mechanics</h2>
 * <ul>
 *   <li>Player parado (velocity &lt; 0.02) + look estável (pitch/yaw change &lt; 2°) por 60 ticks (3s) → começa regen</li>
 *   <li>Enquanto observando: +1 Source a cada 20 ticks (1Hz)</li>
 *   <li>Movendo / olhando muito → reset do timer</li>
 *   <li>Em sanity baixa: regen 2x mais lento</li>
 * </ul>
 *
 * <p>Isso faz com que o player "pague" pra usar magia — precisa parar, olhar,
 * contemplar. Não é mana automática.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SourceTickHandler {

    /** Player UUID → tick em que começou a observar (0 = não observando). */
    private static final Map<UUID, Long> OBSERVING_SINCE = new ConcurrentHashMap<>();
    /** Player UUID → última posição. */
    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    /** Player UUID → último yaw/pitch. */
    private static final Map<UUID, float[]> LAST_LOOK = new HashMap<>();
    /** Player UUID → próximo tick que pode regen. */
    private static final Map<UUID, Long> NEXT_REGEN_TICK = new HashMap<>();

    /** Threshold em ticks até começar a regen. */
    public static final int OBSERVE_THRESHOLD = 60; // 3s
    /** Intervalo entre regens. */
    public static final int REGEN_INTERVAL = 20; // 1s

    private SourceTickHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 4 != 0) return; // 5Hz check

        UUID id = sp.getUUID();
        long now = sp.tickCount;

        // Verifica se está full (skip work)
        if (SourceData.get(sp) >= SourceData.getMax(sp)) {
            OBSERVING_SINCE.put(id, 0L);
            return;
        }

        // Compara posição/look com último frame
        Vec3 pos = sp.position();
        float yaw = sp.getYRot();
        float pitch = sp.getXRot();

        Vec3 lastPos = LAST_POS.get(id);
        float[] lastLook = LAST_LOOK.get(id);

        boolean moved = lastPos == null || lastPos.distanceTo(pos) > 0.1;
        boolean looked = lastLook == null
                || Math.abs(angleDelta(lastLook[0], yaw)) > 4F
                || Math.abs(lastLook[1] - pitch) > 4F;

        LAST_POS.put(id, pos);
        LAST_LOOK.put(id, new float[]{yaw, pitch});

        if (moved || looked) {
            OBSERVING_SINCE.put(id, 0L);
            return;
        }

        // Started observing?
        Long since = OBSERVING_SINCE.get(id);
        if (since == null || since == 0L) {
            OBSERVING_SINCE.put(id, now);
            return;
        }

        // Threshold reached?
        if (now - since < OBSERVE_THRESHOLD) return;

        // Regen tick?
        Long nextRegen = NEXT_REGEN_TICK.get(id);
        if (nextRegen != null && now < nextRegen) return;
        NEXT_REGEN_TICK.put(id, now + REGEN_INTERVAL);

        // Apply regen (2x slow in low sanity)
        int sanity = br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp);
        int regenAmt = sanity < 30 ? 1 : 2;
        SourceData.add(sp, regenAmt);
    }

    /** Helper: ângulo (em graus) menor entre dois yaws. */
    private static float angleDelta(float a, float b) {
        float d = (b - a) % 360F;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    public static void cleanup(UUID id) {
        OBSERVING_SINCE.remove(id);
        LAST_POS.remove(id);
        LAST_LOOK.remove(id);
        NEXT_REGEN_TICK.remove(id);
    }

    /** Server-side helper: o player está observando agora? */
    public static boolean isObserving(ServerPlayer sp) {
        Long since = OBSERVING_SINCE.get(sp.getUUID());
        if (since == null || since == 0L) return false;
        return sp.tickCount - since >= OBSERVE_THRESHOLD;
    }
}
