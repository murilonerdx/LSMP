package br.com.murilo.liberthia.occult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v0.1.22 r32: vela ritualística. 5 cores. Estado LIT bool.
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Apagada (default): bloco pequeno (2x6x2 px), sem luz</li>
 *   <li>Acesa: luz 12, partículas de chama, glow</li>
 *   <li>Acende com {@link OccultItems.LighterItem} ou flint+steel</li>
 *   <li>Detectado por Ritual Circle pra montar configurações</li>
 * </ul>
 */
public class CandleBlock extends Block {

    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    private static final VoxelShape AABB = Shapes.box(0.375, 0.0, 0.375, 0.625, 0.5, 0.625);

    public final OccultItems.ChalkColor color;

    public CandleBlock(BlockBehaviour.Properties p, OccultItems.ChalkColor color) {
        super(p
                .strength(0.1F)
                .noOcclusion()
                .lightLevel(s -> s.getValue(LIT) ? 12 : 0));
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return AABB;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.55;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.005, 0);
        // Particle por cor: white = SOUL, golden = END_ROD, purple = PORTAL, red = LAVA, black = SMOKE
        switch (color) {
            case WHITE  -> level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
            case GOLDEN -> level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.01, 0);
            case PURPLE -> level.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.01, 0);
            case RED    -> level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0.01, 0);
            case BLACK  -> level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
        }
    }
}
