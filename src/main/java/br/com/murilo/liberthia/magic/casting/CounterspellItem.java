package br.com.murilo.liberthia.magic.casting;

import br.com.murilo.liberthia.magic.wizard.AbstractWizardEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * v0.1.24 r91: <b>Counterspell</b> — item que interrompe casts de wizard mobs
 * num raio.
 *
 * <p>Right-click → todos AbstractWizardEntity num raio de 16 ganham WEAKNESS
 * + SLOWNESS por 5s + cooldown +200t (impede cast). Useful contra wizard
 * encounters em alta dificuldade.
 *
 * <p>Cooldown 30s.
 */
public class CounterspellItem extends Item {

    public CounterspellItem(Properties props) {
        super(props.stacksTo(1).durability(0));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            sp.displayClientMessage(Component.literal("§5✦ Counterspell em recarga"), true);
            return InteractionResultHolder.fail(stack);
        }

        int interrupted = 0;
        var wizards = sp.serverLevel().getEntitiesOfClass(
                AbstractWizardEntity.class, sp.getBoundingBox().inflate(16));
        for (var w : wizards) {
            w.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, true, true));
            w.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2, true, true));
            // Reset cooldown — força esperar
            w.resetCooldown();
            interrupted++;
        }

        // Particle effect
        if (sp.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    sp.getX(), sp.getY() + 1, sp.getZ(), 30, 2, 1, 2, 0.1);
        }
        sp.level().playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 1.0F);

        sp.displayClientMessage(Component.literal(
                "§5§l✦ §r§5" + interrupted + " §5wizards interrompidos"), false);
        sp.getCooldowns().addCooldown(this, 600);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5Interrompe casts de wizards"));
        tooltip.add(Component.literal("§7Raio: §b16 §7blocos"));
        tooltip.add(Component.literal("§8§oCooldown: 30s"));
    }
}
