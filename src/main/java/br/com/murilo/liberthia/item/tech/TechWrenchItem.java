package br.com.murilo.liberthia.item.tech;

import br.com.murilo.liberthia.block.ChargerBlock;
import br.com.murilo.liberthia.block.EnergyCellBlock;
import br.com.murilo.liberthia.block.TechGeneratorBlock;
import br.com.murilo.liberthia.block.TechMachineBlock;
import br.com.murilo.liberthia.block.entity.TechMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

/**
 * r180c — <b>Chave Tech</b> (wrench): clique = rotaciona blocos direcionais
 * (FACING/HORIZONTAL_FACING/AXIS). Agachado + clique num bloco tech = desmonta
 * (devolve o item + conteúdo da máquina). Estilo Mekanism Configurator.
 */
public class TechWrenchItem extends Item {
    public TechWrenchItem() { super(new Item.Properties().stacksTo(1).durability(800).rarity(Rarity.UNCOMMON)); }

    private static boolean isTechBlock(Block b) {
        return b instanceof TechMachineBlock || b instanceof EnergyCellBlock
                || b instanceof TechGeneratorBlock || b instanceof ChargerBlock;
    }

    @Override public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // desmontar (agachado, bloco tech)
        if (ctx.getPlayer() != null && ctx.getPlayer().isSecondaryUseActive() && isTechBlock(block)) {
            if (level instanceof ServerLevel sl) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TechMachineBlockEntity tm) tm.drops();
                List<ItemStack> drops = Block.getDrops(state, sl, pos, be, ctx.getPlayer(), ctx.getItemInHand());
                level.removeBlock(pos, false);
                for (ItemStack d : drops) Block.popResource(level, pos, d);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.2F);
                ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), pl -> {});
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // rotacionar (qualquer bloco direcional)
        for (Property<?> prop : new Property[]{
                BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.AXIS}) {
            if (state.hasProperty(prop)) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.cycle(prop), Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.4F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return InteractionResult.PASS;
    }

    @Override public boolean isFoil(ItemStack s) { return false; }
}
