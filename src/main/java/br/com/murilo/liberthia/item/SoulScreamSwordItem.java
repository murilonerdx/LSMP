package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.BloodKin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Soul Scream Sword — handheld version of the {@code screaming_soul} block.
 * On hit: Darkness + Wither I + Confusion + small soul-fire flame burst.
 */
public class SoulScreamSwordItem extends SwordItem {
    public SoulScreamSwordItem(Properties props) {
        super(Tiers.IRON, 4, -2.4F, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (hit && !attacker.level().isClientSide && !BloodKin.is(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, true, true));

            ServerLevel sl = (ServerLevel) attacker.level();
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.4, 0.6, 0.4, 0.05);
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.WARDEN_AGITATED, SoundSource.PLAYERS, 0.7F, 1.6F);
        }
        return hit;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        super.appendHoverText(stack, l, tip, f);
        tip.add(Component.literal("§7Acertos aplicam §8Escuridão§7 + Wither + Confusão"));
    }
}
