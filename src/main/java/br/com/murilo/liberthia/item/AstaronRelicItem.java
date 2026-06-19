package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r195 — <b>Relíquia de Astaron</b> (loot-only, sem craft). Equipável num slot Curios; concede um
 * buff anti-cósmico (lógica em {@code event/AstaronRelicsHandler}). O efeito é descrito no tooltip.
 */
public class AstaronRelicItem extends Item {
    private final String desc;
    public AstaronRelicItem(String desc) {
        super(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant());
        this.desc = desc;
    }
    @Override public boolean isFoil(ItemStack s) { return true; }
    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("✦ Relíquia de Astaron").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tip.add(Component.literal("§7" + desc));
        tip.add(Component.literal("§8Equipe num slot Curios."));
    }
}
