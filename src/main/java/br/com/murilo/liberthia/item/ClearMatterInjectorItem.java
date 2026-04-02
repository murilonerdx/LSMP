package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.backend.BackendClient;
import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ClearMatterInjectorItem extends Item {
    public ClearMatterInjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean success = applyCure(serverPlayer, serverPlayer);
            if (success) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.getCooldowns().addCooldown(this, 100);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide) {
            boolean success = applyCure(player, target);
            if (success) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.getCooldowns().addCooldown(this, 100);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private boolean applyCure(Player source, LivingEntity target) {
        var opt = target.getCapability(ModCapabilities.INFECTION).resolve();
        if (opt.isEmpty()) return false;

        var data = opt.get();
        int before = data.getInfection();

        data.reduceInfection(30);
        data.reducePermanentHealthPenalty(2);

        // Cleanse ALL mutations
        data.setMutations("");
        data.setDirty(true);

        // Remove negative potion effects from infection
        target.removeEffect(ModEffects.DARK_INFECTION.get());
        target.removeEffect(ModEffects.RADIATION_SICKNESS.get());
        target.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        target.removeEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
        target.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        target.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        target.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
        target.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        target.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);

        // Apply 3-minute immunity shield
        target.addEffect(new MobEffectInstance(ModEffects.CLEAR_SHIELD.get(), 3600, 0, false, true, true));

        if (target instanceof ServerPlayer serverTarget) {
            InfectionLogic.applyDerivedEffects(serverTarget, data);
            InfectionLogic.sync(serverTarget, data);
            BackendClient.sendSnapshot(serverTarget, data);
        }

        source.level().playSound(null, target.blockPosition(), ModSounds.CLEAR_HUM.get(), SoundSource.PLAYERS, 0.9F, 1.25F);

        // Return true if it was effective
        return before > data.getInfection() || target.hasEffect(ModEffects.CLEAR_SHIELD.get());
    }
}
