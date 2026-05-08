package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.WirelessChargerBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WirelessChargerBlock extends BaseEntityBlock {
    public WirelessChargerBlock(Properties p) { super(p); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand h, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof WirelessChargerBlockEntity be) {
            int e = be.getEnergyStored(), max = be.getMaxEnergyStored();
            int pct = max == 0 ? 0 : (int) (e * 100L / max);
            player.displayClientMessage(
                    Component.literal(String.format("⚡ Wireless Charger: %,d / %,d FE (%d%%) | %d carregando",
                                    e, max, pct, be.getActiveChargingPlayers()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
        return new WirelessChargerBlockEntity(pos, s);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.WIRELESS_CHARGER.get(),
                WirelessChargerBlockEntity::tick);
    }
}
