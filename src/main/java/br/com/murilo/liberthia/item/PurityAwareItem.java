package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.util.Purity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Item que mostra pureza na tooltip + adiciona prefixo no nome.
 * Usado por {@code inactive_dark_matter} e qualquer item que carregue
 * NBT {@link Purity#KEY}.
 */
public class PurityAwareItem extends Item {
    public PurityAwareItem(Properties props) { super(props); }

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
        int p = Purity.getPurity(stack);
        tip.add(Component.literal("§7Pureza: " + Purity.colorCode(p) + Purity.stars(p)
                + " §8(" + p + "/" + Purity.MAX + ")"));
        if (p > 0) {
            double mult = Purity.feMultiplier(p);
            tip.add(Component.literal(String.format("§7Multiplicador FE: §e%.1f×", mult)));
        }
        if (p < Purity.MAX) {
            tip.add(Component.literal("§8Refine no §dRefinador de Matéria§8 pra aumentar"));
        }
    }

    @Override public boolean isFoil(ItemStack stack) {
        return Purity.getPurity(stack) >= Purity.MAX || super.isFoil(stack);
    }
}
