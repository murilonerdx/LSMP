package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gravity Trap — right-click a player to anchor them with a black hole effect.
 * They get pulled back if they move/teleport. Damage scales with distance.
 */
public class GravityTrapItem extends Item {

    // Active traps: victim UUID -> anchor position + dimension
    public static final Map<UUID, TrapData> ACTIVE_TRAPS = new HashMap<>();

    public record TrapData(Vec3 pos, String dimension, int ticksLeft) {}

    private static final int DURATION_TICKS = 200; // 10 seconds
    private static final double PULL_RANGE = 5.0;
    private static final double TP_DAMAGE_THRESHOLD = 32.0;
    private static final float TP_DAMAGE = 8.0F;

    public GravityTrapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(target instanceof ServerPlayer victim)) return InteractionResult.PASS;

        // Anchor victim at current pos
        ACTIVE_TRAPS.put(victim.getUUID(), new TrapData(
                victim.position(),
                victim.level().dimension().location().toString(),
                DURATION_TICKS
        ));

        // Effects
        if (user.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, victim.getX(), victim.getY() + 1, victim.getZ(), 40, 0.5, 1.0, 0.5, 0.1);
            sl.sendParticles(ParticleTypes.PORTAL, victim.getX(), victim.getY() + 1, victim.getZ(), 60, 1.0, 1.5, 1.0, 0.3);
        }
        if (user.level() instanceof ServerLevel srvl) {
            playSounds(srvl, victim);
        }

        victim.sendSystemMessage(Component.translatable("chat.liberthia.gravity_trapped").withStyle(ChatFormatting.DARK_PURPLE));
        user.sendSystemMessage(Component.translatable("chat.liberthia.gravity_trap_used", victim.getDisplayName()).withStyle(ChatFormatting.GOLD));

        stack.shrink(1);
        user.getCooldowns().addCooldown(this, 100);

        return InteractionResult.SUCCESS;
    }

    private static void playSounds(ServerLevel level, ServerPlayer victim) {
        level.playSound(null, victim.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 0.5F);
        level.playSound(null, victim.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(), SoundSource.PLAYERS, 1.0F, 0.3F);
    }

    /**
     * Called every server tick from InfectionEvents to process active traps.
     */
    public static void tickTraps(ServerLevel level) {
        var iterator = ACTIVE_TRAPS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID uuid = entry.getKey();
            TrapData data = entry.getValue();

            ServerPlayer victim = level.getServer().getPlayerList().getPlayer(uuid);
            if (victim == null || !victim.isAlive()) {
                iterator.remove();
                continue;
            }

            // Decrease timer
            int remaining = data.ticksLeft() - 1;
            if (remaining <= 0) {
                iterator.remove();
                victim.sendSystemMessage(Component.translatable("chat.liberthia.gravity_released").withStyle(ChatFormatting.GREEN));
                continue;
            }
            entry.setValue(new TrapData(data.pos(), data.dimension(), remaining));

            String currentDim = victim.level().dimension().location().toString();
            Vec3 anchor = data.pos();
            double dist = victim.position().distanceTo(anchor);

            // Different dimension = force teleport back + big damage
            if (!currentDim.equals(data.dimension())) {
                // Can't cross-dim teleport easily, just damage heavily
                victim.hurt(victim.damageSources().magic(), 12.0F);
                victim.sendSystemMessage(Component.translatable("chat.liberthia.gravity_dimension_pull").withStyle(ChatFormatting.DARK_RED));
                continue;
            }

            // Long-range teleport attempt → snap back + damage
            if (dist > TP_DAMAGE_THRESHOLD) {
                victim.teleportTo(anchor.x, anchor.y, anchor.z);
                victim.hurt(victim.damageSources().magic(), TP_DAMAGE);
                if (victim.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, anchor.x, anchor.y + 1, anchor.z, 50, 0.5, 1.0, 0.5, 0.15);
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, anchor.x, anchor.y + 1, anchor.z, 10, 0.3, 0.5, 0.3, 0.1);
                }
                victim.sendSystemMessage(Component.translatable("chat.liberthia.gravity_snap_back").withStyle(ChatFormatting.RED));
                continue;
            }

            // Within pull range: suction effect — pull toward anchor
            if (dist > 1.5) {
                Vec3 direction = anchor.subtract(victim.position()).normalize().scale(0.15);
                victim.setDeltaMovement(victim.getDeltaMovement().add(direction));
                victim.hurtMarked = true; // sync motion to client
            }

            // Ambient particles every few ticks
            if (remaining % 4 == 0 && victim.level() instanceof ServerLevel sl) {
                // Spiral particles around victim
                double angle = (remaining * 0.3) % (Math.PI * 2);
                double px = victim.getX() + Math.cos(angle) * 1.5;
                double pz = victim.getZ() + Math.sin(angle) * 1.5;
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, px, victim.getY() + 0.5, pz, 5, 0.1, 0.3, 0.1, 0.02);
                sl.sendParticles(ParticleTypes.PORTAL, victim.getX(), victim.getY() + 1, victim.getZ(), 8, 0.4, 0.8, 0.4, 0.1);
                sl.sendParticles(ParticleTypes.WITCH, victim.getX(), victim.getY() + 0.2, victim.getZ(), 3, 0.3, 0.1, 0.3, 0.01);
            }

            // Struggling sound
            if (remaining % 20 == 0) {
                victim.level().playSound(null, victim.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.4F, 0.3F);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.gravity_trap.desc1").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.liberthia.gravity_trap.desc2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
