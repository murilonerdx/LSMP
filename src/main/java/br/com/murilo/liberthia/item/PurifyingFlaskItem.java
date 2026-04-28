package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.projectile.PurifyingFlaskEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Thrower for {@link PurifyingFlaskEntity}. */
public class PurifyingFlaskItem extends Item {
    public PurifyingFlaskItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7F, 1.5F);
        if (!level.isClientSide) {
            PurifyingFlaskEntity g = new PurifyingFlaskEntity(level, player);
            g.setItem(stack);
            g.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0F, 1.2F, 1.0F);
            level.addFreshEntity(g);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§7Arremesse no chão. AoE §a6 blocos§7."));
        tip.add(Component.literal("§7Cura Infecção, dá Regeneração e fere vermes (§c7 ❤§7)."));
    }
}
