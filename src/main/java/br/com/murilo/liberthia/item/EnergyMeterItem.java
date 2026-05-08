package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Right-click num bloco com capability de FE → mostra energia armazenada. */
public class EnergyMeterItem extends Item {
    public EnergyMeterItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (be == null) {
            player.displayClientMessage(Component.literal("Nenhum bloco com energia").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        var cap = be.getCapability(ForgeCapabilities.ENERGY, ctx.getClickedFace());
        if (cap.isPresent()) {
            cap.ifPresent(es -> {
                int e = es.getEnergyStored(), max = es.getMaxEnergyStored();
                int pct = max <= 0 ? 0 : (int) (e * 100L / max);
                player.displayClientMessage(
                        Component.literal(String.format("⚡ %,d / %,d FE (%d%%)", e, max, pct))
                                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            });
        } else {
            player.displayClientMessage(
                    Component.literal("Bloco sem capacidade de energia").withStyle(ChatFormatting.GRAY), true);
        }

        return InteractionResult.CONSUME;
    }
}
