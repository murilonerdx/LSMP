package br.com.murilo.liberthia.item.script;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.CommandRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tree-of-instructions interpreter for LiberScript. Pre-emptive against runaway
 * loops via a per-resume instruction budget; long sleeps are handled by parking
 * the VM on a global tick queue keyed by holder UUID.
 *
 * <p>Concurrency: only one VM per holder is allowed; starting a new script
 * cancels any pending one for that player.
 *
 * <p>Output policy: the {@code verbose} flag (passed by the holding tablet)
 * decides whether {@code run}/{@code say} feedback and start/finish meta
 * messages reach chat. Errors always surface in chat so users can debug.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ScriptVM {

    private static final int SLICE_BUDGET = 4000;
    private static final int TOTAL_BUDGET = 200_000;
    private static final int OUTPUT_BUDGET = 200_000;
    private static final int STACK_LIMIT = 1024;

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{([A-Za-z_][A-Za-z_0-9]*)\\}");

    private final List<Insn> code;
    private final Map<String, Object> vars = new HashMap<>();
    private final Deque<Object> stack = new ArrayDeque<>();
    private int pc = 0;
    private int totalSteps = 0;
    private int totalOutput = 0;
    private final UUID ownerUuid;
    private final boolean verbose;
    private long resumeAt = -1L;

    private ScriptVM(List<Insn> code, UUID ownerUuid, boolean verbose) {
        this.code = code;
        this.ownerUuid = ownerUuid;
        this.verbose = verbose;
    }

    // ---------------------------------------------------------------- registry

    private static final Map<UUID, ScriptVM> ACTIVE = new HashMap<>();

    public static void start(ServerPlayer holder, String source, boolean verbose) {
        if (holder == null) return;
        cancel(holder.getUUID());
        List<Insn> code;
        try {
            code = ScriptCompiler.compile(source);
        } catch (RuntimeException ex) {
            // Compile errors ALWAYS surface — user needs to see them.
            holder.displayClientMessage(
                    Component.literal("§c[script] erro de compilação: " + ex.getMessage()),
                    false);
            return;
        }
        ScriptVM vm = new ScriptVM(code, holder.getUUID(), verbose);
        ACTIVE.put(holder.getUUID(), vm);
        // Action-bar status is always visible (transient, not chat history)
        // so the user can confirm the script actually ran without enabling verbose.
        holder.displayClientMessage(
                Component.literal("§b▶ script iniciado (" + code.size() + " inst.)")
                        .withStyle(ChatFormatting.AQUA), true);
        vm.runSlice(holder);
    }

    public static void cancel(UUID owner) {
        ACTIVE.remove(owner);
    }

    // ---------------------------------------------------------------- runtime

    private void runSlice(ServerPlayer holder) {
        if (holder == null) { ACTIVE.remove(ownerUuid); return; }
        int slice = 0;
        try {
            while (pc < code.size()) {
                if (slice >= SLICE_BUDGET) {
                    resumeAt = holder.server.getTickCount() + 1;
                    return;
                }
                if (totalSteps++ >= TOTAL_BUDGET) {
                    holder.displayClientMessage(
                            Component.literal("§c[script] abortado: orçamento de instruções esgotado.")
                                    .withStyle(ChatFormatting.RED), false);
                    ACTIVE.remove(ownerUuid);
                    return;
                }
                Insn ins = code.get(pc++);
                if (step(ins, holder)) {
                    return; // wait — VM is parked
                }
                slice++;
            }
        } catch (RuntimeException ex) {
            // Runtime errors ALWAYS surface.
            holder.displayClientMessage(
                    Component.literal("§c[script] erro em runtime: " + ex.getMessage())
                            .withStyle(ChatFormatting.RED), false);
            ACTIVE.remove(ownerUuid);
            return;
        }
        holder.displayClientMessage(
                Component.literal("§a✓ script finalizado")
                        .withStyle(ChatFormatting.GREEN), true);
        ACTIVE.remove(ownerUuid);
    }

    private boolean step(Insn ins, ServerPlayer holder) {
        switch (ins.op) {
            case PUSH -> push(ins.payload);
            case LOAD -> push(vars.getOrDefault((String) ins.payload, 0d));
            case STORE -> vars.put((String) ins.payload, pop());
            case ADD -> { Object b = pop(); Object a = pop(); push(addOp(a, b)); }
            case SUB -> { double b = num(pop()); double a = num(pop()); push(a - b); }
            case MUL -> { double b = num(pop()); double a = num(pop()); push(a * b); }
            case DIV -> { double b = num(pop()); double a = num(pop()); push(b == 0 ? 0d : a / b); }
            case MOD -> { double b = num(pop()); double a = num(pop()); push(b == 0 ? 0d : a % b); }
            case EQ  -> { Object b = pop(); Object a = pop(); push(equalsLoose(a, b)); }
            case NEQ -> { Object b = pop(); Object a = pop(); push(!equalsLoose(a, b)); }
            case LT  -> { double b = num(pop()); double a = num(pop()); push(a <  b); }
            case LE  -> { double b = num(pop()); double a = num(pop()); push(a <= b); }
            case GT  -> { double b = num(pop()); double a = num(pop()); push(a >  b); }
            case GE  -> { double b = num(pop()); double a = num(pop()); push(a >= b); }
            case AND -> { boolean b = bool(pop()); boolean a = bool(pop()); push(a && b); }
            case OR  -> { boolean b = bool(pop()); boolean a = bool(pop()); push(a || b); }
            case NOT -> push(!bool(pop()));
            case NEG -> push(-num(pop()));
            case JMP -> pc = ins.target();
            case JFALSE -> { if (!bool(pop())) pc = ins.target(); }
            case RUN -> {
                String resolved = interpolate((String) ins.payload);
                budget(resolved);
                int rc = CommandRunner.runAs(holder, resolved, verbose);
                // Even in silent mode, surface the FIRST dispatcher failure
                // briefly on the action bar so the user knows something is wrong.
                if (!verbose && rc <= 0) {
                    holder.displayClientMessage(
                            Component.literal("§c! comando falhou: §7" + truncate(resolved, 60)
                                    + " §7(ative Verbose pra ver o erro)"), true);
                }
            }
            case SAY -> {
                String resolved = interpolate((String) ins.payload);
                budget(resolved);
                CommandRunner.runAs(holder, "say " + resolved, verbose);
            }
            case WAIT -> {
                int ticks = (int) Math.max(1, num(pop()));
                resumeAt = holder.server.getTickCount() + ticks;
                return true;
            }
            case HALT -> pc = code.size();
        }
        return false;
    }

    private void budget(String s) {
        totalOutput += s.length();
        if (totalOutput > OUTPUT_BUDGET) {
            throw new RuntimeException("orçamento de saída esgotado");
        }
    }

    private String interpolate(String tpl) {
        if (tpl == null || tpl.isEmpty()) return "";
        Matcher m = VAR_PATTERN.matcher(tpl);
        StringBuilder out = new StringBuilder(tpl.length());
        while (m.find()) {
            String name = m.group(1);
            Object v = vars.get(name);
            String repl = v == null ? m.group(0) : formatValue(v);
            m.appendReplacement(out, Matcher.quoteReplacement(repl));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String formatValue(Object v) {
        if (v instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return Long.toString((long) (double) d);
            }
            return d.toString();
        }
        return String.valueOf(v);
    }

    private void push(Object v) {
        if (stack.size() >= STACK_LIMIT) throw new RuntimeException("stack overflow");
        stack.push(v);
    }

    private Object pop() {
        if (stack.isEmpty()) throw new RuntimeException("stack underflow");
        return stack.pop();
    }

    private static double num(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof Boolean b) return b ? 1d : 0d;
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception ex) { return 0d; }
        }
        return 0d;
    }

    private static boolean bool(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.doubleValue() != 0d;
        if (o instanceof String s) return !s.isEmpty();
        return false;
    }

    private static Object addOp(Object a, Object b) {
        if (a instanceof String || b instanceof String) {
            return formatValue(a) + formatValue(b);
        }
        return num(a) + num(b);
    }

    private static boolean equalsLoose(Object a, Object b) {
        if (a instanceof Number || b instanceof Number) return num(a) == num(b);
        return String.valueOf(a).equals(String.valueOf(b));
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        // Snapshot to avoid ConcurrentModificationException — runSlice may
        // remove the entry from ACTIVE when it reaches HALT.
        List<UUID> ready = new ArrayList<>();
        long now = ev.getServer().getTickCount();
        for (Map.Entry<UUID, ScriptVM> e : ACTIVE.entrySet()) {
            ScriptVM vm = e.getValue();
            if (vm.resumeAt >= 0 && now >= vm.resumeAt) ready.add(e.getKey());
        }
        for (UUID u : ready) {
            ScriptVM vm = ACTIVE.get(u);
            if (vm == null) continue;
            ServerPlayer holder = ev.getServer().getPlayerList().getPlayer(u);
            if (holder == null) {
                ACTIVE.remove(u);
                continue;
            }
            vm.resumeAt = -1L;
            vm.runSlice(holder);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent ev) {
        ACTIVE.clear();
    }
}
