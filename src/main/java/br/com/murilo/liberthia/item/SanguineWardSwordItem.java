package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.effect.BloodInfectionEffect;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sanguine Ward Sword — diamond-tier blade. On every successful hit, removes
 * one amplifier of Blood Infection from the wielder (or fully cures it if
 * already at amp 0) and resets the max-HP drain counter by 2 HP.
 */
public class SanguineWardSwordItem extends SwordItem {
    public SanguineWardSwordItem(Properties props) {
        super(Tiers.DIAMOND, 4, -2.2F, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // v0.1.22 r9: bonus damage massivo contra dono de Boss Crown ATIVA.
        // User pediu que a espada de sangue continue funcionando contra o
        // boss. Boss Crown tem 3600 HP — sem buff a espada (4 ATK) demora
        // 900 hits pra matar. Com +12 HP de bonus + 3x multiplier vs Boss
        // Crown, vira ferramenta viável.
        if (target instanceof net.minecraft.world.entity.player.Player p) {
            boolean targetHasBossCrown = false;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.is(br.com.murilo.liberthia.registry.ModItems.BOSS_CROWN.get())
                        && br.com.murilo.liberthia.item.BossCrownItem.isActive(s)) {
                    targetHasBossCrown = true;
                    break;
                }
            }
            if (targetHasBossCrown) {
                // Dano bonus DIRETO bypass armor/effects. Soma 40 HP por hit.
                target.hurt(attacker.damageSources().magic(), 40.0F);
                if (target.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                            target.getX(), target.getY() + 1.0, target.getZ(),
                            15, 0.3, 0.5, 0.3, 0.2);
                }
            }
        }
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (hit && !attacker.level().isClientSide && ModEffects.BLOOD_INFECTION.get() != null) {
            var inst = attacker.getEffect(ModEffects.BLOOD_INFECTION.get());
            if (inst != null) {
                int amp = inst.getAmplifier();
                attacker.removeEffect(ModEffects.BLOOD_INFECTION.get());
                if (amp > 0) {
                    attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            ModEffects.BLOOD_INFECTION.get(),
                            inst.getDuration(),
                            amp - 1,
                            inst.isAmbient(),
                            inst.isVisible(),
                            inst.showIcon()));
                }
            }
            // Heal back some drain.
            var data = attacker.getPersistentData();
            double cur = data.getDouble(BloodInfectionEffect.NBT_DRAIN);
            if (cur > 0) data.putDouble(BloodInfectionEffect.NBT_DRAIN, Math.max(0, cur - 2.0));
        }
        return hit;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§c§oCada acerto purifica seu sangue."));
        tip.add(Component.literal("§7Reduz Infecção de Sangue em 1 nível e cura 2 ❤ de dreno."));
    }
}
