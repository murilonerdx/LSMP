package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.capability.IInfectionData;
import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class ClearMatterPillItem extends Item {
    public ClearMatterPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                if (!data.canTakePills()) {
                    serverPlayer.displayClientMessage(Component.literal("§c⚠ As pílulas não fazem mais efeito. Você precisa de uma injeção de cura!"), true);
                    return;
                }

                // If taking the pill for the first time or renewing
                data.setPillTimer(36000); // 30 minutes (20 ticks/sec * 60 * 30)
                data.reduceInfection(10); 
                data.setDirty(true);
                
                player.removeEffect(ModEffects.DARK_INFECTION.get());
                player.removeEffect(ModEffects.RADIATION_SICKNESS.get());
                player.addEffect(new MobEffectInstance(ModEffects.CLEAR_SHIELD.get(), 1200, 0, false, true, true));

                level.playSound(null, player.blockPosition(), ModSounds.CLEAR_HUM.get(),
                        SoundSource.PLAYERS, 0.8F, 1.2F);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                player.getCooldowns().addCooldown(this, 100);
                InfectionLogic.sync(serverPlayer, data);
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 16;
    }
}
