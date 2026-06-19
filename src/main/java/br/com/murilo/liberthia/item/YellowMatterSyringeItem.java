package br.com.murilo.liberthia.item;

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
 * Seringa de Matéria Amarela — injeta a matéria instável/energética: dá um surto
 * de Velocidade + Impulso de Salto. Carrega o perfil com Matéria Amarela.
 */
public class YellowMatterSyringeItem extends Item {

    public YellowMatterSyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(profile -> {
                profile.addYellow(40);
                br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(sp);
            });
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1600, 1));
            player.hurt(player.damageSources().magic(), 1.0F);

            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS, 0.8F, 1.5F);
            sp.displayClientMessage(Component.translatable("item.liberthia.yellow_matter_syringe.inject")
                    .withStyle(ChatFormatting.YELLOW), true);

            if (!player.isCreative()) stack.shrink(1);
            player.getCooldowns().addCooldown(this, 100);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.yellow_matter_syringe.desc")
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
