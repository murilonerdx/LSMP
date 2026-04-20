package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Blood Cure Pill — fully removes Blood Infection effect AND drain NBT.
 */
public class BloodCurePillItem extends Item {

    public BloodCurePillItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide) {
            // 1. Clear the effect instance
            user.removeEffect(ModEffects.BLOOD_INFECTION.get());

            // 2. Remove HP-drain modifier + saved data + NBT
            BloodInfectionApplier.clear(user);

            // 3. Remove ALL harmful effects (player wants "tirar os efeitos")
            user.getActiveEffects().stream()
                    .filter(e -> e.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                    .map(e -> e.getEffect())
                    .toList()
                    .forEach(user::removeEffect);

            // 4. Force max-health attribute refresh and restore health
            var attr = user.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (attr != null) {
                // safety net: kill any lingering modifier from our UUID
                var mod = attr.getModifier(java.util.UUID.fromString("e5a2c3f1-7b4d-4a9e-8f1c-2a3b4c5d6e7f"));
                if (mod != null) attr.removeModifier(mod);
            }
            user.setHealth(user.getMaxHealth());

            // 5. Positive buffs so the heal feels real
            user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));
            user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 0, false, true, true));

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, user.getX(), user.getY() + 1.2, user.getZ(),
                        12, 0.4, 0.4, 0.4, 0.02);
                sl.sendParticles(ParticleTypes.EFFECT, user.getX(), user.getY() + 1.0, user.getZ(),
                        20, 0.4, 0.5, 0.4, 0.05);
                sl.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.5F);
            }

            if (user instanceof Player p) {
                p.displayClientMessage(Component.literal("§aInfecção curada! Corações restaurados."), true);
                if (!p.getAbilities().instabuild) stack.shrink(1);
            } else {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§cCura Infecção de Sangue").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("§7Restaura corações perdidos").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
