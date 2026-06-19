package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.WardPillarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import org.jetbrains.annotations.Nullable;

/**
 * r180: <b>Pilar Protetor</b> — campo anti-magia FE-powered. O dono (quem coloca) é imune;
 * intrusos com itens de magia são selados/empurrados. Veja {@link WardPillarBlockEntity}.
 */
public class WardPillarBlock extends Block implements EntityBlock {

    public WardPillarBlock(Properties props) { super(props); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WardPillarBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof WardPillarBlockEntity be) {
            be.setOwner(p.getUUID());
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof WardPillarBlockEntity w) WardPillarBlockEntity.tick(lvl, pos, st, w);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WardPillarBlockEntity be) {
            boolean isOwner = be.getOwner() != null && be.getOwner().equals(player.getUUID());
            player.displayClientMessage(Component.literal("§3§l═ Pilar Protetor ═"), false);
            player.displayClientMessage(Component.literal("§7Energia: §b" + be.getEnergyStored() + " FE  "
                    + (be.isActive() ? "§a[ATIVO]" : "§8[sem energia]")), false);
            player.displayClientMessage(Component.literal("§7Dono: " + (isOwner ? "§avocê" : "§coutro")), false);
            player.displayClientMessage(Component.literal("§8Sela/expulsa quem carrega itens de magia (≤8 blocos). Precisa de FE da rede."), false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
