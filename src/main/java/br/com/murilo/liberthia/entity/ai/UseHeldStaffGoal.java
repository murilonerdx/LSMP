package br.com.murilo.liberthia.entity.ai;

import br.com.murilo.liberthia.item.LightningStaffItem;
import br.com.murilo.liberthia.item.MagneticWandItem;
import br.com.murilo.liberthia.item.StaffActiveLogic;
import br.com.murilo.liberthia.item.ThornStaffItem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * If the mob is wielding one of the mod staves, toggles it ON when it has a
 * target and is in range, OFF when out of combat. Mirrors the player toggle
 * mechanic so the same staves work in monster hands.
 *
 * <p>{@link br.com.murilo.liberthia.event.StaffAuraEvents} only fires for
 * players — so for mobs we apply a simplified pulse here directly.
 */
public class UseHeldStaffGoal extends Goal {
    private final Mob mob;
    private int pulseCooldown = 0;

    public UseHeldStaffGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    private boolean holdingStaff() {
        ItemStack s = mob.getMainHandItem();
        return s.getItem() instanceof ThornStaffItem
                || s.getItem() instanceof LightningStaffItem
                || s.getItem() instanceof MagneticWandItem;
    }

    @Override
    public boolean canUse() {
        return holdingStaff() && mob.getTarget() != null
                && mob.getTarget().isAlive();
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void start() {
        ItemStack s = mob.getMainHandItem();
        if (!StaffActiveLogic.isActive(s, mob.level())) {
            long until = mob.level().getGameTime() + StaffActiveLogic.DURATION_TICKS;
            s.getOrCreateTag().putLong(StaffActiveLogic.NBT_ACTIVE_UNTIL, until);
        }
    }

    @Override
    public void stop() {
        ItemStack s = mob.getMainHandItem();
        s.getOrCreateTag().putLong(StaffActiveLogic.NBT_ACTIVE_UNTIL, 0L);
    }

    @Override
    public void tick() {
        if (--pulseCooldown > 0) return;
        ItemStack s = mob.getMainHandItem();
        var target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        // Auto-pulse simple effects on melee range targets while staff is held.
        // The actual staff-specific aura behavior is borrowed from player code:
        // we re-trigger every 40 ticks to keep the active flag fresh.
        if (s.getItem() instanceof ThornStaffItem
                || s.getItem() instanceof LightningStaffItem
                || s.getItem() instanceof MagneticWandItem) {
            long until = mob.level().getGameTime() + 40;
            s.getOrCreateTag().putLong(StaffActiveLogic.NBT_ACTIVE_UNTIL, until);
        }
        pulseCooldown = 30;
    }
}
