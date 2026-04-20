package br.com.murilo.liberthia.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/**
 * Blood Ritual Dagger — vanilla-esque sword with two extra behaviours:
 *  - Right-click (sneaking) self-cuts: pays 4 HP → grants Strength II + Speed II por 30s (SANGUINE_RUSH).
 *  - On kill via melee: 2 HP lifesteal.
 */
public class BloodRitualDaggerItem extends SwordItem {
    public BloodRitualDaggerItem(Tier tier, int atk, float speed, Properties p) {
        super(tier, atk, speed, p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (player.getHealth() <= 4.5F) return InteractionResultHolder.fail(stack);

        if (!level.isClientSide) {
            player.hurt(player.damageSources().magic(), 4.0F);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 1));
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        20, 0.4, 0.4, 0.4, 0.2);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                    SoundSource.PLAYERS, 0.8F, 0.5F);
        }
        stack.hurtAndBreak(2, player, pl -> pl.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && target.getHealth() <= 0.0F && attacker instanceof Player p) {
            // lifesteal
            p.heal(2.0F);
            if (p.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART,
                        p.getX(), p.getY() + 1.8, p.getZ(),
                        3, 0.2, 0.2, 0.2, 0.0);
            }
        }
        return result;
    }
}
