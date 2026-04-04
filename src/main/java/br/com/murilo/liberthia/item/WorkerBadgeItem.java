package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModCapabilities;
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

public class WorkerBadgeItem extends Item {

    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_TOTAL_EXPOSURE_TICKS = "TotalExposureTicks";
    private static final String TAG_MAX_INFECTION_SEEN = "MaxInfectionSeen";
    private static final String TAG_MUTATIONS_DISCOVERED = "MutationsDiscovered";

    public WorkerBadgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();

            // Bind to player on first use
            if (!tag.contains(TAG_OWNER_UUID)) {
                tag.putUUID(TAG_OWNER_UUID, player.getUUID());
                tag.putString(TAG_OWNER_NAME, player.getName().getString());
                player.displayClientMessage(
                        Component.literal("Badge bound to: " + player.getName().getString())
                                .withStyle(ChatFormatting.GREEN), true);
            }

            // Update stats from current infection data
            player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                int currentExposure = tag.getInt(TAG_TOTAL_EXPOSURE_TICKS);
                tag.putInt(TAG_TOTAL_EXPOSURE_TICKS, currentExposure + 1);
                int maxInf = Math.max(tag.getInt(TAG_MAX_INFECTION_SEEN), data.getInfection());
                tag.putInt(TAG_MAX_INFECTION_SEEN, maxInf);
                String mutations = data.getMutations();
                int mutCount = (mutations == null || mutations.isEmpty()) ? 0 : mutations.split(",").length;
                tag.putInt(TAG_MUTATIONS_DISCOVERED, mutCount);
            });

            // Show status as chat message
            player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                player.sendSystemMessage(Component.literal("--- WORKER STATUS ---").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                String owner = tag.contains(TAG_OWNER_NAME) ? tag.getString(TAG_OWNER_NAME) : "Unknown";
                player.sendSystemMessage(Component.literal("Worker: " + owner).withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal("Infection: " + data.getInfection() + "%").withStyle(ChatFormatting.DARK_PURPLE));
                player.sendSystemMessage(Component.literal("Stage: " + data.getStage()).withStyle(ChatFormatting.YELLOW));
                String muts = data.getMutations();
                int mutCount = (muts == null || muts.isEmpty()) ? 0 : muts.split(",").length;
                player.sendSystemMessage(Component.literal("Mutations: " + mutCount).withStyle(ChatFormatting.AQUA));
                player.sendSystemMessage(Component.literal("Max Infection Seen: " + tag.getInt(TAG_MAX_INFECTION_SEEN) + "%").withStyle(ChatFormatting.DARK_RED));
                boolean immune = data.isImmune();
                player.sendSystemMessage(Component.literal("Immune: " + (immune ? "YES" : "NO"))
                        .withStyle(immune ? ChatFormatting.GREEN : ChatFormatting.RED));
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.liberthia.worker_badge.facility").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_OWNER_NAME)) {
            tooltip.add(Component.literal("Worker: " + tag.getString(TAG_OWNER_NAME)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Max Infection: " + tag.getInt(TAG_MAX_INFECTION_SEEN) + "%").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("Mutations: " + tag.getInt(TAG_MUTATIONS_DISCOVERED)).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.liberthia.worker_badge.id").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.liberthia.worker_badge.status").withStyle(ChatFormatting.DARK_PURPLE));
        }

        tooltip.add(Component.translatable("tooltip.liberthia.worker_badge.clearance").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.liberthia.worker_badge.note").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
