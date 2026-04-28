package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.network.chat.Component;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Tainted Essence — drop from killed Blood-kin creatures (worms, hounds,
 * cultists, mage, etc.). Right-click to consume: instantly cures Blood
 * Infection on self and grants Regeneration II for 5s. Cooldown 60s.
 */
public class TaintedEssenceItem extends Item {
    public TaintedEssenceItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // Cure infection.
        if (ModEffects.BLOOD_INFECTION.get() != null
                && player.hasEffect(ModEffects.BLOOD_INFECTION.get())) {
            player.removeEffect(ModEffects.BLOOD_INFECTION.get());
        }
        BloodInfectionApplier.clear(player);

        // Restorative buff.
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
        player.heal(2.0F);

        level.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7F, 1.4F);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(this, 1200);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§cConsumir: §acura a Infecção de Sangue."));
        tip.add(Component.literal("§7Drop de vermes/cultistas/mages do sangue."));
    }
}
