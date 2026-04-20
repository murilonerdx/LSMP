package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.projectile.BleedingArrowEntity;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Blood Bow — consumes 1 Blood Vial Filled per shot (if available) to upgrade arrow to a BleedingArrow
 * that applies Blood Infection on hit. Falls back to vanilla arrow if no vial is present.
 */
public class BloodBowItem extends BowItem {
    public BloodBowItem(Properties p) { super(p.durability(320)); }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) return;
        int charge = this.getUseDuration(stack) - timeLeft;
        float power = getPowerForTime(charge);
        if (power < 0.1F) return;

        // Find arrow in inventory (or creative free)
        ItemStack ammo = player.getProjectile(stack);
        boolean infinite = player.getAbilities().instabuild
                || (ammo.getItem() instanceof net.minecraft.world.item.ArrowItem
                    && net.minecraft.world.item.ArrowItem.class.cast(ammo.getItem())
                        .isInfinite(ammo, stack, player));
        if (ammo.isEmpty() && !infinite) return;

        if (!level.isClientSide) {
            // Check for blood vial filled — upgrade shot
            boolean hasVial = player.getInventory().contains(new ItemStack(ModItems.BLOOD_VIAL_FILLED.get()));
            AbstractArrow arrow;
            if (hasVial) {
                BleedingArrowEntity b = new BleedingArrowEntity(level, player);
                b.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.2F, 1.0F);
                arrow = b;
                // consume 1 vial
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack s = player.getInventory().getItem(i);
                    if (s.is(ModItems.BLOOD_VIAL_FILLED.get())) {
                        s.shrink(1);
                        // return empty vial
                        if (!player.getInventory().add(new ItemStack(ModItems.BLOOD_VIAL.get()))) {
                            player.drop(new ItemStack(ModItems.BLOOD_VIAL.get()), false);
                        }
                        break;
                    }
                }
            } else {
                arrow = new net.minecraft.world.entity.projectile.Arrow(level, player);
                arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 1.0F);
            }
            if (power >= 1.0F) arrow.setCritArrow(true);
            if (infinite || player.getAbilities().instabuild) {
                arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(arrow);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));

            if (!infinite && !player.getAbilities().instabuild) {
                ammo.shrink(1);
                if (ammo.isEmpty()) player.getInventory().removeItem(ammo);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        player.awardStat(Stats.ITEM_USED.get(this));
    }
}
