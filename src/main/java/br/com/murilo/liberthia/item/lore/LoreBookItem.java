package br.com.murilo.liberthia.item.lore;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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

/**
 * Read-only narrative book. Pages are compiled into the item subclass; there
 * is no editing. Right-click cycles through pages (one displayed at a time
 * via chat to avoid opening a GUI and thus avoid client-class bleed on the
 * dedicated server).
 */
public abstract class LoreBookItem extends Item {
    private static final String TAG_PAGE = "lore_page";

    public LoreBookItem(Properties properties) {
        super(properties);
    }

    /** Human-readable title shown in tooltip and as chat header. */
    protected abstract String title();

    /** Immutable page list. Each page may contain {@code \n} to split lines. */
    protected abstract List<String> pages();

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            List<String> pages = pages();
            if (pages.isEmpty()) {
                return InteractionResultHolder.success(stack);
            }
            CompoundTag tag = stack.getOrCreateTag();
            int page = tag.getInt(TAG_PAGE);
            if (page >= pages.size() || page < 0) page = 0;

            player.displayClientMessage(Component.literal("=== " + title() + " ===")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
            player.displayClientMessage(Component.literal("[ " + (page + 1) + "/" + pages.size() + " ]")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
            for (String line : pages.get(page).split("\n")) {
                player.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.GRAY), false);
            }
            tag.putInt(TAG_PAGE, (page + 1) % pages.size());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(title()).withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(pages().size() + " páginas")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Clique para folhear")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
