package br.com.murilo.liberthia.magic.crop;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * v0.1.24 r103: <b>Source Berry</b> — alimento que restaura source bonus.
 *
 * <p>Comer +1 fome, satura 0.3, +50 NBT liberthia.bonus_source ao player.
 */
public class SourceBerryItem extends ItemNameBlockItem {

    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(2).saturationMod(0.3F).fast().build();

    public SourceBerryItem(Block block, Properties props) {
        super(block, props.food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer sp) {
            int current = sp.getPersistentData().getInt("liberthia.bonus_source");
            sp.getPersistentData().putInt("liberthia.bonus_source",
                    Math.min(500, current + 50));
            sp.displayClientMessage(Component.literal(
                    "§b✦ +50 Source bonus §7(total: " + (current + 50) + ")"), true);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Comer: §b+50 Source bonus"));
        tooltip.add(Component.literal("§8§oCap 500"));
    }
}
