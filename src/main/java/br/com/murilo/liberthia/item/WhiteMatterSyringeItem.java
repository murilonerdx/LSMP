package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * White Matter Syringe — emergency purification injection.
 * More powerful than Clear Matter Injector: fully cures infection,
 * removes all mutations, grants temporary immunity, and heals HP.
 */
public class WhiteMatterSyringeItem extends Item {

    public WhiteMatterSyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                // Full cure
                data.setInfection(0);
                data.setMutations("");
                data.setImmune(true);
                data.setPermanentHealthPenalty(Math.max(0, data.getPermanentHealthPenalty() - 4));
                data.setDirty(true);

                // Remove all negative effects
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.BLINDNESS);
                player.removeEffect(MobEffects.CONFUSION);
                player.removeEffect(MobEffects.HUNGER);
                player.removeEffect(MobEffects.DIG_SLOWDOWN);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

                // Grant strong immunity + regen
                player.addEffect(new MobEffectInstance(ModEffects.CLEAR_SHIELD.get(), 6000)); // 5 minutes
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));

                player.displayClientMessage(
                        Component.literal("White Matter purification complete!")
                                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);
            });

            level.playSound(null, player.blockPosition(), ModSounds.CLEAR_HUM.get(),
                    SoundSource.PLAYERS, 1.0F, 1.5F);

            if (!player.isCreative()) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 200);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.liberthia.white_matter_syringe.desc")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.liberthia.white_matter_syringe.effect")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
