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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Freeze Staff — right-click a player to completely freeze them.
 * They cannot move, look around, or interact with inventory.
 */
public class FreezeStaffItem extends Item {

    public static final Map<UUID, FreezeData> FROZEN_PLAYERS = new HashMap<>();

    public record FreezeData(Vec3 pos, float yRot, float xRot, int ticksLeft) {}

    private static final int DURATION = 160; // 8 seconds

    public FreezeStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(target instanceof ServerPlayer victim)) return InteractionResult.PASS;

        FROZEN_PLAYERS.put(victim.getUUID(), new FreezeData(
                victim.position(), victim.getYRot(), victim.getXRot(), DURATION
        ));

        // Visual freeze
        victim.setTicksFrozen(DURATION + 40); // Powdered snow freeze visual
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION, 255, false, false, false));
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, DURATION, 255, false, false, false));
        victim.addEffect(new MobEffectInstance(MobEffects.JUMP, DURATION, 128, false, false, false));

        // Freeze particles
        if (victim.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY() + 1, victim.getZ(), 50, 0.5, 1.0, 0.5, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, victim.getX(), victim.getY() + 1, victim.getZ(), 20, 0.3, 0.8, 0.3, 0.01);
            sl.playSound(null, victim.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.5F, 0.3F);
            sl.playSound(null, victim.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 2.0F, 0.5F);
        }

        victim.sendSystemMessage(Component.translatable("chat.liberthia.frozen").withStyle(ChatFormatting.AQUA));
        user.sendSystemMessage(Component.translatable("chat.liberthia.freeze_used", victim.getDisplayName()).withStyle(ChatFormatting.GOLD));

        user.getCooldowns().addCooldown(this, 200);
        return InteractionResult.SUCCESS;
    }

    /**
     * Tick from InfectionEvents — locks frozen players in place.
     */
    public static void tickFrozen(ServerLevel level) {
        var it = FROZEN_PLAYERS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null || !p.isAlive()) { it.remove(); continue; }

            FreezeData data = entry.getValue();
            int remaining = data.ticksLeft() - 1;
            if (remaining <= 0) {
                it.remove();
                p.setTicksFrozen(0);
                p.sendSystemMessage(Component.translatable("chat.liberthia.thawed").withStyle(ChatFormatting.GREEN));
                continue;
            }
            entry.setValue(new FreezeData(data.pos(), data.yRot(), data.xRot(), remaining));

            // Lock position completely
            p.teleportTo(data.pos().x, data.pos().y, data.pos().z);
            p.setYRot(data.yRot());
            p.setXRot(data.xRot());
            p.setYHeadRot(data.yRot());
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;

            // Keep frozen visual
            p.setTicksFrozen(remaining + 40);

            // Close any open container (locks inventory)
            if (p.containerMenu != p.inventoryMenu) {
                p.closeContainer();
            }

            // Ice particles
            if (remaining % 6 == 0 && p.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 5, 0.3, 0.6, 0.3, 0.01);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 0.5, p.getZ(), 3, 0.2, 0.4, 0.2, 0.005);
            }

            // Cracking sound
            if (remaining % 40 == 0) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.3F, 1.5F);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.freeze_staff.desc1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.liberthia.freeze_staff.desc2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
