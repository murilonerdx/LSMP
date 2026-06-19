package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r179: <b>Tônico de Lucidez</b> — beba pra restaurar sanidade (+30) e limpar a
 * confusão/escuridão da insanidade. Um respiro num mundo que te observa.
 */
public class LucidTonicItem extends Item {

    private static final int RESTORE = 30;

    public LucidTonicItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            SpiritDimension.addSanity(sp, RESTORE);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
            sp.removeEffect(MobEffects.CONFUSION);
            sp.removeEffect(MobEffects.DARKNESS);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.2, sp.getZ(), 14, 0.3, 0.5, 0.3, 0.01);
                sl.sendParticles(ParticleTypes.INSTANT_EFFECT, sp.getX(), sp.getY() + 1.0, sp.getZ(), 8, 0.3, 0.4, 0.3, 0.0);
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7F, 1.1F);
            sp.displayClientMessage(Component.literal("§bSua mente clareia. (+" + RESTORE + " sanidade)"), true);
        }
        if (entity instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§bBeba: §f+" + RESTORE + " sanidade"));
        tooltip.add(Component.literal("§7Limpa a confusão e a escuridão da loucura."));
        tooltip.add(Component.literal("§8§oTambém recupera sanidade ficando no §rsol§8§o."));
    }
}
