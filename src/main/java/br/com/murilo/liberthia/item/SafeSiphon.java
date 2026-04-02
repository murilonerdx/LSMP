package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class SafeSiphon extends BucketItem {
    public SafeSiphon(Properties properties) {
        super(ModFluids.DARK_MATTER, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(5.0D, 0.0F, true);
        
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos pos = blockHit.getBlockPos();
            Fluid fluid = level.getFluidState(pos).getType();
            
            if (fluid.isSame(ModFluids.DARK_MATTER.get()) || fluid.isSame(ModFluids.FLOWING_DARK_MATTER.get())) {
                if (!level.isClientSide) {
                    level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    player.displayClientMessage(Component.literal("§5☢ Fluido Coletado com Segurança"), true);
                }
                return InteractionResultHolder.success(stack);
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }
}
