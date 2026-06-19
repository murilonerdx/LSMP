package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Daily Pill — pílula de manutenção que reduz exposição passiva por 24000
 * ticks (1 dia in-game).
 *
 * <p>Diferente do MatterCure, ela não ZERA — só dá um "buffer" de proteção:
 * <ul>
 *   <li>Resistance I + Regen I por 24000 ticks (20min de tempo real)</li>
 *   <li>Absorption I (4 HP de escudo) por 1200 ticks (1min) pra emergências</li>
 *   <li>Slowness 0 imediato — micro-tonteira da pílula fazendo efeito</li>
 * </ul>
 *
 * <p>Stack até 16. Receita: 1 Clear Matter Pill + 1 Sugar + 1 Glow Berry =
 * 4 Daily Pills.
 */
public class DailyPillItem extends Item {

    public DailyPillItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            // Duração principal: 1 dia in-game = 24000 ticks (~20min real)
            // Não usa Long.MAX, então hits de exposição NÃO sumem — só amortecidos.
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24000, 0)); // Resistance I
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 24000, 0));      // Regen I
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 0));         // Absorption I (1min)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));    // Tonteira leve 3s

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_DRINK, player.getSoundSource(), 1.0f, 1.3f);

            player.displayClientMessage(
                    Component.literal("💊 Pílula consumida. Proteção ativa por 1 dia.")
                            .withStyle(ChatFormatting.YELLOW),
                    true);
        }

        if (user instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24; // mais rápido que poção (é só engolir)
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Manutenção diária — toma 1x por dia")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("• Resistance I + Regen I (20min)")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("• +2 corações de absorção (1min)")
                .withStyle(ChatFormatting.GOLD));
    }
}
