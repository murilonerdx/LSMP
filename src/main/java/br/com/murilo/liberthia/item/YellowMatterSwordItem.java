package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class YellowMatterSwordItem extends SwordItem {
    public YellowMatterSwordItem(Properties properties) {
        super(YellowMatterToolMaterial.INSTANCE, 3, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (result && !attacker.level().isClientSide) {
            // 30% chance to place yellow_matter_block at entity's feet
            if (attacker.level().getRandom().nextFloat() < 0.3F) {
                BlockPos feetPos = target.blockPosition();
                Level level = target.level();
                BlockState currentState = level.getBlockState(feetPos);

                if (currentState.isAir()) {
                    level.setBlock(feetPos, ModBlocks.YELLOW_MATTER_BLOCK.get().defaultBlockState(), 3);
                }
            }
        }

        return result;
    }
}
