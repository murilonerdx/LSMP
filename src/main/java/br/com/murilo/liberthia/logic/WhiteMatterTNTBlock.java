package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.entity.WhiteMatterExplosionEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * White Matter TNT — when activated (right-click or redstone), spawns a WhiteMatterExplosionEntity
 * that clears infection in 8-block radius and applies CLEAR_SHIELD in 16-block radius.
 */
public class WhiteMatterTNTBlock extends Block {

    public WhiteMatterTNTBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            ignite(level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            ignite(level, pos);
        }
    }

    private void ignite(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
        WhiteMatterExplosionEntity entity = new WhiteMatterExplosionEntity(ModEntities.WHITE_MATTER_EXPLOSION.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        level.addFreshEntity(entity);
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
