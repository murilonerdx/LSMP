package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WorkerBadgeItem extends Item {
    public WorkerBadgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("LIBERTHIA RESEARCH FACILITY").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Worker Identification").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Status: INFECTED").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("Clearance: STANDARD").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
