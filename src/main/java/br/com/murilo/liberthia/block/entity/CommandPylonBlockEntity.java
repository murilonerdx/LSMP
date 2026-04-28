package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.item.CommandRunner;
import br.com.murilo.liberthia.item.CommandTabletItem;
import br.com.murilo.liberthia.logic.CommandScheduler;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Programmable command block backend.
 *
 * <p>Triggers (any combination — they are OR'd):
 * <ul>
 *   <li>Manual right-click ({@link br.com.murilo.liberthia.logic.CommandPylonBlock#use}).
 *   <li>Redstone rising-edge.
 *   <li>{@code @loop N} directive on the first non-comment line — fires every N ticks.
 *   <li>{@code @trigger proximity R} directive — fires when any player enters
 *       a sphere of radius R blocks around the pylon. Re-arms when no player
 *       is within range. Capped at 64.
 * </ul>
 *
 * <p>While armed, the pylon emits a thin {@link ParticleTypes#END_ROD} aura
 * around itself so it's easy to spot.
 */
public class CommandPylonBlockEntity extends BlockEntity {
    private static final String NBT_COMMANDS = "Commands";
    private static final String NBT_LOOP = "LoopInterval";
    private static final String NBT_LAST_POWER = "LastPow";
    private static final String NBT_NEXT_FIRE = "NextFire";
    private static final String NBT_VERBOSE = "Verbose";
    private static final String NBT_PROXIMITY_RADIUS = "ProxR";
    private static final String NBT_PROXIMITY_FIRED = "ProxFired";

    private static final Pattern PROXIMITY_DIRECTIVE = Pattern.compile(
            "^@trigger\\s+proximity\\s+(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOP_DIRECTIVE = Pattern.compile(
            "^@loop\\s+(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);

    private List<String> commands = new ArrayList<>();
    private int loopInterval = 0;
    private int proximityRadius = 0;
    /** True after a proximity fire — resets when no players are in range. */
    private boolean proximityFired = false;
    private boolean lastPowered = false;
    private long nextFireTick = -1L;
    private boolean verbose = false;

    public CommandPylonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMMAND_PYLON.get(), pos, state);
    }

    public int commandCount() { return commands.size(); }
    public int getLoopInterval() { return loopInterval; }
    public int getProximityRadius() { return proximityRadius; }
    public boolean isVerbose() { return verbose; }

    public void clearProgram() {
        commands.clear();
        loopInterval = 0;
        proximityRadius = 0;
        proximityFired = false;
        nextFireTick = -1L;
        setChanged();
    }

    public void imprintFromTablet(ItemStack tablet) {
        List<String> from = CommandTabletItem.readCommands(tablet);
        commands = new ArrayList<>(from);
        verbose = CommandTabletItem.readVerbose(tablet);
        parseDirectives();
        proximityFired = false;
        nextFireTick = -1L;
        setChanged();
    }

    /** Parses {@code @loop N} and {@code @trigger proximity R} directives at the top. */
    private void parseDirectives() {
        loopInterval = 0;
        proximityRadius = 0;
        for (String raw : commands) {
            String t = raw == null ? "" : raw.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            if (!t.startsWith("@")) break; // first real command ends directive scan
            Matcher mLoop = LOOP_DIRECTIVE.matcher(t);
            if (mLoop.matches()) {
                try { loopInterval = Math.max(1, Integer.parseInt(mLoop.group(1))); } catch (Exception ignored) {}
                continue;
            }
            Matcher mProx = PROXIMITY_DIRECTIVE.matcher(t);
            if (mProx.matches()) {
                try {
                    int r = Integer.parseInt(mProx.group(1));
                    proximityRadius = Math.max(1, Math.min(64, r));
                } catch (Exception ignored) {}
            }
        }
    }

    /** Builds a one-line human-readable status. */
    public String describeMode() {
        StringBuilder sb = new StringBuilder();
        sb.append("manual");
        if (loopInterval > 0) sb.append(" + loop ").append(loopInterval).append("t");
        if (proximityRadius > 0) sb.append(" + proximidade ").append(proximityRadius).append(" blocos");
        sb.append(" + redstone");
        return sb.toString();
    }

    public void onRedstone(ServerLevel level, boolean powered) {
        if (powered && !lastPowered) {
            fire(level, null);
        }
        lastPowered = powered;
        setChanged();
    }

    public void fire(ServerLevel level, ServerPlayer asPlayer) {
        if (commands.isEmpty()) return;
        int delay = 0;
        for (String raw : commands) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("@")) {
                int sp = line.indexOf(' ');
                if (sp > 1) {
                    String tag = line.substring(1, sp);
                    String rest = line.substring(sp + 1).trim();
                    // skip directives during execution
                    if (tag.equalsIgnoreCase("loop") || tag.equalsIgnoreCase("trigger")) continue;
                    try {
                        delay += Math.max(0, Integer.parseInt(tag));
                        line = rest;
                    } catch (NumberFormatException ignored) {}
                }
            }
            String cmd = line
                    .replace("{bx}", String.valueOf(worldPosition.getX()))
                    .replace("{by}", String.valueOf(worldPosition.getY()))
                    .replace("{bz}", String.valueOf(worldPosition.getZ()));

            if (delay <= 0) {
                if (asPlayer != null) {
                    CommandRunner.runAs(asPlayer, cmd, verbose);
                } else {
                    runFromBlock(level, cmd);
                }
            } else {
                CommandScheduler.schedule(level, asPlayer, cmd, delay, verbose);
            }
        }

        // Visual: small flash of particles when firing.
        Vec3 c = Vec3.atCenterOf(worldPosition);
        level.sendParticles(ParticleTypes.FLASH,
                c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                c.x, c.y + 1.0, c.z, 8, 0.3, 0.4, 0.3, 0.04);
    }

    private void runFromBlock(ServerLevel level, String cmd) {
        CommandSourceStack src = level.getServer().createCommandSourceStack()
                .withPosition(Vec3.atCenterOf(worldPosition))
                .withLevel(level)
                .withPermission(CommandRunner.OP_LEVEL);
        if (!verbose) src = src.withSuppressedOutput();
        CommandRunner.run(level.getServer(), src, cmd);
    }

    public void serverTick(ServerLevel level) {
        if (commands.isEmpty()) return;
        long now = level.getGameTime();

        // --- Particle aura — every 10 ticks ---
        if (now % 10 == 0) {
            Vec3 c = Vec3.atCenterOf(worldPosition);
            for (int i = 0; i < 3; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = 0.55;
                level.sendParticles(ParticleTypes.END_ROD,
                        c.x + Math.cos(a) * r,
                        c.y + 0.4 + level.random.nextDouble() * 0.6,
                        c.z + Math.sin(a) * r,
                        1, 0, 0.02, 0, 0);
            }
            // Extra brightness for proximity-armed pylons.
            if (proximityRadius > 0 && !proximityFired) {
                level.sendParticles(ParticleTypes.ENCHANT,
                        c.x, c.y + 1.0, c.z, 2, 0.4, 0.2, 0.4, 0.5);
            }
        }

        // --- Loop trigger ---
        if (loopInterval > 0) {
            if (nextFireTick < 0) {
                nextFireTick = now + loopInterval;
            } else if (now >= nextFireTick) {
                fire(level, null);
                nextFireTick = now + loopInterval;
                setChanged();
            }
        }

        // --- Proximity trigger ---
        if (proximityRadius > 0) {
            // Cheap check every 5 ticks (no need every tick).
            if (now % 5 == 0) {
                Vec3 center = Vec3.atCenterOf(worldPosition);
                double r2 = (double) proximityRadius * proximityRadius;
                boolean someoneNear = false;
                for (ServerPlayer p : level.players()) {
                    if (p.isSpectator()) continue;
                    if (p.position().distanceToSqr(center) <= r2) {
                        someoneNear = true; break;
                    }
                }
                if (someoneNear && !proximityFired) {
                    fire(level, null);
                    proximityFired = true;
                    setChanged();
                } else if (!someoneNear && proximityFired) {
                    proximityFired = false;
                    setChanged();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (String c : commands) list.add(StringTag.valueOf(c));
        tag.put(NBT_COMMANDS, list);
        tag.putInt(NBT_LOOP, loopInterval);
        tag.putInt(NBT_PROXIMITY_RADIUS, proximityRadius);
        tag.putBoolean(NBT_PROXIMITY_FIRED, proximityFired);
        tag.putBoolean(NBT_LAST_POWER, lastPowered);
        tag.putLong(NBT_NEXT_FIRE, nextFireTick);
        tag.putBoolean(NBT_VERBOSE, verbose);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        commands.clear();
        if (tag.contains(NBT_COMMANDS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_COMMANDS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) commands.add(list.getString(i));
        }
        loopInterval = tag.getInt(NBT_LOOP);
        proximityRadius = tag.getInt(NBT_PROXIMITY_RADIUS);
        proximityFired = tag.getBoolean(NBT_PROXIMITY_FIRED);
        lastPowered = tag.getBoolean(NBT_LAST_POWER);
        nextFireTick = tag.contains(NBT_NEXT_FIRE) ? tag.getLong(NBT_NEXT_FIRE) : -1L;
        verbose = tag.getBoolean(NBT_VERBOSE);
    }
}
