package br.com.murilo.liberthia.magic.spells;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r108: <b>Ice Tomb</b> — freezes target em statue por 5s + spawns
 * {@link FrozenHumanoidEntity} statue no lugar.
 *
 * <p>Right-click em alvo → target ganha SLOWNESS X + freeze ticks + statue
 * aparece. Target invulnerável (statue absorve dano).
 */
public class IceTombSpellItem extends Item {

    public IceTombSpellItem(Properties props) {
        super(props.stacksTo(1).durability(40));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        // r179 FIX: Entity.pick() só detecta BLOCOS — usar raycast de entidade real.
        LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 20.0);
        if (target == null) {
            sp.displayClientMessage(Component.literal("§7§o✦ Aponte pra um alvo"), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel sl = sp.serverLevel();

        // Freeze 100t + slowness X + dano leve
        target.setTicksFrozen(target.getTicksFrozen() + 100);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 250, true, true));
        target.hurt(SchoolDamageSource.ice(100).toVanilla(sl, sp), 4F);

        // Spawn FrozenHumanoid statue on top
        var statue = br.com.murilo.liberthia.registry.ModEntities.FROZEN_HUMANOID.get().create(sl);
        if (statue != null) {
            statue.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), 0);
            sl.addFreshEntity(statue);
        }

        // Ice burst particles
        for (int i = 0; i < 30; i++) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + 1, target.getZ(),
                    1, 0.5, 1.0, 0.5, 0.1);
        }
        sl.playSound(null, target.blockPosition(),
                net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.7F);

        sp.getCooldowns().addCooldown(this, 200);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§bCongela alvo + spawna statue"));
        tooltip.add(Component.literal("§7Freeze §b100t §7+ Slowness X"));
    }
}
