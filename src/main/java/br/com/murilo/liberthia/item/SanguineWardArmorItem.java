package br.com.murilo.liberthia.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Marker armor type for Sanguine Ward set. Per-piece protection logic lives
 * in {@link br.com.murilo.liberthia.event.SanguineWardEvents}: each piece
 * grants a 25% chance to block fresh Blood Infection applications, and the
 * full set is fully immune + periodically cleanses ongoing infection.
 */
public class SanguineWardArmorItem extends ArmorItem {
    public SanguineWardArmorItem(ArmorItem.Type type, Properties props) {
        super(SanguineWardArmorMaterial.INSTANCE, type, props);
    }

    public static int countPieces(Player p) {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack s = p.getInventory().armor.get(i);
            if (s.getItem() instanceof SanguineWardArmorItem) n++;
        }
        return n;
    }

    public static boolean hasFullSet(Player p) {
        return countPieces(p) >= 4;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§c+25% §7resistência a Infecção de Sangue"));
        tip.add(Component.literal("§7Conjunto completo: §aimunidade total §7+ cura passiva."));
    }
}
