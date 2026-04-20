package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Execution Stick — shows GUI (chat-based) to select a marked player and teleport them.
 * Reads marks from Marking Stick in inventory.
 */
public class ExecutionStickItem extends Item {

    public ExecutionStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // Find marking stick in inventory
        ItemStack mark = null;
        int markSlot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() == ModItems.MARKING_STICK.get()) {
                mark = s;
                markSlot = i;
                break;
            }
        }

        if (mark == null) {
            player.sendSystemMessage(Component.translatable("chat.liberthia.exec_no_stick").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        if (!MarkingStickItem.hasPos(mark)) {
            player.sendSystemMessage(Component.translatable("chat.liberthia.exec_no_pos").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }
        List<UUID> marked = MarkingStickItem.getMarkedUUIDs(mark);
        if (marked.isEmpty()) {
            player.sendSystemMessage(Component.translatable("chat.liberthia.exec_no_marked").withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        ServerPlayer sp = (ServerPlayer) player;
        int[] pos = MarkingStickItem.getPos(mark);

        // Header
        sp.sendSystemMessage(Component.literal("§8§m                              "));
        sp.sendSystemMessage(Component.translatable("chat.liberthia.exec_header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        sp.sendSystemMessage(Component.literal("§7TP: §a" + pos[0] + ", " + pos[1] + ", " + pos[2]));
        sp.sendSystemMessage(Component.literal(""));

        // List marked players with clickable [TP] and [X] actions
        for (UUID uuid : marked) {
            ServerPlayer target = level.getServer().getPlayerList().getPlayer(uuid);
            String name = target != null ? target.getGameProfile().getName() : uuid.toString().substring(0, 8);
            String status = target != null ? "§a●" : "§c●";

            MutableComponent line = Component.literal(status + " §f" + name + " ");
            MutableComponent tpBtn = Component.literal("§a[TP]")
                    .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/liberthia execstick tp " + uuid))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Teleportar " + name))));
            MutableComponent unBtn = Component.literal(" §c[X]")
                    .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/liberthia execstick unmark " + uuid))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Desmarcar " + name))));
            line.append(tpBtn).append(unBtn);
            sp.sendSystemMessage(line);
        }

        // Clear all button
        MutableComponent clearAll = Component.literal("§c[LIMPAR TODOS]")
                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/liberthia execstick clearall")));
        sp.sendSystemMessage(Component.literal(""));
        sp.sendSystemMessage(clearAll);
        sp.sendSystemMessage(Component.literal("§8§m                              "));

        if (level instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
            sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.5, 0.5, 0.5);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.execution_stick.desc1").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.liberthia.execution_stick.desc2").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
