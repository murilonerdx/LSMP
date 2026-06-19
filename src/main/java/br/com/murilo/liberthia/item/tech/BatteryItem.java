package br.com.murilo.liberthia.item.tech;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180c — <b>Bateria</b> (item FE portátil): carrega os outros itens energizados do
 * seu inventário com a própria energia. Recarregue-a num carregador/célula. Estilo
 * Powah "Energy Cell" de mão.
 */
public class BatteryItem extends Item {
    public static final int CAP = 1_000_000, XFER = 4_000;

    public BatteryItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }

    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) {
        return TechEnergy.provider(s, CAP, 20_000);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player p)) return;
        int budget = XFER;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && budget > 0; i++) {
            ItemStack other = inv.getItem(i);
            if (other == stack || other.isEmpty() || other.getItem() instanceof BatteryItem) continue;
            int avail = TechEnergy.energy(stack);
            if (avail <= 0) return;
            int give = Math.min(budget, avail);
            int moved = other.getCapability(ForgeCapabilities.ENERGY)
                    .map(es -> es.canReceive() ? es.receiveEnergy(give, false) : 0).orElse(0);
            if (moved > 0) { TechEnergy.setEnergy(stack, avail - moved, CAP); budget -= moved; }
        }
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal(TechEnergy.energy(s) + " / " + CAP + " FE").withStyle(net.minecraft.ChatFormatting.GREEN));
        tip.add(Component.translatable("tooltip.liberthia.battery").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, CAP); }
    @Override public int getBarColor(ItemStack s) { return TechEnergy.barColor(); }
}
