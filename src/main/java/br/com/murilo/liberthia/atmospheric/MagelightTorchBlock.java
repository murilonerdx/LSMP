package br.com.murilo.liberthia.atmospheric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v0.1.24 r89: <b>Magelight Torch</b> — torch flutuante que ilumina cavernas.
 *
 * <p>Emite luz 15 + particles glow. Não precisa support. Float fixo.
 *
 * <p>Future: a cada N ticks, scaneia raio de 16 procurando ar escuro (light&lt;8)
 * e coloca TempLightBlock invisível.
 */
public class MagelightTorchBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.box(0.3, 0.3, 0.3, 0.7, 0.7, 0.7);

    public MagelightTorchBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Particle glow + end_rod ascending
        level.addParticle(ParticleTypes.END_ROD,
                pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                pos.getY() + 0.5 + random.nextDouble() * 0.3,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                0, 0.01, 0);
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.GLOW,
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.05,
                    0.02,
                    (random.nextDouble() - 0.5) * 0.05);
        }
    }
}
