package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.event.DimensionalDiseaseHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;

import javax.annotation.Nullable;
import java.util.List;

/**
 * r184 — <b>Soro Dimensional</b>: a cura das doenças dimensionais. Ao usar, purga
 * Memória/Infecção/Paranoia Dimensional do jogador e gasta uma unidade.
 */
public class DimensionalSerumItem extends Item {
    public DimensionalSerumItem(Properties p) { super(p.rarity(Rarity.EPIC)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean had = DimensionalDiseaseHandler.hasAnyDisease(player);
            DimensionalDiseaseHandler.cureAll(player);
            level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 1.2F);
            if (had && !player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§7Cura todas as doenças dimensionais."));
        tip.add(Component.literal("§8Memória · Infecção · Paranoia"));
    }
}
