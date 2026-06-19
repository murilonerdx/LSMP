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
 * v0.1.51: Dark Matter Pill — pílula comestível.
 *
 * <p>Comportamento (alterado por pedido do user):
 * <ul>
 *   <li><b>NÃO reduz mais a Dark Matter acumulada</b> no perfil.</li>
 *   <li>Aplica {@link ModEffects#DARK_MATTER_RESISTANCE} por <b>30 minutos</b>:
 *       enquanto ativo, o player não GANHA Dark Matter por proximidade (blocos,
 *       blood tree, mobs DM) e os efeitos negativos derivados de DM
 *       (Wither, etc.) são atenuados/bloqueados.</li>
 *   <li>É <b>comestível</b> ({@code Item.Properties.food}) — anim de comer
 *       padrão (~32 ticks), pode ser comida mesmo com fome cheia
 *       ({@code alwaysEat()}).</li>
 * </ul>
 *
 * <p>A {@link FoodProperties.Builder#effect} já aplica o MobEffect quando
 * o player termina de comer — não precisa de {@code finishUsingItem} custom.
 */
public class DarkMatterPillItem extends Item {

    public static FoodProperties FOOD() {
        return new FoodProperties.Builder()
                .nutrition(0)
                .saturationMod(0f)
                .alwaysEat()
                .effect(() -> new MobEffectInstance(
                        ModEffects.DARK_MATTER_RESISTANCE.get(),
                        MatterResistance.DURATION_TICKS,
                        0, false, true, true), 1.0f)
                .build();
    }

    public DarkMatterPillItem(Properties properties) {
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
        // O efeito já foi aplicado pelo .food().effect() acima. Só logamos
        // diagnóstico e delegamos.
        return super.finishUsingItem(stack, level, entity);
    }
}
