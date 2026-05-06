package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.blockentity.RitualPedestalBlockEntity;
import br.com.murilo.liberthia.item.SealingSealItem;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.ritual.RitualStructureValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RitualPedestalBlock extends BaseEntityBlock {

    public RitualPedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof RitualPedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }

        ItemStack handStack = player.getItemInHand(hand);

        if (handStack.is(ModItems.MAGIC_BOOK.get()) && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                tryPerformRitual((ServerLevel) level, pos, player, pedestal);
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            if (pedestal.hasItem()) {
                ItemStack removed = pedestal.removeStoredItem();

                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }

                player.displayClientMessage(Component.literal("Item removido do pedestal."), true);
                return InteractionResult.CONSUME;
            }

            if (!handStack.isEmpty()) {
                ItemStack one = handStack.copy();
                one.setCount(1);

                pedestal.setStoredItem(one);

                if (!player.getAbilities().instabuild) {
                    handStack.shrink(1);
                }

                player.displayClientMessage(Component.literal("Item colocado no pedestal."), true);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void tryPerformRitual(ServerLevel level, BlockPos pos, Player player, RitualPedestalBlockEntity pedestal) {
        Optional<Component> error = RitualStructureValidator.validate(level, pos);

        if (error.isPresent()) {
            player.displayClientMessage(error.get().copy().withStyle(ChatFormatting.RED), false);
            return;
        }

        if (!pedestal.hasItem()) {
            player.displayClientMessage(Component.literal("O pedestal precisa conter um item catalisador.").withStyle(ChatFormatting.RED), false);
            return;
        }

        ItemStack catalyst = pedestal.getStoredItem();

        ItemStack resultSeal = createSealFromCatalyst(catalyst);

        if (resultSeal.isEmpty()) {
            player.displayClientMessage(Component.literal("Este item não serve como catalisador para nenhum selo.").withStyle(ChatFormatting.RED), false);
            return;
        }

        pedestal.removeStoredItem();

        spawnRitualParticles(level, pos);

        ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 1.25D,
                pos.getZ() + 0.5D,
                resultSeal
        );

        itemEntity.setNoPickUpDelay();
        level.addFreshEntity(itemEntity);

        player.displayClientMessage(Component.literal("O selo foi formado pelo ritual.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    private ItemStack createSealFromCatalyst(ItemStack catalyst) {
        if (catalyst.is(Items.BONE)) {
            return namedSeal(new ItemStack(ModItems.BONE_SEAL.get()));
        }

        if (catalyst.is(Items.GOLD_INGOT)) {
            return namedSeal(new ItemStack(ModItems.GOLD_SEAL.get()));
        }

        if (catalyst.is(Items.DIAMOND)) {
            return namedSeal(new ItemStack(ModItems.DIAMOND_SEAL.get()));
        }

        if (catalyst.is(Items.NETHERITE_INGOT)) {
            return namedSeal(new ItemStack(ModItems.NETHERITE_SEAL.get()));
        }

        return ItemStack.EMPTY;
    }

    private ItemStack namedSeal(ItemStack stack) {
        if (stack.getItem() instanceof SealingSealItem sealItem) {
            stack.setHoverName(sealItem.getTier().displayName());
        }

        return stack;
    }

    private void spawnRitualParticles(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 1.1D;
        double cz = pos.getZ() + 0.5D;

        for (int i = 0; i < 80; i++) {
            double angle = (Math.PI * 2D) * i / 80D;
            double radius = 1.5D;

            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;

            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    x,
                    cy + Math.sin(angle * 2D) * 0.25D,
                    z,
                    1,
                    0.0D,
                    0.04D,
                    0.0D,
                    0.02D
            );
        }

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                cx,
                cy,
                cz,
                80,
                0.7D,
                0.5D,
                0.7D,
                0.04D
        );

        level.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                cx,
                cy + 0.2D,
                cz,
                40,
                0.4D,
                0.2D,
                0.4D,
                0.01D
        );
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof RitualPedestalBlockEntity pedestal && pedestal.hasItem()) {
                Containers.dropItemStack(
                        level,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        pedestal.removeStoredItem()
                );
            }
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof RitualPedestalBlockEntity pedestal) {
            return pedestal.hasItem() ? 15 : 0;
        }

        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualPedestalBlockEntity(pos, state);
    }
}
