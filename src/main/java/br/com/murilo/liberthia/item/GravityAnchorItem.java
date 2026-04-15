package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Gravity Anchor — grounds all nearby players for 8 seconds.
 * They cannot fly, jump, or move. Pulled to the ground constantly.
 */
public class GravityAnchorItem extends Item {

    public static final Map<UUID, Integer> GROUNDED_PLAYERS = new HashMap<>();
    private static final double RANGE = 15.0;
    private static final int DURATION = 160; // 8 seconds

    public GravityAnchorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerLevel sl = (ServerLevel) level;
        AABB area = player.getBoundingBox().inflate(RANGE);
        List<ServerPlayer> targets = sl.getEntitiesOfClass(ServerPlayer.class, area, p -> p != player);

        for (ServerPlayer target : targets) {
            GROUNDED_PLAYERS.put(target.getUUID(), DURATION);

            // Disable flying
            if (target.getAbilities().flying) {
                target.getAbilities().flying = false;
                target.onUpdateAbilities();
            }

            // Heavy debuffs
            target.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DURATION, 0, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION, 4, false, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.JUMP, DURATION, 128, false, false, true)); // negative jump = no jump

            // Slam to ground
            target.setDeltaMovement(0, -2.0, 0);
            target.hurtMarked = true;

            // Particles - heavy chains
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY(), target.getZ(), 30, 0.5, 0.1, 0.5, 0.01);
            sl.sendParticles(ParticleTypes.ASH, target.getX(), target.getY() + 1, target.getZ(), 40, 1.0, 1.5, 1.0, 0.05);

            target.sendSystemMessage(Component.translatable("chat.liberthia.grounded").withStyle(ChatFormatting.DARK_RED));
        }

        if (!targets.isEmpty()) {
            player.sendSystemMessage(Component.translatable("chat.liberthia.anchor_used", targets.size()).withStyle(ChatFormatting.GOLD));
            sl.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
            sl.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.2F, 0.3F);
        }

        stack.shrink(1);
        player.getCooldowns().addCooldown(this, 200);
        return InteractionResultHolder.success(stack);
    }

    /**
     * Tick from InfectionEvents — keeps grounded players stuck.
     */
    public static void tickGrounded(ServerLevel level) {
        var it = GROUNDED_PLAYERS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (p == null || !p.isAlive()) { it.remove(); continue; }

            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
                p.sendSystemMessage(Component.translatable("chat.liberthia.anchor_released").withStyle(ChatFormatting.GREEN));
                continue;
            }
            entry.setValue(remaining);

            // Keep them grounded
            if (p.getAbilities().flying) {
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
            Vec3 mot = p.getDeltaMovement();
            // Cancel upward motion, force down
            if (mot.y > 0) {
                p.setDeltaMovement(mot.x * 0.3, -0.5, mot.z * 0.3);
                p.hurtMarked = true;
            }

            // Chain particles
            if (remaining % 5 == 0 && p.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 0.1, p.getZ(), 4, 0.3, 0.05, 0.3, 0.005);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.gravity_anchor.desc1").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.liberthia.gravity_anchor.desc2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
