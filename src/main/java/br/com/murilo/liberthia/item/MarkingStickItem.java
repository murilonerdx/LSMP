package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Marking Stick — Shift+right-click block = set TP position.
 * Right-click player = add to marked list.
 */
public class MarkingStickItem extends Item {

    public MarkingStickItem(Properties properties) {
        super(properties);
    }

    // Shift+right-click on block → save position
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos().above();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("tpX", pos.getX());
        tag.putInt("tpY", pos.getY());
        tag.putInt("tpZ", pos.getZ());
        tag.putString("tpDim", ctx.getLevel().dimension().location().toString());

        player.sendSystemMessage(Component.translatable("chat.liberthia.mark_pos_set",
                pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GOLD));
        if (ctx.getLevel() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 2.0F);
        }
        return InteractionResult.SUCCESS;
    }

    // Right-click on player → add to list
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(target instanceof Player victim)) return InteractionResult.PASS;

        CompoundTag tag = stack.getOrCreateTag();
        ListTag marked = tag.getList("marked", 8); // string list
        String uuid = victim.getUUID().toString();

        // Check duplicate
        for (int i = 0; i < marked.size(); i++) {
            if (marked.getString(i).equals(uuid)) {
                user.sendSystemMessage(Component.translatable("chat.liberthia.mark_already", victim.getDisplayName())
                        .withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }
        }

        marked.add(net.minecraft.nbt.StringTag.valueOf(uuid));
        tag.put("marked", marked);

        user.sendSystemMessage(Component.translatable("chat.liberthia.mark_added",
                victim.getDisplayName(), marked.size()).withStyle(ChatFormatting.GOLD));

        if (user.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + 1, victim.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
            sl.playSound(null, victim.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
        return InteractionResult.SUCCESS;
    }

    public static List<UUID> getMarkedUUIDs(ItemStack stack) {
        List<UUID> result = new ArrayList<>();
        if (!stack.hasTag()) return result;
        ListTag marked = stack.getTag().getList("marked", 8);
        for (int i = 0; i < marked.size(); i++) {
            try { result.add(UUID.fromString(marked.getString(i))); } catch (Exception ignored) {}
        }
        return result;
    }

    public static boolean hasPos(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("tpX");
    }

    public static int[] getPos(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return new int[]{t.getInt("tpX"), t.getInt("tpY"), t.getInt("tpZ")};
    }

    public static String getDim(ItemStack stack) {
        return stack.getTag().getString("tpDim");
    }

    public static void clearMarked(ItemStack stack) {
        if (stack.hasTag()) stack.getTag().remove("marked");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.marking_stick.desc1").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.liberthia.marking_stick.desc2").withStyle(ChatFormatting.GRAY));
        if (hasPos(stack)) {
            int[] p = getPos(stack);
            tooltip.add(Component.literal("§8Pos: " + p[0] + ", " + p[1] + ", " + p[2]));
        }
        int count = getMarkedUUIDs(stack).size();
        if (count > 0) tooltip.add(Component.literal("§7Marcados: §c" + count));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasPos(stack) || !getMarkedUUIDs(stack).isEmpty();
    }
}
