package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r180: <b>Quebra-Feitiços</b> — espada anti-mago. Ao acertar, <b>disrompe o cast</b>
 * do alvo (Fraqueza + Fadiga + Lentidão + Náusea + Escuridão) e drena vida pro portador.
 * Contra magos do <b>Mana and Artifice</b> (classe {@code com.mna}) o efeito é mais forte/longo.
 */
public class SpellbreakerItem extends SwordItem {

    public SpellbreakerItem(Properties props) {
        super(Tiers.NETHERITE, 5, -2.2F, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean ok = super.hurtEnemy(stack, target, attacker);
        if (ok && !target.level().isClientSide) {
            boolean isMage = target.getClass().getName().startsWith("com.mna");
            int dur = isMage ? 160 : 80;
            int amp = isMage ? 2 : 1;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, amp, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, dur, amp, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 1, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, true));
            attacker.heal(isMage ? 3.0F : 1.0F);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 14, 0.3, 0.4, 0.3, 0.06);
            }
        }
        return ok;
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§5Ao acertar: disrompe o cast do alvo").withStyle(ChatFormatting.DARK_PURPLE));
        tip.add(Component.literal("§7Fraqueza + Fadiga + Lentidão + Náusea + Escuridão · drena vida"));
        tip.add(Component.literal("§d✦ Dobro contra magos do Mana and Artifice"));
    }
}
