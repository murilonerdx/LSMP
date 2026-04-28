package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.BloodKin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Shared toggle helpers for the staves: a single right-click activates the
 * staff for {@link #DURATION_TICKS} during which it pulses its aura every
 * tick onto nearby hostile mobs (never players). Shift + right-click toggles
 * it off early. Durability decays only while active.
 *
 * <p>Per-mob effect application is done by the caller in
 * {@link br.com.murilo.liberthia.event.StaffAuraEvents}.
 */
public final class StaffActiveLogic {

    public static final String NBT_ACTIVE_UNTIL = "ActiveUntil";
    /** 20 seconds of active state. */
    public static final int DURATION_TICKS = 20 * 20;

    private StaffActiveLogic() {}

    public static boolean isActive(ItemStack stack, Level level) {
        long now = level.getGameTime();
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        return tag.getLong(NBT_ACTIVE_UNTIL) > now;
    }

    public static long activeUntil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(NBT_ACTIVE_UNTIL);
    }

    public static int remainingSeconds(ItemStack stack, Level level) {
        long t = activeUntil(stack) - level.getGameTime();
        return (int) Math.max(0, t / 20);
    }

    /** Right-click activates; shift+right-click deactivates. Returns true on toggle. */
    public static boolean handleToggle(ItemStack stack, Player player) {
        Level level = player.level();
        if (level.isClientSide) return true;
        if (!(player instanceof ServerPlayer sp)) return false;

        net.minecraft.server.level.ServerLevel sl = sp.serverLevel();

        if (player.isShiftKeyDown() || isActive(stack, level)) {
            // Deactivate.
            stack.getOrCreateTag().putLong(NBT_ACTIVE_UNTIL, 0L);
            sp.displayClientMessage(
                    Component.literal("§7Bastão desativado."), true);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    sp.getX(), sp.getY() + 1.0, sp.getZ(),
                    20, 0.4, 0.5, 0.4, 0.05);
            sl.playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.4F);
            return true;
        }
        // Activate for 20s.
        long until = level.getGameTime() + DURATION_TICKS;
        stack.getOrCreateTag().putLong(NBT_ACTIVE_UNTIL, until);
        sp.displayClientMessage(
                Component.literal("§c§l● BASTÃO ATIVADO §7(20s)"), true);

        // Big visible burst around the player so they SEE that something happened.
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                40, 0.8, 0.8, 0.8, 0.1);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                sp.getX(), sp.getY() + 1.5, sp.getZ(),
                30, 0.6, 0.4, 0.6, 0.05);
        sl.playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.6F);
        sl.playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.WITHER_SHOOT,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 2.0F);
        return true;
    }

    /**
     * True if the candidate is a valid victim of a staff aura — must be a Mob,
     * not in a peaceful faction (BloodKin allowed for AOE damage too — caller
     * decides), not the player, not invulnerable, not a player.
     *
     * <p>Important: this never targets players (PvP-safe by design).
     */
    public static boolean isValidVictim(LivingEntity le, Player owner, boolean spareKin) {
        if (le == null || le == owner) return false;
        if (le instanceof Player) return false;
        if (le.isInvulnerable() || !le.isAlive()) return false;
        if (!(le instanceof Mob)) return false;
//        if (spareKin && BloodKin.is(le)) return false;
        return true;
    }
}
