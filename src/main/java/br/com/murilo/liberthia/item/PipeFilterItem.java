package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.block.ItemPipeBlock;
import br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pipe Filter — abre GUI do filtro do pipe e garante modo WHITELIST.
 *
 * <p>v0.1.31: filter unificado por pipe (não por face). Mesma GUI do
 * pipe_filter_not, diferença é o modo inicial (WHITELIST vs BLACKLIST).
 */
public class PipeFilterItem extends Item {

    public PipeFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level.getBlockState(ctx.getClickedPos()).getBlock() instanceof ItemPipeBlock)) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(ctx.getClickedPos()) instanceof ItemPipeBlockEntity pipe)) {
            return InteractionResult.PASS;
        }
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Garante WHITELIST (idempotente).
        if (pipe.getFilterMode() != ItemPipeBlockEntity.FilterMode.WHITELIST) {
            pipe.cycleFilterMode();
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            PipeFilterNotItem.openFilterGui(sp, pipe, ctx.getClickedFace());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click no pipe: abre GUI do filtro.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Inicia a face como WHITELIST (passa SÓ os listados).")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Filtro é POR PIPE — vale pra todas as faces.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
