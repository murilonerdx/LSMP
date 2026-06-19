package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.util.EntityRaycast;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;

/**
 * r180: <b>Selador de Magia</b> — mire num alvo e use: ele recebe <b>Antimagia por 20s</b>
 * (não voa, magia anulada). O selo da linha anti-magia. Craft com matéria escura.
 */
public class MagicSealItem extends Item {

    private static final int SEAL_TICKS = 400; // 20s
    private static final double REACH = 14.0;

    public MagicSealItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        LivingEntity target = EntityRaycast.pickLiving(player, REACH);
        if (target == null || target == player) {
            sp.displayClientMessage(Component.literal("§7Mire em um alvo pra selar a magia dele."), true);
            return InteractionResultHolder.fail(stack);
        }
        target.addEffect(new MobEffectInstance(ModEffects.ANTIMAGIA.get(), SEAL_TICKS, 0, false, true, true));
        var cyan = new net.minecraft.core.particles.DustParticleOptions(new Vector3f(0.17F, 0.84F, 0.84F), 1.4F);
        sl.sendParticles(cyan, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 30, 0.4, 0.7, 0.4, 0.0);
        sl.playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        sp.displayClientMessage(Component.literal("§b✦ " + target.getName().getString() + " §3selado (sem magia por 20s)")
                .withStyle(ChatFormatting.AQUA), true);
        player.getCooldowns().addCooldown(this, 120);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§3Mire e use: §bAntimagia por 20s no alvo"));
        tip.add(Component.literal("§7Sem voo, magia anulada."));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }
}
