package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ClearMatterShieldItem extends Item {
    public ClearMatterShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer) {
            Direction facing = player.getDirection();
            BlockPos center = player.blockPosition().relative(facing, 2);

            int placed = 0;
            Direction perpendicular = facing.getClockWise();

            for (int h = 0; h < 3; h++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos wallPos = center.relative(perpendicular, w).above(h);
                    if (level.isEmptyBlock(wallPos) || level.getBlockState(wallPos).canBeReplaced()) {
                        level.setBlockAndUpdate(wallPos, ModBlocks.CLEAR_MATTER_BLOCK.get().defaultBlockState());
                        placed++;
                    }
                }
            }

            if (placed > 0) {
                level.playSound(null, center, ModSounds.CLEAR_HUM.get(),
                        SoundSource.BLOCKS, 1.0F, 0.8F);
                player.displayClientMessage(
                        Component.literal("§b[Escudo] Barreira de Matéria Clara implantada! (§f" + placed + " blocos§b)"),
                        true
                );

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                player.getCooldowns().addCooldown(this, 60);
            } else {
                player.displayClientMessage(
                        Component.literal("§c[Escudo] Não há espaço para implantar a barreira!"),
                        true
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
