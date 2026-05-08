package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.world.RiftSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * Bússola Dimensional — aponta para o RIFT dimensional mais próximo,
 * armazenado em {@link RiftSavedData} (persistente por mundo).
 *
 * <p>Right-click escaneia os rifts da dimensão atual e salva a posição
 * do mais próximo no NBT do item. Mostra coordenadas e distância no chat.
 */
public class DimensionalCompassItem extends Item {

    public static final String TAG_X = "TargetX";
    public static final String TAG_Y = "TargetY";
    public static final String TAG_Z = "TargetZ";
    public static final String TAG_DIST = "TargetDist";

    public DimensionalCompassItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel sl))
            return InteractionResultHolder.success(stack);

        BlockPos origin = player.blockPosition();
        RiftSavedData data = RiftSavedData.get(sl);
        BlockPos nearest = data.findNearest(origin);

        if (nearest != null) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt(TAG_X, nearest.getX());
            tag.putInt(TAG_Y, nearest.getY());
            tag.putInt(TAG_Z, nearest.getZ());
            int dist = (int) Math.sqrt(origin.distSqr(nearest));
            tag.putInt(TAG_DIST, dist);

            player.displayClientMessage(
                    Component.literal("⌖ Rift Dimensional: " + nearest.getX() + ", "
                            + nearest.getY() + ", " + nearest.getZ()
                            + " (" + dist + "m)")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            player.getCooldowns().addCooldown(this, 40);
        } else {
            player.displayClientMessage(
                    Component.literal("Nenhum rift detectado nesta dimensão")
                            .withStyle(ChatFormatting.GRAY), true);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains(TAG_X)) {
            CompoundTag tag = stack.getTag();
            tooltip.add(Component.literal("Rift: " + tag.getInt(TAG_X) + ", "
                    + tag.getInt(TAG_Y) + ", " + tag.getInt(TAG_Z))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            if (tag.contains(TAG_DIST)) {
                tooltip.add(Component.literal("Distância: " + tag.getInt(TAG_DIST) + " blocos")
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            tooltip.add(Component.literal("Right-click pra localizar rift").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_X);
    }
}
