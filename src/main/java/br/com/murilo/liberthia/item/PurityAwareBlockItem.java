package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.util.Purity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * BlockItem que mostra pureza na tooltip + nome decorado.
 * Usado pelo {@code dark_matter_block} (que herda pureza do Crystallizer).
 */
public class PurityAwareBlockItem extends BlockItem {
    public PurityAwareBlockItem(Block block, Properties props) { super(block, props); }

    @Override
    public Component getName(ItemStack stack) {
        int p = Purity.getPurity(stack);
        Component base = super.getName(stack);
        if (p <= 0) return base;
        return Component.literal(Purity.colorCode(p) + Purity.stars(p) + "§r ")
                .append(base);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        int p = Purity.getPurity(stack);
        if (p > 0) {
            tip.add(Component.literal("§7Pureza: " + Purity.colorCode(p) + Purity.stars(p)));
            double mult = Purity.feMultiplier(p);
            tip.add(Component.literal(String.format("§7§oQueima a §e%.1f×§7§o no Gerador", mult)));
        }
    }

    @Override public boolean isFoil(ItemStack stack) {
        return Purity.getPurity(stack) >= Purity.MAX || super.isFoil(stack);
    }
}
