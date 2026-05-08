package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dark Matter Cell — pilha FE portátil que carrega outros itens FE da inventory.
 *
 * <p>Capacidade: {@link #MAX_ENERGY} FE. Auto-distribui {@link #DISCHARGE_RATE}
 * FE/tick para itens segurados (mãos, hotbar, armadura) que aceitem energia.
 *
 * <p>NBT armazena a quantidade atual no item-stack via {@code Energy} integer.
 * Quando empilhada, conta zerada (stack-to=1, então não importa).
 */
public class DarkMatterCellItem extends Item {

    public static final int MAX_ENERGY = 1_000_000;
    public static final int DISCHARGE_RATE = 500;
    private static final String TAG_ENERGY = "Energy";

    public DarkMatterCellItem(Properties props) {
        super(props.stacksTo(1).durability(0));
    }

    public static int getStored(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(TAG_ENERGY) : 0;
    }

    public static void setStored(ItemStack stack, int amount) {
        amount = Math.max(0, Math.min(MAX_ENERGY, amount));
        stack.getOrCreateTag().putInt(TAG_ENERGY, amount);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new CellCapability(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        int stored = getStored(stack);
        if (stored <= 0) return;

        // Auto-distribui pra outros itens FE da inventory do jogador
        int budget = Math.min(DISCHARGE_RATE, stored);
        int spent = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (budget <= 0) break;
            ItemStack other = player.getInventory().getItem(i);
            if (other == stack || other.isEmpty()) continue;
            // Não carrega outras Cells
            if (other.getItem() instanceof DarkMatterCellItem) continue;
            int sent = other.getCapability(ForgeCapabilities.ENERGY).map(es -> {
                if (!es.canReceive()) return 0;
                return es.receiveEnergy(Math.min(DISCHARGE_RATE, stored - 0), false);
            }).orElse(0);
            if (sent > 0) {
                budget -= sent;
                spent += sent;
            }
        }
        if (spent > 0) {
            setStored(stack, stored - spent);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int stored = getStored(stack);
        int pct = (int) (stored * 100L / MAX_ENERGY);
        ChatFormatting color = pct >= 50 ? ChatFormatting.GREEN
                : pct >= 20 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        tooltip.add(Component.literal(String.format("⚡ %,d / %,d FE (%d%%)",
                stored, MAX_ENERGY, pct)).withStyle(color));
        tooltip.add(Component.literal("Carrega itens FE da inventory")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) { return getStored(stack) > 0; }
    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * getStored(stack) / (float) MAX_ENERGY);
    }
    @Override
    public int getBarColor(ItemStack stack) {
        // Roxo pra matéria escura
        return 0x8B40D8;
    }

    /** ICapabilityProvider que expõe FE no item, persiste no NBT. */
    private static class CellCapability implements ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<IEnergyStorage> lazy;

        CellCapability(ItemStack stack) {
            this.stack = stack;
            this.lazy = LazyOptional.of(() -> new IEnergyStorage() {
                @Override public int receiveEnergy(int max, boolean simulate) {
                    int stored = getStored(stack);
                    int room = MAX_ENERGY - stored;
                    int received = Math.min(Math.min(room, max), DISCHARGE_RATE * 4);
                    if (!simulate) setStored(stack, stored + received);
                    return received;
                }
                @Override public int extractEnergy(int max, boolean simulate) {
                    int stored = getStored(stack);
                    int extracted = Math.min(Math.min(stored, max), DISCHARGE_RATE * 4);
                    if (!simulate) setStored(stack, stored - extracted);
                    return extracted;
                }
                @Override public int getEnergyStored() { return getStored(stack); }
                @Override public int getMaxEnergyStored() { return MAX_ENERGY; }
                @Override public boolean canExtract() { return true; }
                @Override public boolean canReceive() { return true; }
            });
        }

        @NotNull @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
            return LazyOptional.empty();
        }
    }
}
