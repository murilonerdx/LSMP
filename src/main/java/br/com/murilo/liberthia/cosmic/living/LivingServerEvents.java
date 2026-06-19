package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * v0.1.22 r47: Hook central que aplica observation pressure no chunk
 * dependendo do que o player faz.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LivingServerEvents {

    /** Regex captura coords mencionadas no chat (formatos: "X Z", "X Y Z", "X, Y, Z"). */
    private static final Pattern COORD_PATTERN = Pattern.compile(
            "(-?\\d{2,5})\\s*[,\\s]\\s*(-?\\d{1,4})?\\s*[,\\s]?\\s*(-?\\d{2,5})");

    private LivingServerEvents() {}

    /** Decay periodico do pressure. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = event.getServer().overworld().getGameTime();
        // Decay 1/min
        if (now % 1200 == 0) ObservationPressure.decayTick();
    }

    /** Aplica pressure baseado em onde o player está olhando. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 60 != 0) return; // 3s

        // Pressure +1 no chunk atual (parado conta como "estando ali")
        ChunkPos current = new ChunkPos(sp.blockPosition());
        ObservationPressure.add(current, 1);

        // Pressure +1 no chunk pra onde olha (até 32b)
        HitResult hit = sp.pick(32.0, 0, false);
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 hp = hit.getLocation();
            ChunkPos looked = new ChunkPos(
                    net.minecraft.core.BlockPos.containing(hp));
            if (!looked.equals(current)) {
                ObservationPressure.add(looked, 1);
            }
        }
    }

    /** Chat mencionando coordenadas aumenta pressure muito. */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        String raw = event.getRawText();
        Matcher m = COORD_PATTERN.matcher(raw);
        while (m.find()) {
            try {
                int x = Integer.parseInt(m.group(1));
                int z = Integer.parseInt(m.group(3) != null ? m.group(3) : m.group(2));
                ChunkPos cp = new ChunkPos(x >> 4, z >> 4);
                ObservationPressure.add(cp, 8);
                LiberthiaMod.LOGGER.debug("[ObsPressure] chat mention chunk {} +8", cp);
            } catch (NumberFormatException ignored) {}
        }
        // Mention "anomaly", "ghost", "phantom", "estranho" etc. adds general pressure
        String lower = raw.toLowerCase();
        String[] horrorKeywords = {"phantom", "ghost", "anomalia", "estranho", "vulto",
                "demon", "demônio", "horror", "voice", "vozes"};
        boolean keyword = false;
        for (String k : horrorKeywords) {
            if (lower.contains(k)) { keyword = true; break; }
        }
        if (keyword && event.getPlayer() != null) {
            ChunkPos chatChunk = new ChunkPos(event.getPlayer().blockPosition());
            ObservationPressure.add(chatChunk, 5);
        }
    }

    /** Cleanup tracking quando player desloga. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MemoryReplayManager.cleanupPlayer(event.getEntity().getUUID());
    }
}
