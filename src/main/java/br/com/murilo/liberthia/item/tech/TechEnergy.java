package br.com.murilo.liberthia.item.tech;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

/**
 * r180c — helper de ENERGIA EM ITEM (Powah/Mekanism): FE guardado na NBT do ItemStack,
 * exposto via capability ForgeCapabilities.ENERGY (carregável por carregador/bateria).
 * Usado pelas ferramentas/armaduras energizadas.
 */
public final class TechEnergy {
    private TechEnergy() {}
    private static final String TAG = "Energy";

    public static int energy(ItemStack s) { return s.getOrCreateTag().getInt(TAG); }
    public static void setEnergy(ItemStack s, int v, int cap) { s.getOrCreateTag().putInt(TAG, Math.max(0, Math.min(cap, v))); }
    public static boolean has(ItemStack s) { return energy(s) > 0; }
    public static boolean drain(ItemStack s, int amt, int cap) {
        int e = energy(s);
        if (e < amt) return false;
        setEnergy(s, e - amt, cap);
        return true;
    }
    public static int barWidth(ItemStack s, int cap) { return Math.round(13f * energy(s) / Math.max(1, cap)); }
    public static int barColor() { return 0x35E0A0; }

    public static ICapabilityProvider provider(ItemStack s, int cap, int xfer) { return new Prov(s, cap, xfer); }

    static final class Prov implements ICapabilityProvider {
        private final LazyOptional<IEnergyStorage> opt;
        Prov(ItemStack s, int cap, int xfer) { opt = LazyOptional.of(() -> new Store(s, cap, xfer)); }
        @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> c, Direction d) {
            return c == ForgeCapabilities.ENERGY ? opt.cast() : LazyOptional.empty();
        }
    }

    static final class Store implements IEnergyStorage {
        private final ItemStack s; private final int cap, xfer;
        Store(ItemStack s, int cap, int xfer) { this.s = s; this.cap = cap; this.xfer = xfer; }
        @Override public int receiveEnergy(int max, boolean sim) {
            int e = energy(s), r = Math.min(xfer, Math.min(max, cap - e));
            if (r > 0 && !sim) setEnergy(s, e + r, cap);
            return Math.max(0, r);
        }
        @Override public int extractEnergy(int max, boolean sim) {
            int e = energy(s), x = Math.min(xfer, Math.min(max, e));
            if (x > 0 && !sim) setEnergy(s, e - x, cap);
            return Math.max(0, x);
        }
        @Override public int getEnergyStored() { return energy(s); }
        @Override public int getMaxEnergyStored() { return cap; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    }
}
