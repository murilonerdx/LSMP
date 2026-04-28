package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ported from EvilCraft's {@code ItemDarkenedApple}. A "deal-with-the-devil"
 * apple — gives strong short-term healing in exchange for long-lasting
 * Hemo Sickness. Always edible.
 */
public class TaintedAppleItem extends Item {

    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(3)
            .saturationMod(0.3F)
            .alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 20 * 30, 1), 1.0F)
            .build();

    public TaintedAppleItem(Properties props) {
        super(props.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && ModEffects.HEMO_SICKNESS.get() != null) {
            entity.addEffect(new MobEffectInstance(ModEffects.HEMO_SICKNESS.get(), 20 * 30, 1, false, true, true));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
