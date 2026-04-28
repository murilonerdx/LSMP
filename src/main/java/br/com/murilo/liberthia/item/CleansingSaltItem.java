package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Cleansing Salt — single-use, instant burst around the holder. No projectile,
 * no aim required. Pops a 4-block sphere that:
 *
 *   • Removes Blood Infection from every LivingEntity in range (including the
 *     thrower).
 *   • Damages BloodKin entities for 5 hp.
 *
 * Stack of 16. Cooldown 5s.
 */
public class CleansingSaltItem extends Item {
    public CleansingSaltItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerLevel sl = (ServerLevel) level;
        double radius = 4.0;
        AABB box = new AABB(player.position(), player.position()).inflate(radius);

        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            // Cleanse infection from anyone in range.
            if (ModEffects.BLOOD_INFECTION.get() != null
                    && le.hasEffect(ModEffects.BLOOD_INFECTION.get())) {
                le.removeEffect(ModEffects.BLOOD_INFECTION.get());
            }
            BloodInfectionApplier.clear(le);

            // Damage blood-kin caught in the burst.
            if (BloodKin.is(le) && le != player) {
                le.hurt(le.damageSources().magic(), 5.0F);
            }
        }

        // Visuals: salt shimmer + small smoke puff.
        sl.sendParticles(ParticleTypes.GLOW,
                player.getX(), player.getY() + 1.0, player.getZ(),
                28, radius * 0.6, 0.6, radius * 0.6, 0.05);
        sl.sendParticles(ParticleTypes.WHITE_ASH,
                player.getX(), player.getY() + 1.0, player.getZ(),
                40, radius * 0.7, 0.4, radius * 0.7, 0.02);

        sl.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);

        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§7Estouro instantâneo §a4 blocos§7 ao redor."));
        tip.add(Component.literal("§7Cura Infecção de Sangue. Dano §c5 ❤§7 em vermes."));
    }
}
