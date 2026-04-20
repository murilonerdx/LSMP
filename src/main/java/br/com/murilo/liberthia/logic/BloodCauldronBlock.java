package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.block.entity.BloodCauldronBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BloodCauldronBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.block(),
            Shapes.box(0.125, 0.25, 0.125, 0.875, 1.0, 0.875),
            BooleanOp.ONLY_FIRST);

    public BloodCauldronBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BloodCauldronBlockEntity cauldron)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // Empty hand + sneak = dump ingredients
        if (held.isEmpty() && player.isShiftKeyDown()) {
            cauldron.dropAll();
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 1.2F);
            return InteractionResult.CONSUME;
        }

        // Empty hand = attempt craft (needs boiling)
        if (held.isEmpty()) {
            if (!cauldron.isBoiling()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cO caldeirão precisa de Sangue embaixo."),
                        true);
                return InteractionResult.CONSUME;
            }
            ItemStack out = cauldron.tryCraft();
            if (!out.isEmpty()) {
                cauldron.produce(out);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§7Receita desconhecida."),
                    true);
            return InteractionResult.CONSUME;
        }

        // Otherwise add ingredient
        if (cauldron.addIngredient(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.4F, 1.5F);
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§cCaldeirão cheio."),
                true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BloodCauldronBlockEntity cauldron) {
                cauldron.dropAll();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloodCauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.BLOOD_CAULDRON.get() ? BloodCauldronBlockEntity::tick : null;
    }
}
