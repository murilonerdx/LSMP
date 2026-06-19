package br.com.murilo.liberthia.occult;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r32: <b>Pilar de Mineração Espiritual</b> — automação de mineração
 * via Foliot. Funciona se tiver {@code BoundFoliotCrystal} em ANY adjacent
 * inventory. Minera blocos num raio configurável.
 */
public class SpiritMinerBlock extends BaseEntityBlock {

    public SpiritMinerBlock(BlockBehaviour.Properties p) { super(p); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiritMinerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.SPIRIT_MINER.get(),
                SpiritMinerBlockEntity::serverTick);
    }
}
