package br.com.murilo.liberthia.item;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Helper to run vanilla / mod commands at OP permission level (4) on behalf of
 * an item. Used by Growth/Shrink rods, the Command Tablet, the Command Pylon
 * and the Script Tablet.
 *
 * <p><b>Output policy:</b> these are op-only items; by default, command output
 * (Brigadier feedback, "Unknown command", scale-set confirmations, etc) is
 * <b>suppressed</b> so the player's chat stays clean. Pass {@code verbose=true}
 * to opt in — wired through each item's "Verbose" NBT toggle.
 */
public final class CommandRunner {
    public static final int OP_LEVEL = 4;

    private CommandRunner() {}

    /** Returns true if the actor has op permission ≥ 2. */
    public static boolean isOp(ServerPlayer p) {
        return p != null && p.hasPermissions(2);
    }

    /**
     * Build a command source stack at op level rooted at the given entity.
     * If the actor is a {@link ServerPlayer}, it is also the {@link CommandSource}
     * (so verbose output goes to the player's chat).
     */
    public static CommandSourceStack opSourceFor(Entity actor, ServerLevel level) {
        Vec3 pos = actor.position();
        Vec2 rot = new Vec2(actor.getXRot(), actor.getYRot());
        CommandSource src = (actor instanceof ServerPlayer sp) ? sp : CommandSource.NULL;
        return new CommandSourceStack(
                src,
                pos, rot, level, OP_LEVEL,
                actor.getName().getString(), actor.getDisplayName(),
                level.getServer(), actor
        );
    }

    /** Low-level dispatch. Returns the dispatcher result (>0 success). */
    public static int run(MinecraftServer server, CommandSourceStack source, String command) {
        if (server == null || command == null || command.isBlank()) return -1;
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        try {
            return server.getCommands().performPrefixedCommand(source, trimmed);
        } catch (Exception ex) {
            return -1;
        }
    }

    /** Run as actor — silent by default (op-tool default). */
    public static int runAs(Entity actor, String command) {
        return runAs(actor, command, false);
    }

    /** Run as actor with explicit verbose flag. */
    public static int runAs(Entity actor, String command, boolean verbose) {
        if (!(actor.level() instanceof ServerLevel sl)) return -1;
        MinecraftServer server = sl.getServer();
        if (server == null) return -1;
        CommandSourceStack src = opSourceFor(actor, sl);
        if (!verbose) src = src.withSuppressedOutput();
        return run(server, src, command);
    }

    /** Convenience verbose alias. */
    public static int runAsVerbose(Entity actor, String command) {
        return runAs(actor, command, true);
    }

    /** Convenience suppressed alias. */
    public static int runAsSuppressed(Entity actor, String command) {
        return runAs(actor, command, false);
    }
}
