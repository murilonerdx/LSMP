package br.com.murilo.liberthia.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * v0.1.24 r88: <b>Spell Turret Block</b> — bloco que casta spell parchment.
 *
 * <h2>Controle</h2>
 * <ul>
 *   <li>Right-click com parchment → seta parchment</li>
 *   <li>Right-click vazio → toggle mode (Basic ↔ Timer)</li>
 *   <li>Shift+Right-click → drop parchment</li>
 *   <li>Redstone → fire (modo Basic)</li>
 * </ul>
 */
public class SpellTurretBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);

    public SpellTurretBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpellTurretBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof SpellTurretBlockEntity sbe) {
                SpellTurretBlockEntity.serverTick(lvl, pos, st, sbe);
            }
        };
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                 BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof SpellTurretBlockEntity be) {
            be.onPowered(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SpellTurretBlockEntity be)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);

        // Shift+Right-click vazio → drop parchment
        if (player.isShiftKeyDown() && held.isEmpty() && !be.getParchment().isEmpty()) {
            ItemStack drop = be.getParchment().copy();
            be.setParchment(ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(drop);
            player.displayClientMessage(Component.literal("§5✦ Parchment removido."), true);
            return InteractionResult.CONSUME;
        }

        // Right-click com parchment → seta
        if (!held.isEmpty() && held.getItem() instanceof br.com.murilo.liberthia.observation.item.SpellParchmentItem) {
            if (be.getParchment().isEmpty()) {
                be.setParchment(held.split(1));
                player.displayClientMessage(Component.literal("§5§l✦ Parchment instalado."), true);
                return InteractionResult.CONSUME;
            }
        }

        // Right-click vazio (sneak) → toggle mode (legacy behavior)
        if (held.isEmpty() && player.isShiftKeyDown()) {
            be.setMode(be.getMode() + 1);
            String label = be.getMode() == 0 ? "§eRedstone (basic)" : "§bTimer (" + be.getTimerInterval() + "t)";
            player.displayClientMessage(Component.literal("§5✦ Modo: ").append(Component.literal(label)), true);
            return InteractionResult.CONSUME;
        }

        // r164: Right-click vazio sem sneak → abre info screen
        if (held.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            java.util.List<String> stats = new java.util.ArrayList<>();
            stats.add("§7Direção: §e" + state.getValue(FACING).getName().toUpperCase());
            stats.add("§7Parchment: " + (be.getParchment().isEmpty()
                    ? "§c§o(nenhum)" : "§a" + be.getParchment().getHoverName().getString()));
            stats.add("§7Modo: " + (be.getMode() == 0 ? "§eRedstone" : "§bTimer §7(" + be.getTimerInterval() + "t)"));
            stats.add("§7Cast direction: §dao longo da face FACING");
            java.util.List<String> instr = new java.util.ArrayList<>();
            instr.add("§7• §eRClick com Spell Parchment§7 = instalar feitiço");
            instr.add("§7• §eShift+RClick vazio§7 = trocar modo (Redstone/Timer)");
            instr.add("§7• §eShift+RClick com parchment§7 = remover parchment");
            instr.add("§7• Modo §eRedstone§7: dispara quando recebe sinal");
            instr.add("§7• Modo §bTimer§7: dispara automático a cada §e" + be.getTimerInterval() + "t");
            instr.add("§7• Combine com §dSpell Sensor + Prism§7 pra puzzle de mira");
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                    new br.com.murilo.liberthia.network.packet.OpenBlockInfoS2CPacket(
                            "🎯 Spell Turret", stats, instr, 0xFFA040));
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}
