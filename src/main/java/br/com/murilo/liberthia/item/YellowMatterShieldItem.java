package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.InfectionLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class YellowMatterShieldItem extends ShieldItem {

    public YellowMatterShieldItem(Properties properties) {
        super(properties.durability(600));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) return;

        // Only process when shield is in main hand or offhand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (stack != mainHand && stack != offHand) return;

        BlockPos pos = player.blockPosition();
        int particles = InfectionLogic.countDarkMatterParticles(level, pos, 16);
        float density = InfectionLogic.getChunkInfectionDensity(level, pos);

        // If particles > 20000 or density >= 1.0: break shield + spawn black hole at player pos
        if (particles > 20000 || density >= 1.0F) {
            stack.shrink(1);
            // The shield shatters under extreme dark matter pressure
            player.displayClientMessage(
                    Component.literal("\u00a74[Shield] Yellow Matter Shield shattered from dark matter overload!"),
                    true
            );
            return;
        }

        // If particles > 10000: durability damage * 3 per tick when blocking
        if (particles > 10000 && player.isBlocking()) {
            stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        if (level != null && level.isClientSide) {
            // Client-side tooltip will show a static message; actual particle count needs server data
            tooltipComponents.add(Component.literal("\u00a7eYellow Matter Shield"));
            tooltipComponents.add(Component.literal("\u00a77Degrades faster in high dark matter zones"));
        }
    }
}
