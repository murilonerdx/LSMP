package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.projectile.BloodPearlEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ported from EvilCraft's {@code ItemBloodPearlOfTeleportation}. Throws a
 * {@link BloodPearlEntity} that teleports the user; the cost is paid in HP
 * (4 HP per throw) instead of EvilCraft's blood-fluid container.
 */
public class BloodTeleportPearlItem extends Item {

    private static final float HP_COST = 4.0F;

    public BloodTeleportPearlItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getHealth() <= HP_COST + 0.5F && !player.isCreative()) {
            // Not enough HP to safely throw.
            return InteractionResultHolder.fail(stack);
        }

        if (!player.isCreative()) {
            DamageSource bleed = player.damageSources().magic();
            player.hurt(bleed, HP_COST);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, 0.6F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            BloodPearlEntity pearl = new BloodPearlEntity(level, player);
            pearl.setItem(stack);
            pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(pearl);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
