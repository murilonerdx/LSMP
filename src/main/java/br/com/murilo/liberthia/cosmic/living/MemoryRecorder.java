package br.com.murilo.liberthia.cosmic.living;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r47: Hooks Forge que gravam o que acontece em cada chunk
 * no {@link ChunkMemoryStorage}.
 *
 * <h2>Hooks</h2>
 * <ul>
 *   <li>{@link LivingDeathEvent}: morte → record death, +PvP se killer for player</li>
 *   <li>{@link ServerChatEvent}: chat → record activity + guarda mensagem como echo (10% chance)</li>
 *   <li>{@link BlockEvent.BreakEvent}: blocks broken</li>
 *   <li>{@link BlockEvent.EntityPlaceEvent}: blocks placed</li>
 *   <li>{@code PlayerTickEvent}: visit ticks (a cada 200t = 10s)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MemoryRecorder {

    private MemoryRecorder() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        LivingEntity victim = event.getEntity();
        ChunkPos cp = new ChunkPos(victim.blockPosition());
        ChunkMemory cm = ChunkMemoryStorage.get(sl).get(cp);
        long now = sl.getGameTime();
        boolean pvp = victim instanceof Player && event.getSource().getEntity() instanceof Player;
        if (pvp) {
            cm.recordPvpKill(now);
            // Echo extra: gravação de "death cry" pra replay
            cm.addEcho(new ChunkMemory.Echo(
                    ChunkMemory.Echo.Type.DEATH_CRY,
                    victim.getName().getString(),
                    victim.getUUID(),
                    now));
        } else {
            cm.recordDeath(now);
            // 30% chance de gravar como echo
            if (Math.random() < 0.30) {
                cm.addEcho(new ChunkMemory.Echo(
                        ChunkMemory.Echo.Type.DEATH_CRY,
                        victim.getName().getString(),
                        victim.getUUID(),
                        now));
            }
        }
        ChunkMemoryStorage.get(sl).setDirty();
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sp = event.getPlayer();
        ServerLevel sl = sp.serverLevel();
        ChunkPos cp = new ChunkPos(sp.blockPosition());
        ChunkMemory cm = ChunkMemoryStorage.get(sl).get(cp);
        cm.recordChat(sl.getGameTime(), event.getRawText(), sp.getUUID());
        ChunkMemoryStorage.get(sl).setDirty();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        ChunkPos cp = new ChunkPos(event.getPos());
        ChunkMemory cm = ChunkMemoryStorage.get(sl).get(cp);
        cm.recordBlockBreak(sl.getGameTime());
        // 1% chance de gravar como echo de mining sound
        if (Math.random() < 0.01) {
            cm.addEcho(new ChunkMemory.Echo(
                    ChunkMemory.Echo.Type.MINING,
                    event.getState().getBlock().toString(),
                    event.getPlayer() != null ? event.getPlayer().getUUID() : null,
                    sl.getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        ChunkPos cp = new ChunkPos(event.getPos());
        ChunkMemoryStorage.get(sl).get(cp).recordBlockPlace(sl.getGameTime());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 200 != 0) return; // grava visit a cada 10s

        ChunkPos cp = new ChunkPos(sp.blockPosition());
        ChunkMemory cm = ChunkMemoryStorage.get(sp.serverLevel()).get(cp);
        cm.recordVisit(200);
    }
}
