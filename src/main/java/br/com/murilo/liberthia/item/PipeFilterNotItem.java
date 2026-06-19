package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.block.ItemPipeBlock;
import br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pipe Filter (NOT) — marker que abre a GUI do filtro do pipe E garante que
 * a face esteja em modo BLACKLIST.
 *
 * <p><b>v0.1.31</b>: filter agora é UNIFICADO por pipe (não por face).
 * Right-click em qualquer face de qualquer pipe (normal/extractor/inserter)
 * abre a MESMA GUI. Cycle WHITELIST/BLACKLIST é via botão dentro da GUI.
 *
 * <p>Pipe ItemPipe, ItemExtractor e ItemInserter compartilham o mesmo
 * {@link ItemPipeBlockEntity} então o filter funciona idêntico nos 3.
 */
public class PipeFilterNotItem extends Item {

    public PipeFilterNotItem(Properties properties) {
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

        // Garante BLACKLIST (semantic do _not).
        if (pipe.getFilterMode() != ItemPipeBlockEntity.FilterMode.BLACKLIST) {
            pipe.cycleFilterMode();
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            openFilterGui(sp, pipe, ctx.getClickedFace());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click no pipe: abre GUI do filtro.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Inicia a face como BLACKLIST (passa tudo MENOS listados).")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Filtro é POR PIPE — vale pra todas as faces.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Abre a GUI do filtro. Face passada serve só pra label da tela. */
    public static void openFilterGui(net.minecraft.server.level.ServerPlayer sp,
                                ItemPipeBlockEntity pipe,
                                Direction face) {
        net.minecraftforge.network.NetworkHooks.openScreen(sp,
                new net.minecraft.world.SimpleMenuProvider(
                        (id, inv, p) -> new br.com.murilo.liberthia.menu.PipeFilterMenu(
                                id, inv, pipe, face),
                        Component.literal("Filtro do Pipe")),
                buf -> {
                    buf.writeBlockPos(pipe.getBlockPos());
                    buf.writeByte(face.get3DDataValue());
                });
    }
}
