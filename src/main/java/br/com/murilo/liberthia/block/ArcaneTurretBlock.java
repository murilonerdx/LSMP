package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.ArcaneTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** r184 — Torre Arcana. Mecânica em {@link ArcaneTurretBlockEntity}. */
public class ArcaneTurretBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public ArcaneTurretBlock(Properties props) { super(props); registerDefaultState(stateDefinition.any().setValue(LIT, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneTurretBlockEntity(pos, state); }

    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> t) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> { if (be instanceof ArcaneTurretBlockEntity e) ArcaneTurretBlockEntity.tick(lvl, pos, st, e); };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp
                && level.getBlockEntity(pos) instanceof ArcaneTurretBlockEntity be)
            net.minecraftforge.network.NetworkHooks.openScreen(sp, be, b -> b.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
