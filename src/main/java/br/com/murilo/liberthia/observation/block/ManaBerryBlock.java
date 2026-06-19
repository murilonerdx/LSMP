package br.com.murilo.liberthia.observation.block;

import br.com.murilo.liberthia.observation.source.SourceData;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v0.1.22 r74: <b>Mana Berry Block</b> — crop block que cresce 3 stages.
 *
 * <p>Pattern AN's ManaBerry: crop em vine. Quando maduro, right-click colhe 1-3 berries.
 * Berry comestível restaura +20 Source.
 */
public class ManaBerryBlock extends Block implements BonemealableBlock {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    private static final VoxelShape SHAPE_0 = Block.box(5, 0, 5, 11, 6, 11);
    private static final VoxelShape SHAPE_1 = Block.box(4, 0, 4, 12, 10, 12);
    private static final VoxelShape SHAPE_2_3 = Block.box(2, 0, 2, 14, 14, 14);

    public ManaBerryBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        int age = s.getValue(AGE);
        return age == 0 ? SHAPE_0 : age == 1 ? SHAPE_1 : SHAPE_2_3;
    }

    @Override public boolean isRandomlyTicking(BlockState s) { return s.getValue(AGE) < 3; }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) < 3 && random.nextFloat() < 0.18F) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 2);
        }
    }

    /** Right-click: colhe berries se age==3, dá +20 Source. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (state.getValue(AGE) < 3) {
            sp.displayClientMessage(Component.literal("§7§oFruto ainda imaturo."), true);
            return InteractionResult.PASS;
        }
        // Drop 1-3 berries
        int count = 1 + level.random.nextInt(3);
        if (!sp.getInventory().add(new ItemStack(ModItems.MANA_BERRY.get(), count))) {
            sp.drop(new ItemStack(ModItems.MANA_BERRY.get(), count), false);
        }
        // Source bonus
        SourceData.add(sp, 10);
        sp.displayClientMessage(Component.literal(
            "§5§l✦ §r§dColheu §e" + count + " Mana Berries §7+10 Source"), true);
        // Reset age
        level.setBlock(pos, state.setValue(AGE, 1), 2);
        return InteractionResult.CONSUME;
    }

    // BonemealableBlock — pode usar bonemeal pra crescer
    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader lvl, BlockPos pos, BlockState state, boolean isClient) {
        return state.getValue(AGE) < 3;
    }
    @Override public boolean isBonemealSuccess(Level l, RandomSource r, BlockPos p, BlockState s) { return true; }
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(3, state.getValue(AGE) + 1)), 2);
    }
}
