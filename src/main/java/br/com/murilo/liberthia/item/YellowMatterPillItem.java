package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.matter.MatterResistance;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * v0.1.51: Yellow Matter Pill — pílula comestível.
 *
 * <p>NÃO reduz mais a Yellow Matter acumulada. Aplica
 * {@link ModEffects#YELLOW_MATTER_RESISTANCE} por 30 min — bloqueia ganho
 * de YM e os efeitos negativos derivados (Hunger, Nausea, Unluck, etc.).
 *
 * <p>Comestível via {@link FoodProperties} ({@code alwaysEat()}, sem nutrição,
 * só efeito).
 */
public class YellowMatterPillItem extends Item {

    public static FoodProperties FOOD() {
        return new FoodProperties.Builder()
                .nutrition(0)
                .saturationMod(0f)
                .alwaysEat()
                .effect(() -> new MobEffectInstance(
                        ModEffects.YELLOW_MATTER_RESISTANCE.get(),
                        MatterResistance.DURATION_TICKS,
                        0, false, true, true), 1.0f)
                .build();
    }

    public YellowMatterPillItem(Properties properties) {
        super(properties.food(FOOD()));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return super.finishUsingItem(stack, level, entity);
    }
}
