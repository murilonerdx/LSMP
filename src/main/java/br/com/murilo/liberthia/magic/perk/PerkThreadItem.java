package br.com.murilo.liberthia.magic.perk;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r90: <b>Perk Thread</b> — item consumível que carrega 1 perk.
 *
 * <p>Aplicado em armor via Anvil (TODO GUI). Por enquanto: shift+right-click
 * com armor na offhand instala o perk.
 */
public class PerkThreadItem extends Item {

    private final String perkId;

    public PerkThreadItem(Properties props, String perkId) {
        super(props.stacksTo(16));
        this.perkId = perkId;
    }

    public String getPerkId() { return perkId; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Perk perk = Perks.get(perkId);
        if (perk != null) {
            tooltip.add(Component.literal("§b").append(perk.getLabel()));
            tooltip.add(Component.literal("§7" + perk.getDescription()));
            tooltip.add(Component.literal("§8§oMax stack: " + perk.getMaxStack()));
        }
        tooltip.add(Component.literal("§8§oUse com armor pra instalar"));
    }
}
