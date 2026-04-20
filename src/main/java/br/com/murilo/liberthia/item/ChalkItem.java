package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Chalk — draws a containment symbol on the ground by placing a flat symbol block.
 * 4 symbols around a proliferation mother block stop its spread.
 */
public class ChalkItem extends Item {

    public ChalkItem(Properties props) {
        super(props.durability(32));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        BlockPos above = clicked.above();
        Player player = ctx.getPlayer();

        if (player == null) return InteractionResult.PASS;

        BlockState groundBelow = level.getBlockState(clicked);
        BlockState targetSpot = level.getBlockState(above);

        // Only place on top of solid block, into an empty/replaceable space
        if (!groundBelow.isSolidRender(level, clicked)) return InteractionResult.PASS;
        if (!targetSpot.isAir() && !targetSpot.canBeReplaced()) return InteractionResult.PASS;

        if (!level.isClientSide) {
            level.setBlockAndUpdate(above, ModBlocks.CHALK_SYMBOL.get().defaultBlockState());
            level.playSound(null, above, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
            ctx.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Desenha um símbolo de contenção no chão.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8Coloque 4 em volta de um bloco-mãe para conter a proliferação.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
