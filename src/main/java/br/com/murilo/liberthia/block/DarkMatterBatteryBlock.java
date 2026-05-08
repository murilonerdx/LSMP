package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.DarkMatterBatteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Bloco-base de bateria. Usa um {@code BiFunction<BlockPos, BlockState, BE>}
 * pra instanciar o BE específico do tier.
 *
 * <p>Right-click sem item → mostra status de energia no chat.
 */
public class DarkMatterBatteryBlock extends BaseEntityBlock {

    private final BiFunction<BlockPos, BlockState, ? extends DarkMatterBatteryBlockEntity> factory;
    private final java.util.function.Supplier<BlockEntityType<? extends DarkMatterBatteryBlockEntity>> typeSupplier;

    public DarkMatterBatteryBlock(Properties p,
                                  BiFunction<BlockPos, BlockState, ? extends DarkMatterBatteryBlockEntity> factory,
                                  java.util.function.Supplier<BlockEntityType<? extends DarkMatterBatteryBlockEntity>> typeSupplier) {
        super(p);
        this.factory = factory;
        this.typeSupplier = typeSupplier;
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand h, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof net.minecraft.world.MenuProvider mp
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.minecraftforge.network.NetworkHooks.openScreen(sp, mp, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
        return factory.apply(pos, s);
    }

    @Nullable @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type != typeSupplier.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker) (BlockEntityTicker<DarkMatterBatteryBlockEntity>)
                DarkMatterBatteryBlockEntity::tick;
    }
}
