package br.com.murilo.liberthia.entity.ai;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Mob picks up nearby ItemEntities and equips the best one in each slot.
 *
 * <p>Uses the same approach Forge zombies/skeletons use natively via
 * {@code canPickUpLoot()}, but works for any Mob and re-evaluates loot every
 * cooldown ticks instead of only on natural pickup events. Also drops back
 * any inferior gear it was holding.
 */
public class EquipNearbyGearGoal extends Goal {
    private final Mob mob;
    private final double radius;
    private int cooldown = 0;

    public EquipNearbyGearGoal(Mob mob, double radius) {
        this.mob = mob;
        this.radius = radius;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (cooldown-- > 0) return false;
        cooldown = 40;
        return true;
    }

    @Override
    public void start() {
        AABB box = new AABB(mob.blockPosition()).inflate(radius);
        List<ItemEntity> items = mob.level().getEntitiesOfClass(ItemEntity.class, box,
                ie -> !ie.hasPickUpDelay() && ie.isAlive());
        for (ItemEntity ie : items) {
            ItemStack stack = ie.getItem();
            EquipmentSlot slot = pickSlotFor(stack);
            if (slot == null) continue;
            ItemStack held = mob.getItemBySlot(slot);
            if (compareGear(stack, held, slot) > 0) {
                if (!held.isEmpty()) {
                    // Drop the current item in this slot.
                    mob.spawnAtLocation(held.copy());
                }
                mob.setItemSlot(slot, stack.copy());
                mob.setGuaranteedDrop(slot);
                ie.discard();
            }
        }
    }

    private EquipmentSlot pickSlotFor(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem ai) {
            return ai.getEquipmentSlot();
        }
        if (stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof TieredItem
                || stack.getItem() instanceof br.com.murilo.liberthia.item.ThornStaffItem
                || stack.getItem() instanceof br.com.murilo.liberthia.item.LightningStaffItem
                || stack.getItem() instanceof br.com.murilo.liberthia.item.MagneticWandItem
                || stack.getItem() instanceof br.com.murilo.liberthia.item.SoulScreamSwordItem) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    /** Higher = better. Compares by tier/durability/armor. */
    private int compareGear(ItemStack candidate, ItemStack current, EquipmentSlot slot) {
        if (current.isEmpty()) return 1;
        if (slot == EquipmentSlot.MAINHAND) {
            float candAtt = weaponDamage(candidate);
            float curAtt = weaponDamage(current);
            return Float.compare(candAtt, curAtt);
        }
        if (candidate.getItem() instanceof ArmorItem c
                && current.getItem() instanceof ArmorItem r) {
            return Integer.compare(c.getDefense(), r.getDefense());
        }
        return 0;
    }

    private float weaponDamage(ItemStack stack) {
        if (stack.getItem() instanceof SwordItem sw) return sw.getDamage();
        if (stack.getItem() instanceof TieredItem ti) return ti.getTier().getAttackDamageBonus();
        // Mod staves: treat as mid-tier (3.5).
        return 3.5F;
    }
}
