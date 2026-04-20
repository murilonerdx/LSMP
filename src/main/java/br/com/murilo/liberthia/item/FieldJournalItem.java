package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FieldJournalItem extends Item {
    public static final int MAX_PAGES = 10;
    private static final String TAG_PAGES = "journal_pages";
    private static final String TAG_TITLE = "journal_title";
    private static final String TAG_READ_PAGE = "read_page";

    public FieldJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                // Edit mode — only ops can edit but GUI check is server-side via packet
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.item.client.ClientScreenHelper.openFieldJournal(hand));
            } else {
                // Read mode — show current page
                displayCurrentPage(player, stack);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void displayCurrentPage(Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        List<String> pages = getPages(tag);
        if (pages.isEmpty()) {
            player.displayClientMessage(Component.literal("Diário vazio — Shift+Clique para escrever")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
            return;
        }

        int page = tag.getInt(TAG_READ_PAGE);
        if (page >= pages.size()) page = 0;

        String title = tag.getString(TAG_TITLE);
        if (!title.isEmpty()) {
            player.displayClientMessage(Component.literal("=== " + title + " ===")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        }
        player.displayClientMessage(Component.literal("[ " + (page + 1) + "/" + pages.size() + " ]")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
        player.displayClientMessage(Component.literal(""), false);

        // Split page text by \n and display each line
        String text = pages.get(page);
        for (String line : text.split("\n")) {
            player.displayClientMessage(Component.literal(line).withStyle(ChatFormatting.GRAY), false);
        }

        tag.putInt(TAG_READ_PAGE, (page + 1) % pages.size());
    }

    public static List<String> getPages(CompoundTag tag) {
        List<String> pages = new ArrayList<>();
        if (tag.contains(TAG_PAGES, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_PAGES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                pages.add(list.getString(i));
            }
        }
        return pages;
    }

    public static void setPages(CompoundTag tag, List<String> pages, String title) {
        ListTag list = new ListTag();
        int count = Math.min(pages.size(), MAX_PAGES);
        for (int i = 0; i < count; i++) {
            list.add(StringTag.valueOf(pages.get(i)));
        }
        tag.put(TAG_PAGES, list);
        tag.putString(TAG_TITLE, title);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getOrCreateTag();
        String title = tag.getString(TAG_TITLE);
        if (!title.isEmpty()) {
            tooltip.add(Component.literal(title).withStyle(ChatFormatting.DARK_PURPLE));
        }
        List<String> pages = getPages(tag);
        tooltip.add(Component.literal(pages.size() + " páginas").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Clique: ler | Shift+Clique: editar").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getOrCreateTag().contains(TAG_PAGES);
    }
}
