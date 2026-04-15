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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EquilibriumFragmentItem extends Item {

    public EquilibriumFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (player.tickCount % 40 != 0) return;

        // Took damage recently → Resistance
        if (player.hurtTime > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false, true));
        }

        // Standing still → Regeneration
        double speed = player.getDeltaMovement().horizontalDistanceSqr();
        if (speed < 0.0001) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false, true));
        }

        // Recently attacked something → Strength
        if (player.getLastHurtMob() != null && player.tickCount - player.getLastHurtMobTimestamp() < 60) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, false, true));
        }

        // Emotional lapse: every ~15s, 20% chance of confusion
        if (player.tickCount % 300 == 0 && level.random.nextFloat() < 0.20F) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Um fragmento da Ilha Equilibrium").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Amplifica emoções do portador").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Cuidado com os lapsos...").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0, false, true, true));
            player.sendSystemMessage(Component.translatable("chat.liberthia.equilibrium_use").withStyle(ChatFormatting.GOLD));
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.getCooldowns().addCooldown(this, 600);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
