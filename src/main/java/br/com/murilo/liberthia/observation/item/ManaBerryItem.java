package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r74: <b>Mana Berry</b> — fruta comestível que dá Source.
 */
public class ManaBerryItem extends Item {

    private static final FoodProperties FOOD = new FoodProperties.Builder()
        .nutrition(2).saturationMod(0.1f).fast().build();

    public ManaBerryItem(Properties p) {
        super(p.food(FOOD).stacksTo(64).rarity(Rarity.UNCOMMON));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof ServerPlayer sp) {
            SourceData.add(sp, 20);
            sp.displayClientMessage(Component.literal(
                "§d§l+20 Source §r§7(consumiu Mana Berry)"), true);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§d§oMana Berry").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Comestível — restaura §5+20 Source§7."));
        t.add(Component.literal("§8§o\"Cresce onde a observação pulsa.\""));
    }
}
