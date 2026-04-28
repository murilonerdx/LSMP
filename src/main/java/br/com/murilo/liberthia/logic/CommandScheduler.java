package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.CommandRunner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Lightweight per-server scheduler used by {@link br.com.murilo.liberthia.item.CommandTabletItem}
 * and {@link br.com.murilo.liberthia.logic.CommandPylonBlock} to execute
 * commands after a tick delay. Each entry carries a {@code verbose} flag so
 * the original op-tool's output policy is preserved across the delay.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CommandScheduler {

    private static final Deque<Entry> QUEUE = new ArrayDeque<>();
    private static final int MAX_PENDING = 4096;

    private CommandScheduler() {}

    private record Entry(ServerLevel level, java.util.UUID ownerUuid,
                         String command, long fireAtTick, boolean verbose) {}

    /** Default: silent (op-tool default). */
    public static void schedule(ServerLevel level, Entity owner, String command, int delayTicks) {
        schedule(level, owner, command, delayTicks, false);
    }

    public static void schedule(ServerLevel level, Entity owner, String command,
                                int delayTicks, boolean verbose) {
        if (level == null || command == null || command.isBlank()) return;
        if (QUEUE.size() >= MAX_PENDING) return;
        long fireAt = level.getGameTime() + Math.max(1, delayTicks);
        QUEUE.add(new Entry(level, owner == null ? null : owner.getUUID(),
                command, fireAt, verbose));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || QUEUE.isEmpty()) return;
        Iterator<Entry> it = QUEUE.iterator();
        int budget = 256;
        while (it.hasNext() && budget > 0) {
            Entry e = it.next();
            if (e.level.isClientSide()) { it.remove(); continue; }
            if (e.level.getGameTime() < e.fireAtTick) continue;
            it.remove();
            budget--;

            ServerPlayer owner = e.ownerUuid == null ? null
                    : e.level.getServer().getPlayerList().getPlayer(e.ownerUuid);
            if (owner != null) {
                CommandRunner.runAs(owner, e.command, e.verbose);
            } else {
                CommandSourceStack src = e.level.getServer().createCommandSourceStack()
                        .withLevel(e.level)
                        .withPermission(CommandRunner.OP_LEVEL);
                if (!e.verbose) src = src.withSuppressedOutput();
                CommandRunner.run(e.level.getServer(), src, e.command);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent ev) {
        QUEUE.clear();
    }
}
