package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.SpiritAltarBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r24: Spirit Altar — Altar do Espírito.
 *
 * <h2>Função</h2>
 * Ritual block que envia o player pro Spirit World após uma cerimônia de 5s
 * com animação + drain de XP. Alternativa ao Soul Sever item — não consome
 * cooldown do item, mas exige XP e tempo.
 *
 * <h2>Como usar</h2>
 * <ol>
 *   <li>Coloque o altar no chão.</li>
 *   <li>Right-click no altar pra começar o ritual.</li>
 *   <li>Custo: 10 níveis de XP descontados na ativação.</li>
 *   <li>Aguarde 5s parado em cima (raio 3). Se sair, ritual aborta.</li>
 *   <li>Partículas SOUL + sound build-up crescem até o estouro.</li>
 *   <li>No fim, todos os players num raio de 3 blocos são enviados pro Spirit
 *       World (multi-target! diferente do item single-target).</li>
 * </ol>
 *
 * <h2>Forma</h2>
 * Altura 0.5 (bloco baixo, parece pedestal), full bottom.
 */
public class SpiritAltarBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);

    public SpiritAltarBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // r24: precisa de MODEL pra render normal (não invisible)
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiritAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.SPIRIT_ALTAR.get(),
                SpiritAltarBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SpiritAltarBlockEntity altar)) return InteractionResult.PASS;

        // Se já tá ativo, mostra status
        if (altar.isActive()) {
            sp.displayClientMessage(Component.literal(
                    "§5✦ Ritual em andamento... §d" + altar.getProgressTicks() + "/100§7 ticks"), true);
            return InteractionResult.CONSUME;
        }

        // Custo XP — 10 níveis
        if (sp.experienceLevel < 10) {
            sp.displayClientMessage(Component.literal(
                    "§cRitual requer pelo menos 10 níveis de XP.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        sp.giveExperienceLevels(-10);

        // Inicia ritual — altar gerencia daqui
        altar.startRitual(sp.getUUID());
        sp.displayClientMessage(Component.literal(
                "§5§l✦ §r§5Ritual começou. Fique no raio de 3 blocos por 5s."), true);
        return InteractionResult.CONSUME;
    }
}
