package br.com.murilo.liberthia.item.script;

import br.com.murilo.liberthia.item.CommandRunner;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenScriptTabletScreenS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Op-only programmable item that interprets LiberScript — a tiny scripting
 * language with {@code if/else}, {@code while}, {@code repeat}, variables,
 * arithmetic, string interpolation and {@code wait <ticks>}.
 *
 *   • SHIFT + right-click in air → opens the editor.
 *   • Right-click in air → compiles and runs the script.
 *
 * The source lives entirely in the item's NBT; {@link ScriptVM} runs one VM
 * per holder UUID and parks across {@code wait} statements via the server tick
 * loop.
 */
public class ScriptTabletItem extends Item {
    public static final String NBT_SOURCE = "Source";
    public static final String NBT_LABEL = "Label";
    public static final String NBT_VERBOSE = "Verbose";

    public ScriptTabletItem(Properties props) {
        super(props);
    }

    public static String readSource(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(NBT_SOURCE);
    }

    public static void writeSource(ItemStack stack, String src) {
        if (src == null) src = "";
        if (src.length() > 16_000) src = src.substring(0, 16_000);
        stack.getOrCreateTag().putString(NBT_SOURCE, src);
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
        if (label.isEmpty()) return base;
        return Component.literal("§d[" + label + "]§r ").append(base);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        if (!CommandRunner.isOp(sp)) {
            sp.displayClientMessage(
                    Component.literal("Apenas OPs podem usar o Tablete de Script.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (sp.isShiftKeyDown()) {
            ModNetwork.sendToPlayer(sp,
                    new OpenScriptTabletScreenS2CPacket(
                            readSource(stack), readLabel(stack), readVerbose(stack)));
            return InteractionResultHolder.success(stack);
        }

        String src = readSource(stack);
        if (src.isBlank()) {
            sp.displayClientMessage(
                    Component.literal("Tablete de script vazio — SHIFT + clique para programar.")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }
        ScriptVM.start(sp, src, readVerbose(stack));
        sp.getCooldowns().addCooldown(this, 5);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tip, TooltipFlag flag) {
        String src = readSource(stack);
        int lines = src.isEmpty() ? 0 : src.split("\\R", -1).length;
        tip.add(Component.literal("§7Linhas: §f" + lines));
        tip.add(Component.literal("§7Saída: " + (readVerbose(stack) ? "§averbosa" : "§csilenciosa")));
        tip.add(Component.literal("§7SHIFT + clique direito: §feditar"));
        tip.add(Component.literal("§7clique direito: §fexecutar script"));
        tip.add(Component.literal("§7Suporta §fif/else/while/repeat/let/wait/say/run"));
    }
}
