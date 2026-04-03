package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class YellowMatterPickaxeItem extends PickaxeItem {

    private boolean inInfectedZone = false;

    public YellowMatterPickaxeItem(Properties properties) {
        super(YellowMatterToolMaterial.INSTANCE, 1, -2.8F, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && isSelected && entity instanceof Player) {
            inInfectedZone = isInInfectedZone(level, entity.blockPosition());
        }
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float baseSpeed = super.getDestroySpeed(stack, state);
        // 2x mining speed in infected areas
        return inInfectedZone ? baseSpeed * 2.0F : baseSpeed;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        return super.mineBlock(stack, level, state, pos, miner);
    }

    /**
     * Checks if position is in an infected zone by looking for corrupted_soil or dark_matter nearby.
     */
    public static boolean isInInfectedZone(Level level, BlockPos pos) {
        for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
            Block block = level.getBlockState(neighbor).getBlock();
            if (block == ModBlocks.CORRUPTED_SOIL.get() || block == ModBlocks.DARK_MATTER_BLOCK.get()) {
                return true;
            }
        }
        return false;
    }
}
