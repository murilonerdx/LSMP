package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.projectile.HemoBoltEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Hemomancer Staff — dispara um HemoBolt pagando 2 HP. Cooldown 30t normal, 15t com Blood Infection ativa.
 */
public class HemomancerStaffItem extends Item {
    public HemomancerStaffItem(Properties p) {
        super(p.durability(256).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (player.getHealth() <= 2.5F) {
            return InteractionResultHolder.fail(stack);
        }

        boolean infected = player.hasEffect(br.com.murilo.liberthia.registry.ModEffects.BLOOD_INFECTION.get());
        int cd = infected ? 15 : 30;

        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle();
            HemoBoltEntity bolt = new HemoBoltEntity(ModEntities.HEMO_BOLT.get(), level);
            bolt.setOwner(player);
            bolt.setPos(player.getX() + look.x, player.getEyeY() - 0.1, player.getZ() + look.z);
            bolt.setDeltaMovement(look.scale(1.2));
            level.addFreshEntity(bolt);

            // pay 2 HP
            player.hurt(player.damageSources().magic(), 2.0F);

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX(), player.getEyeY(), player.getZ(),
                        12, 0.4, 0.4, 0.4, 0.1);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        stack.hurtAndBreak(1, player, pl -> pl.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, cd);
        return InteractionResultHolder.consume(stack);
    }
}
