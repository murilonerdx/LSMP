package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.cosmic.ICosmicHorror;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * r194 — <b>Telescópio Dimensional</b> (do Núcleo da Constelação). Direito: visão noturna +
 * revela (glow) todas as criaturas cósmicas num raio de 48 blocos por 15s.
 */
public class DimensionalTelescopeItem extends Item {
    public DimensionalTelescopeItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, false, false));
            int n = 0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48), e -> e instanceof ICosmicHorror)) {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
                n++;
            }
            int found = n;
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§9Telescópio: §f" + found + " §7criatura(s) cósmica(s) revelada(s)."), true);
            sl.playSound(null, player.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1f, 0.8f);
            player.getCooldowns().addCooldown(this, 100);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
