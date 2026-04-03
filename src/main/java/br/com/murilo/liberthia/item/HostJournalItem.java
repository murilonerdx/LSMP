package br.com.murilo.liberthia.item;

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

public class HostJournalItem extends Item {
    private static final int MAX_PAGES = 7;

    public HostJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            int page = tag.getInt("journal_page");

            displayPage(player, page);

            // Advance to next page (cycle)
            tag.putInt("journal_page", (page + 1) % MAX_PAGES);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void displayPage(Player player, int page) {
        player.displayClientMessage(Component.literal(""), false);

        switch (page) {
            case 0 -> { // The Three Islands
                player.displayClientMessage(Component.literal("=== The Host's Journal: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("[ Page 1/7 - The Three Secret Islands ]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("Day 1 - Three islands exist, linked to three dimensions.").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.literal("Their origin remains uncertain. None belong to our world.").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.literal("Liberthia is the largest, the most habitable, the most diverse.").withStyle(ChatFormatting.WHITE), false);
                player.displayClientMessage(Component.literal("It is the first we discovered. It was here I moved my family.").withStyle(ChatFormatting.WHITE), false);
            }
            case 1 -> { // Liberthia
                player.displayClientMessage(Component.literal("=== The Host's Journal: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("[ Page 2/7 - The Island of Liberthia ]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("Day 7 - Research facilities are now operational across the island.").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.literal("Workers travel between installations constantly.").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.literal("Liberthia hosts all three known matters simultaneously:").withStyle(ChatFormatting.WHITE), false);
                player.displayClientMessage(Component.literal("  Dark Matter, Clear Matter, and Yellow Matter.").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("No other island shows such coexistence. This is unprecedented.").withStyle(ChatFormatting.GRAY), false);
            }
            case 2 -> { // Dark Matter
                player.displayClientMessage(Component.literal("=== The Host's Journal: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("[ Page 3/7 - Dark Matter ]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("Day 14 - Dark Matter is the most powerful of the three.").withStyle(ChatFormatting.DARK_RED), false);
                player.displayClientMessage(Component.literal("It distorts reality itself, forging hostile environments").withStyle(ChatFormatting.DARK_RED), false);
                player.displayClientMessage(Component.literal("where chaos and destruction dominate.").withStyle(ChatFormatting.DARK_RED), false);
                player.displayClientMessage(Component.literal("It infects any living form, turning the host into a puppet").withStyle(ChatFormatting.RED), false);
                player.displayClientMessage(Component.literal("of an Entity that seems to control it.").withStyle(ChatFormatting.RED), false);
                player.displayClientMessage(Component.literal("When manipulated correctly, it creates beings from nothing.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false);
            }
            case 3 -> { // Clear Matter
                player.displayClientMessage(Component.literal("=== The Host's Journal: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("[ Page 4/7 - Clear Matter ]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("Day 47 - Clear Matter appears harmless on its own.").withStyle(ChatFormatting.AQUA), false);
                player.displayClientMessage(Component.literal("No observable effects on test subjects... initially.").withStyle(ChatFormatting.AQUA), false);
                player.displayClientMessage(Component.literal("But combined with Dark Matter, a consciousness EMERGES.").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("The host becomes a malign being - not purely destructive,").withStyle(ChatFormatting.DARK_AQUA), false);
                player.displayClientMessage(Component.literal("but possessing its own will, motivations, and abilities.").withStyle(ChatFormatting.DARK_AQUA), false);
            }
            case 4 -> { // Yellow Matter
                player.displayClientMessage(Component.literal("=== The Host's Journal: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("[ Page 5/7 - Yellow Matter ]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("Day 89 - Yellow Matter is the least studied.").withStyle(ChatFormatting.GOLD), false);
                player.displayClientMessage(Component.literal("It and Dark Matter COMPLETELY REPEL each other.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("They are incapable of connecting in any form.").withStyle(ChatFormatting.GOLD), false);
                player.displayClientMessage(Component.literal("Yet when Yellow is combined with Clear inside a host,").withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("independent entities emerge - much like Dark+Clear.").withStyle(ChatFormatting.YELLOW), false);
            }
            case 5 -> { // Yellow + Clear specifics
                player.displayClientMessage(Component.literal("=== The Host's Journal: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("[ Page 6/7 - The Yellow-Clear Bond ]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("Day 112 - Crucial difference from Dark+Clear fusion:").withStyle(ChatFormatting.GOLD), false);
                player.displayClientMessage(Component.literal("The host's original motivations remain INTACT.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("Yellow does not influence the mind like Dark does.").withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("With Clear, it amplifies preexisting emotions subtly,").withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("causing occasional lapses of emotional control.").withStyle(ChatFormatting.YELLOW), false);
                player.displayClientMessage(Component.literal("Despite loathing Dark Matter, Yellow+Clear shows").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal("elevated strategic and rational combat capability.").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            }
            case 6 -> { // The Host's note
                player.displayClientMessage(Component.literal("=== The Host's Journal: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("[ Page 7/7 - Final Notes ]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("Day 200+ - The pattern is clear now.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                player.displayClientMessage(Component.literal("Clear Matter is the BRIDGE between Dark and Yellow.").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), false);
                player.displayClientMessage(Component.literal("Without it, the two extremes cannot interact at all.").withStyle(ChatFormatting.WHITE), false);
                player.displayClientMessage(Component.literal("The Infuser is our only machine that combines all three.").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.literal("The Matter Core... if we can stabilize it...").withStyle(ChatFormatting.GRAY), false);
                player.displayClientMessage(Component.literal(""), false);
                player.displayClientMessage(Component.literal("I fear the Entity is watching. Something stirs beneath.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED), false);
                player.displayClientMessage(Component.literal("   ...the island is alive.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
            }
        }

        player.displayClientMessage(Component.literal(""), false);
        player.displayClientMessage(Component.literal("[Right-click again to turn the page]").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.liberthia.host_journal.line1").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.liberthia.host_journal.line2").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.liberthia.host_journal.line3").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        CompoundTag tag = stack.getOrCreateTag();
        int page = tag.getInt("journal_page") + 1;
        tooltip.add(Component.literal("Page " + page + "/" + MAX_PAGES).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
