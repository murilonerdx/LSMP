package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HostJournalItem extends Item {
    public HostJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            // Display lore pages as chat messages for now
            player.displayClientMessage(Component.literal("=== The Host's Journal ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Day 1 - The island appeared from nowhere.").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.literal("Three types of matter coexist here:").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.literal("Dark, Clear, and Yellow.").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Day 14 - Dark Matter is unstable.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("It distorts reality. Transforms living beings.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("The workers... they are changing.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Day 47 - Clear Matter balances the infection.").withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.literal("Without it, we would all be lost.").withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.literal("I can feel it in my veins now.").withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.literal("Both Dark and Light. Coexisting.").withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Day 112 - The test subjects arrive today.").withStyle(ChatFormatting.GOLD), false);
            player.displayClientMessage(Component.literal("They don't know what Liberthia really is.").withStyle(ChatFormatting.GOLD), false);
            player.displayClientMessage(Component.literal("Not yet.").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("A worn leather journal").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("belonging to The Host").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("Right-click to read").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
