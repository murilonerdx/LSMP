package br.com.murilo.liberthia.magic.spell.weaver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

/**
 * v0.1.151 r119: <b>SpellWeaverBlock</b> — bloco que combina um scroll BASE
 * + múltiplos modifier-glyphs em um scroll COMPOSED.
 *
 * <p>Diferente do GlyphInscriber (que cria reagentes em glyphs), o SpellWeaver
 * trabalha o nível acima: você pega um scroll vanilla + adiciona modifier
 * glyphs (AOE, AMPLIFY, etc.) e gera um novo scroll com múltiplas páginas.
 *
 * <h2>Layout</h2>
 * <pre>
 *  ┌─ SpellWeaver ───────────────┐
 *  │   [base]   →  ★  →  [out]  │   row top: base/output
 *  │                              │
 *  │  [m1] [m2] [m3] [m4]         │   row mid: 4 modifiers
 *  │  [m5] [m6] [m7]              │   row bot: 3 modifiers
 *  │                              │
 *  │     [Player Inventory]       │
 *  └──────────────────────────────┘
 * </pre>
 *
 * <p>VFX top: rune-orbit constante, intensifica quando craft válido.
 */
public class SpellWeaverBlock extends Block implements EntityBlock {

    // Bloco maior que GlyphInscriber — base octogonal + pilar
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 4, 15),
            Block.box(3, 4, 3, 13, 12, 13),
            Block.box(0, 12, 0, 16, 16, 16));

    public SpellWeaverBlock(Properties props) {
        super(props);
    }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpellWeaverBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return (l, p, s, be) -> {
            if (be instanceof SpellWeaverBlockEntity swe) {
                SpellWeaverBlockEntity.serverTick(l, p, s, swe);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SpellWeaverBlockEntity swe && player instanceof ServerPlayer sp) {
            NetworkHooks.openScreen(sp, swe, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SpellWeaverBlockEntity swe) swe.drops();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
