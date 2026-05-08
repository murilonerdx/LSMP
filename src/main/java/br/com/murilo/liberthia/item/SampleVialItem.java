package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.matter.MatterContentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sample Vial — frasco de coleta. Right-click num bloco copia o conteúdo de
 * matéria daquele bloco/item pro frasco (NBT). Depois você coloca o frasco no
 * Matter Analyzer pra ver os detalhes.
 *
 * <p>Funciona pra qualquer bloco registrado no {@link MatterContentRegistry}.
 */
public class SampleVialItem extends Item {

    public static final String TAG_DM = "dm";
    public static final String TAG_WM = "wm";
    public static final String TAG_YM = "ym";
    public static final String TAG_SOURCE = "src";

    public SampleVialItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack blockStack = new ItemStack(state.getBlock().asItem());
        MatterContent content = MatterContentRegistry.of(blockStack);

        if (content.total() <= 0) {
            player.displayClientMessage(
                    Component.literal("Bloco sem traços de matéria")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }

        ItemStack inHand = ctx.getItemInHand();
        // Se tem mais de 1 frasco, tira 1 e devolve no inventário separado
        ItemStack vial = inHand.copy();
        vial.setCount(1);
        var tag = vial.getOrCreateTag();
        tag.putFloat(TAG_DM, content.dark());
        tag.putFloat(TAG_WM, content.white());
        tag.putFloat(TAG_YM, content.yellow());
        tag.putString(TAG_SOURCE, blockStack.getHoverName().getString());

        if (inHand.getCount() > 1) {
            inHand.shrink(1);
            if (!player.getInventory().add(vial)) player.drop(vial, false);
        } else {
            // Substitui o item na mão
            player.setItemInHand(ctx.getHand(), vial);
        }
        player.displayClientMessage(
                Component.literal("Amostra coletada: " + blockStack.getHoverName().getString())
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResult.CONSUME;
    }

    /** Lê o conteúdo do frasco — usado pelo Analyzer. */
    public static MatterContent contentOf(ItemStack vialStack) {
        if (!vialStack.hasTag()) return MatterContent.EMPTY;
        var tag = vialStack.getTag();
        return new MatterContent(tag.getFloat(TAG_DM), tag.getFloat(TAG_WM), tag.getFloat(TAG_YM));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains(TAG_SOURCE)) {
            tooltip.add(Component.literal("Fonte: " + stack.getTag().getString(TAG_SOURCE))
                    .withStyle(ChatFormatting.AQUA));
            MatterContent c = contentOf(stack);
            tooltip.add(Component.literal(String.format(
                    "DM:%.0f  WM:%.0f  YM:%.0f", c.dark(), c.white(), c.yellow()))
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("» " + c.dominantMutation().displayName)
                    .withStyle(c.dominantMutation().color));
        } else {
            tooltip.add(Component.literal("Right-click num bloco pra coletar amostra")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG_SOURCE);
    }
}
