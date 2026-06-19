package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

import java.util.List;

/**
 * Seringa de Matéria Escura — o oposto da seringa Clear/White. Injeta Matéria
 * Escura no perfil do player: poder sombrio (Força + Visão Noturna) ao custo de
 * subir a infecção. Para quem abraça a corrupção.
 */
public class DarkMatterSyringeItem extends Item {

    public DarkMatterSyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(profile -> {
                profile.addDark(40);
                br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(sp);
            });
            player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                data.setInfection(Math.min(100, data.getInfection() + 15));
                data.setDirty(true);
            });
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0));
            player.hurt(player.damageSources().magic(), 2.0F);

            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS, 0.8F, 0.6F);
            sp.displayClientMessage(Component.translatable("item.liberthia.dark_matter_syringe.inject")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);

            if (!player.isCreative()) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 100);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.dark_matter_syringe.desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.liberthia.dark_matter_syringe.warn")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
