package br.com.murilo.liberthia.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * ChalkSymbol — flat 1-pixel-tall decorative block. Non-collidable.
 * Used to mark containment boundaries for proliferation mother blocks.
 */
public class ChalkSymbolBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);

    public ChalkSymbolBlock(Properties props) {
        super(props.noCollission().noOcclusion().instabreak());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        // Soft dust particles occasionally
        if (rng.nextFloat() < 0.05F) {
            level.addParticle(ParticleTypes.WHITE_ASH,
                    pos.getX() + 0.2 + rng.nextDouble() * 0.6,
                    pos.getY() + 0.05,
                    pos.getZ() + 0.2 + rng.nextDouble() * 0.6,
                    0, 0.01, 0);
        }
    }
}
