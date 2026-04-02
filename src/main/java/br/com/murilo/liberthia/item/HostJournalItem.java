package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HostJournalItem extends Item {
    public HostJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            player.displayClientMessage(Component.literal("=== Diário do Anfitrião: Liberthia ===").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Dia 1 - Liberthia surgiu de origem desconhecida.").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.literal("Três matérias coexistem aqui: Escura, Clara e Amarela.").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.literal("É a ilha mais habitável e diversa até agora.").withStyle(ChatFormatting.GRAY), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Dia 14 - Matéria Escura: a mais poderosa.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("Ela distorce a realidade e infecta qualquer vida.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal("Em 40s de exposição contínua, um ser é totalmente tomado.").withStyle(ChatFormatting.DARK_RED), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Dia 47 - Matéria Clara sozinha parece segura.").withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.literal("Quando unida à Escura, surge uma consciência hostil.").withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.literal("Não é puro caos: existe intenção e habilidade.").withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.literal(""), false);
            player.displayClientMessage(Component.literal("Dia 112 - Matéria Amarela repele a Escura.").withStyle(ChatFormatting.GOLD), false);
            player.displayClientMessage(Component.literal("Com a Clara, amplifica emoções, mas preserva a mente.").withStyle(ChatFormatting.GOLD), false);
            player.displayClientMessage(Component.literal("Juntas, Clara + Amarela combatem a Escura com estratégia.").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Um diário gasto de couro").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("pertencente ao Anfitrião").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("Clique com botão direito para ler").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
