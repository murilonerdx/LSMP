package br.com.murilo.liberthia.occult;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * v0.1.22 r32: marca de giz no chão. 5 cores × 4 formas = 20 estados visuais.
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Quebrável instantaneamente (qualquer item destrói)</li>
 *   <li>Sem colisão — atravessável</li>
 *   <li>Visual: textura no top do bloco abaixo, altura 1 pixel</li>
 *   <li>Detectado por {@code RitualCircleBlockEntity} pra montar padrões</li>
 * </ul>
 *
 * <p>5 cores instantiadas pelo registry (uma por color), 1 enum SHAPE pra
 * variar entre dot/line/circle/sigil.
 */
public class ChalkMarkBlock extends Block {

    public enum Shape implements StringRepresentable {
        DOT, LINE, CIRCLE, SIGIL;
        @Override public String getSerializedName() { return name().toLowerCase(); }
    }

    public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
    private static final VoxelShape AABB = Shapes.box(0, 0, 0, 1, 0.0625, 1);

    public final OccultItems.ChalkColor color;

    public ChalkMarkBlock(BlockBehaviour.Properties props, OccultItems.ChalkColor color) {
        super(props
                .strength(0.0f, 0.0f)
                .noCollission()
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)
                .instabreak());
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, Shape.DOT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(SHAPE);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return AABB;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction dir,
                                   BlockState neighborState, net.minecraft.world.level.LevelAccessor level,
                                   BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        // Devolve o chalk item correspondente quando pick-block
        return new ItemStack(getChalkItemFor(color));
    }

    // ───────── helpers pra mapear color → block + color → chalk item ─────────

    private static final Map<OccultItems.ChalkColor, ChalkMarkBlock> BY_COLOR = new HashMap<>();

    public static void register(OccultItems.ChalkColor color, ChalkMarkBlock block) {
        BY_COLOR.put(color, block);
    }

    public static ChalkMarkBlock getBlockFor(OccultItems.ChalkColor color) {
        return BY_COLOR.get(color);
    }

    public static net.minecraft.world.item.Item getChalkItemFor(OccultItems.ChalkColor color) {
        return switch (color) {
            case WHITE  -> br.com.murilo.liberthia.registry.ModItems.CHALK_WHITE.get();
            case GOLDEN -> br.com.murilo.liberthia.registry.ModItems.CHALK_GOLDEN.get();
            case PURPLE -> br.com.murilo.liberthia.registry.ModItems.CHALK_PURPLE.get();
            case RED    -> br.com.murilo.liberthia.registry.ModItems.CHALK_RED.get();
            case BLACK  -> br.com.murilo.liberthia.registry.ModItems.CHALK_BLACK.get();
        };
    }
}
