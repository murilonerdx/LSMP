package br.com.murilo.liberthia.magic.conduit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * v0.1.149 r117: <b>SourceTransmuterBlock</b>.
 *
 * <h2>UX de bind</h2>
 * <ol>
 *   <li>Player vai pro Spirit World, coloca um SpiritConduit</li>
 *   <li>Sneak+right-click no SpiritConduit com Spiritual Link → grava pos no item</li>
 *   <li>Sai do Spirit World, coloca SourceTransmuter, right-click com Link → bind</li>
 *   <li>Daí em diante: chegue perto do Transmuter → ele drena charge e te dá Source</li>
 * </ol>
 *
 * <p>OU método simplificado: right-click com item que tenha NBT
 * {@code liberthia.conduit_pos} (long).
 */
public class SourceTransmuterBlock extends Block implements EntityBlock {

    public static final String NBT_LINK_TARGET = "liberthia.conduit_pos";

    public SourceTransmuterBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SourceTransmuterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return (l, p, s, be) -> {
            if (be instanceof SourceTransmuterBlockEntity tr) {
                SourceTransmuterBlockEntity.serverTick(l, p, s, tr);
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SourceTransmuterBlockEntity tr)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        // Bind: se o item tem NBT liberthia.conduit_pos, transfere pro transmuter
        if (held.hasTag() && held.getTag().contains(NBT_LINK_TARGET)) {
            BlockPos linked = BlockPos.of(held.getTag().getLong(NBT_LINK_TARGET));
            tr.setBoundConduit(linked);
            player.displayClientMessage(Component.literal(
                    "§a✓ Bound to conduit at §e" + linked.toShortString()), true);
            // Limpa do item (consome o link)
            held.getTag().remove(NBT_LINK_TARGET);
            return InteractionResult.CONSUME;
        }

        // Status check sem item
        BlockPos bound = tr.getBoundConduit();
        if (bound == null) {
            player.displayClientMessage(Component.literal(
                    "§7Transmuter sem bind. Use um item com NBT §6liberthia.conduit_pos"), true);
        } else {
            player.displayClientMessage(Component.literal(
                    "§5✦ Bound to §d" + bound.toShortString() + " §7(Spirit World)"), true);
        }
        return InteractionResult.CONSUME;
    }

    /** Helper: marca um item com NBT pra fazer o bind. */
    public static void markLinkOnItem(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(NBT_LINK_TARGET, pos.asLong());
    }
}
