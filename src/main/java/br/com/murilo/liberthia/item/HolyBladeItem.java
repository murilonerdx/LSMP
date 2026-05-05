package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.effect.ParticleEffectConfig;
import br.com.murilo.liberthia.effect.ParticleEffectEngine;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holy Blade — Order weapon. Extra smite vs monsters, fire aspect on hit,
 * periodic heal to wielder, golden cross particle burst on critical hits.
 */
public class HolyBladeItem extends SwordItem {

    public HolyBladeItem(Tier tier, int baseDmg, float atkSpeed, Properties props) {
        super(tier, baseDmg, atkSpeed, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean ok = super.hurtEnemy(stack, target, attacker);

        if (attacker.level() instanceof ServerLevel sl) {
            // Extra smite damage vs undead/corrupted
            if (target instanceof Monster) {
                target.hurt(attacker.damageSources().magic(), 4.0F);
            }

            // Fire aspect
            target.setSecondsOnFire(5);

            // Golden particles
            sl.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.4, 0.6, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + 0.8, target.getZ(),
                    10, 0.3, 0.4, 0.3, 0.05);

            sl.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.6F, 1.8F);

            // Lifesteal: 20% chance per hit
            if (attacker.getRandom().nextFloat() < 0.20F && attacker instanceof Player p) {
                p.heal(2.0F);
                sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.5, p.getZ(),
                        3, 0.3, 0.3, 0.3, 0.01);
            }
        }
        return ok;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);


        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        ParticleEffectConfig config = ParticleEffectConfig.builder()
                .size(0.18F, 0.55F)
                .alpha(0.95F)
                .lifetime(60)
                .count(24)
                .gravity(-0.01F)
                .friction(0.90F)
                .spin(0.04F)
                .physics(false)
                .emissive(true)
                .speed(0.045D)
                .upwardSpeed(0.015D)
                .spread(0.45D)
                .range(5.0D)
                .build();

        ParticleEffectEngine.meteorShowerTimed(
                serverLevel,
                player,
                config,
                80,    // duração
                8,     // quantidade de meteoros
                7.0D,   // distância mínima do player
                18.0D,  // raio de busca/alvo
                40.0D,  // altura
                2.4F,   // explosão
                2,      // fogo no chão
                80
        );

        if(!player.getAbilities().instabuild){
            stack.hurtAndBreak(20, player, (p) -> p.broadcastBreakEvent(hand));
        }
        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, 60);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!selected) return;
        if (!(entity instanceof Player p)) return;

        // Passive: brief regeneration + resistance while wielded
        if (p.tickCount % 100 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 110, 0, false, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6A luz da Ordem corta as trevas."));
        tooltip.add(Component.literal("§7+ Smite contra monstros").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7+ Fogo sagrado 5s").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7+ Regeneração passiva").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
