package br.com.murilo.liberthia.magic.spells;

import br.com.murilo.liberthia.magic.effect.SynergyEffects;
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
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * v0.1.24 r108: <b>Ascension</b> — empurra um target (ou self com shift)
 * verticalmente. Aplica MobEffect {@link
 * br.com.murilo.liberthia.magic.effect.AscensionEffect}.
 */
public class AscensionSpellItem extends Item {

    public AscensionSpellItem(Properties props) {
        super(props.stacksTo(1).durability(60));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        ServerLevel sl = sp.serverLevel();

        LivingEntity target = sp;
        if (!sp.isShiftKeyDown()) {
            // r179 FIX: raycast de entidade real (Entity.pick() só pega blocos)
            LivingEntity le = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 20.0);
            if (le != null) target = le;
        }

        // Aplica Ascension effect (5s)
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                SynergyEffects.ASCENSION.get(), 100, 0, true, true));

        // Knockup burst inicial
        target.setDeltaMovement(target.getDeltaMovement().x, 1.5, target.getDeltaMovement().z);
        target.hasImpulse = true;
        target.fallDistance = 0;

        // Particles
        for (int i = 0; i < 20; i++) {
            sl.sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY(), target.getZ(),
                    1, 0.3, 0.1, 0.3, 0.1);
        }
        sl.playSound(null, target.blockPosition(),
                net.minecraft.sounds.SoundEvents.PHANTOM_FLAP,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.5F);

        sp.getCooldowns().addCooldown(this, 100);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        sp.displayClientMessage(Component.literal("§e§l✦ Ascend!"), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§eEmpurra alvo verticalmente 5s"));
        tooltip.add(Component.literal("§7Shift+R-click: usar em si mesmo"));
        tooltip.add(Component.literal("§7R-click no alvo: target enemy"));
    }
}
