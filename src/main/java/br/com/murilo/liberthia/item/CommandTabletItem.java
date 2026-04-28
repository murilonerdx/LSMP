package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.CommandScheduler;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenCommandTabletScreenS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Programmable item — like a portable command block:
 *
 *   • SHIFT + right-click in air → opens the editor (op-only).
 *   • Right-click in air → executes the saved commands at OP level.
 *   • Right-click on a block → executes commands at the clicked position.
 *
 * Each saved line is a single command (no leading slash needed). Lines may
 * be prefixed with {@code @<ticks> } to delay execution by N ticks (relative
 * to the activation moment). Lines starting with {@code #} are treated as
 * comments. Lines starting with {@code @loop <ticks>} (only on first line)
 * mark this tablet as repeating: it re-fires its block every N ticks until
 * the holder right-clicks again or unequips it. (Loops are intentionally
 * limited to held tablets so they auto-stop on relog.)
 */
public class CommandTabletItem extends Item {
    public static final String NBT_COMMANDS = "Commands";
    public static final String NBT_LABEL = "Label";
    public static final String NBT_LOOP_ID = "LoopId";
    public static final String NBT_TARGET_MODE = "TargetMode";   // "self" | "other"
    public static final String NBT_TARGET_NAME = "TargetName";   // player name when mode=other
    public static final String NBT_VERBOSE = "Verbose";          // boolean — show command output in chat

    public static final String MODE_SELF = "self";
    public static final String MODE_OTHER = "other";

    public CommandTabletItem(Properties props) {
        super(props);
    }

    public static List<String> readCommands(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_COMMANDS, Tag.TAG_LIST)) {
            return Collections.emptyList();
        }
        ListTag list = tag.getList(NBT_COMMANDS, Tag.TAG_STRING);
        List<String> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    public static void writeCommands(ItemStack stack, List<String> commands) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = new ListTag();
        int n = Math.min(commands.size(), 32);
        for (int i = 0; i < n; i++) {
            String s = commands.get(i);
            if (s == null) s = "";
            if (s.length() > 256) s = s.substring(0, 256);
            list.add(StringTag.valueOf(s));
        }
        tag.put(NBT_COMMANDS, list);
    }

    public static String readLabel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(NBT_LABEL);
    }

    public static void writeLabel(ItemStack stack, String label) {
        if (label == null) label = "";
        if (label.length() > 64) label = label.substring(0, 64);
        stack.getOrCreateTag().putString(NBT_LABEL, label);
    }

    public static String readTargetMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return MODE_SELF;
        String m = tag.getString(NBT_TARGET_MODE);
        return MODE_OTHER.equalsIgnoreCase(m) ? MODE_OTHER : MODE_SELF;
    }

    public static String readTargetName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(NBT_TARGET_NAME);
    }

    public static void writeTarget(ItemStack stack, String mode, String name) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(NBT_TARGET_MODE, MODE_OTHER.equalsIgnoreCase(mode) ? MODE_OTHER : MODE_SELF);
        if (name == null) name = "";
        if (name.length() > 32) name = name.substring(0, 32);
        tag.putString(NBT_TARGET_NAME, name);
    }

    public static boolean readVerbose(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(NBT_VERBOSE);
    }

    public static void writeVerbose(ItemStack stack, boolean verbose) {
        stack.getOrCreateTag().putBoolean(NBT_VERBOSE, verbose);
    }

    @Override
    public Component getName(ItemStack stack) {
        String label = readLabel(stack);
        Component base = super.getName(stack);
        String prefix = "";
        if (!label.isEmpty()) prefix += "§b[" + label + "]§r ";
        String mode = readTargetMode(stack);
        if (MODE_OTHER.equals(mode)) {
            String name = readTargetName(stack);
            prefix += "§e→" + (name.isEmpty() ? "?" : name) + "§r ";
        }
        if (prefix.isEmpty()) return base;
        return Component.literal(prefix).append(base);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        if (!CommandRunner.isOp(sp)) {
            sp.displayClientMessage(
                    Component.literal("Apenas OPs podem usar o Tablete de Comando.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (sp.isShiftKeyDown()) {
            // Open the editor.
            ModNetwork.sendToPlayer(sp,
                    new OpenCommandTabletScreenS2CPacket(
                            readCommands(stack), readLabel(stack),
                            readTargetMode(stack), readTargetName(stack),
                            readVerbose(stack)));
            return InteractionResultHolder.success(stack);
        }

        executeAt(sp, stack, sp.position(), null);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS; // let `use()` handle editor
        Level level = ctx.getLevel();
        // Don't intercept clicks on a Command Pylon — let the block's use()
        // handle imprinting/firing instead of running the tablet program.
        if (level.getBlockState(ctx.getClickedPos()).getBlock()
                instanceof br.com.murilo.liberthia.logic.CommandPylonBlock) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!CommandRunner.isOp(sp)) {
            sp.displayClientMessage(
                    Component.literal("Apenas OPs podem usar o Tablete de Comando.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        ItemStack stack = ctx.getItemInHand();
        executeAt(sp, stack, net.minecraft.world.phys.Vec3.atCenterOf(ctx.getClickedPos()), ctx.getClickedPos());
        return InteractionResult.CONSUME;
    }

    private void executeAt(ServerPlayer sp, ItemStack stack,
                           net.minecraft.world.phys.Vec3 pos,
                           @Nullable net.minecraft.core.BlockPos clickedPos) {
        List<String> cmds = readCommands(stack);
        if (cmds.isEmpty()) {
            sp.displayClientMessage(
                    Component.literal("Tablete vazio — segure SHIFT e clique para programar.")
                            .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        ServerLevel sl = (ServerLevel) sp.level();
        boolean verbose = readVerbose(stack);

        // Resolve target: SELF → caller; OTHER → online player by name (case-insensitive).
        String mode = readTargetMode(stack);
        String targetName = readTargetName(stack);
        ServerPlayer actor = sp;
        if (MODE_OTHER.equals(mode)) {
            ServerPlayer match = null;
            if (!targetName.isEmpty()) {
                for (ServerPlayer p : sp.server.getPlayerList().getPlayers()) {
                    if (p.getGameProfile().getName().equalsIgnoreCase(targetName)) {
                        match = p; break;
                    }
                }
            }
            if (match == null) {
                sp.displayClientMessage(
                        Component.literal("Alvo \"" + targetName + "\" não está online.")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }
            actor = match;
        }
        String resolvedTargetName = actor.getGameProfile().getName();

        int delay = 0;
        int fired = 0;
        for (String raw : cmds) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("@")) {
                int sp2 = line.indexOf(' ');
                if (sp2 > 1) {
                    String tag = line.substring(1, sp2);
                    String rest = line.substring(sp2 + 1).trim();
                    if (tag.equalsIgnoreCase("loop")) continue; // ignore loop markers in single-shot
                    try {
                        int t = Integer.parseInt(tag);
                        delay += Math.max(0, t);
                        line = rest;
                    } catch (NumberFormatException ignored) { /* fall through, treat as command */ }
                }
            }

            String resolved = replaceTokens(line, pos, clickedPos)
                    .replace("{target}", resolvedTargetName);

            if (delay <= 0) {
                CommandRunner.runAs(actor, resolved, verbose);
            } else {
                CommandScheduler.schedule(sl, actor, resolved, delay, verbose);
            }
            fired++;
        }

        sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        if (verbose) {
            String suffix = MODE_OTHER.equals(mode) ? " em " + resolvedTargetName : "";
            sp.displayClientMessage(
                    Component.literal("Tablete executado (" + fired + " comandos)" + suffix + ".")
                            .withStyle(ChatFormatting.AQUA), true);
        }
        sp.getCooldowns().addCooldown(this, 5);
    }

    /** Substitute simple positional tokens for convenience. */
    private static String replaceTokens(String line,
                                        net.minecraft.world.phys.Vec3 pos,
                                        @Nullable net.minecraft.core.BlockPos clicked) {
        String px = String.format(java.util.Locale.ROOT, "%.2f", pos.x);
        String py = String.format(java.util.Locale.ROOT, "%.2f", pos.y);
        String pz = String.format(java.util.Locale.ROOT, "%.2f", pos.z);
        line = line.replace("{x}", px).replace("{y}", py).replace("{z}", pz);
        if (clicked != null) {
            line = line.replace("{bx}", String.valueOf(clicked.getX()))
                    .replace("{by}", String.valueOf(clicked.getY()))
                    .replace("{bz}", String.valueOf(clicked.getZ()));
        }
        return line;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        List<String> cmds = readCommands(stack);
        tip.add(Component.literal("§7Linhas salvas: §f" + cmds.size()));
        String mode = readTargetMode(stack);
        if (MODE_OTHER.equals(mode)) {
            String name = readTargetName(stack);
            tip.add(Component.literal("§7Alvo: §e" + (name.isEmpty() ? "(sem nome)" : name)));
        } else {
            tip.add(Component.literal("§7Alvo: §aSelf"));
        }
        tip.add(Component.literal("§7Saída: " + (readVerbose(stack) ? "§averbosa" : "§csilenciosa")));
        tip.add(Component.literal("§7SHIFT + clique direito: §feditar"));
        tip.add(Component.literal("§7clique direito: §fexecutar"));
        tip.add(Component.literal("§7Use §f@<ticks> §7para atrasar uma linha."));
        tip.add(Component.literal("§7Token §f{target}§7 = nome do alvo."));
        if (flag.isAdvanced() && !cmds.isEmpty()) {
            int max = Math.min(3, cmds.size());
            for (int i = 0; i < max; i++) {
                tip.add(Component.literal("§8• " + cmds.get(i)));
            }
            if (cmds.size() > max) tip.add(Component.literal("§8…"));
        }
    }
}
