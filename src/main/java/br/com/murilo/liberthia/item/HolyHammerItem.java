package br.com.murilo.liberthia.item;

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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Holy Hammer — Order weapon. Heavy knockback on hit + right-click releases
 * a radial light shockwave that damages monsters and pushes players back.
 */
public class HolyHammerItem extends SwordItem {

    public HolyHammerItem(Tier tier, int baseDmg, float atkSpeed, Properties props) {
        super(tier, baseDmg, atkSpeed, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean ok = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel sl) {
            // Heavy knockback
            Vec3 dir = target.position().subtract(attacker.position()).normalize();
            target.push(dir.x * 1.5, 0.55, dir.z * 1.5);
            target.hurtMarked = true;

            sl.sendParticles(ParticleTypes.FLASH,
                    target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 0.5, target.getZ(),
                    12, 0.4, 0.4, 0.4, 0.1);
            sl.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        return ok;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerLevel sl = (ServerLevel) level;
        double radius = 8.0;
        AABB area = player.getBoundingBox().inflate(radius);

        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            Vec3 dir = e.position().subtract(player.position()).normalize();
            if (dir.lengthSqr() < 0.001) dir = new Vec3(0, 0.5, 0);
            e.push(dir.x * 1.2, 0.5, dir.z * 1.2);

            float dmg = e instanceof Monster ? 10.0F : 4.0F;
            e.hurt(level.damageSources().magic(), dmg);
            e.hurtMarked = true;

            if (e instanceof Monster) {
                e.setSecondsOnFire(4);
            }
        }

        // Radial shockwave particles
        for (int i = 0; i < 60; i++) {
            double ang = i * (Math.PI * 2.0 / 60.0);
            double rx = Math.cos(ang) * radius;
            double rz = Math.sin(ang) * radius;
            sl.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + rx, player.getY() + 0.2, player.getZ() + rz,
                    1, 0, 0.05, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.2F);
        sl.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.4F);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false, true));
        player.getCooldowns().addCooldown(this, 120);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6O peso da verdade."));
        tooltip.add(Component.literal("§7+ Knockback pesado").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7Clique direito: onda de luz").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
