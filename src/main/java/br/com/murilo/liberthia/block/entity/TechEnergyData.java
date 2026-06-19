package br.com.murilo.liberthia.block.entity;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.IntSupplier;

/**
 * r182 — ContainerData de 6 ints p/ a GUI de energia. Energia/Max são ints grandes
 * (até 16M) que NÃO cabem num short do pacote do ContainerData, então são quebrados em
 * lo/hi (16 bits cada) e remontados no cliente. "atividade"/"total" são pequenos
 * (burnTime/burnTotal ou gen atual/gen max) e cabem direto.
 */
public final class TechEnergyData implements ContainerData {
    private final IntSupplier energy, maxEnergy, activity, total;

    public TechEnergyData(IntSupplier energy, IntSupplier maxEnergy, IntSupplier activity, IntSupplier total) {
        this.energy = energy; this.maxEnergy = maxEnergy; this.activity = activity; this.total = total;
    }

    @Override public int get(int i) {
        return switch (i) {
            case 0 -> energy.getAsInt() & 0xFFFF;
            case 1 -> (energy.getAsInt() >>> 16) & 0xFFFF;
            case 2 -> maxEnergy.getAsInt() & 0xFFFF;
            case 3 -> (maxEnergy.getAsInt() >>> 16) & 0xFFFF;
            case 4 -> activity.getAsInt();
            case 5 -> total.getAsInt();
            default -> 0;
        };
    }
    @Override public void set(int i, int v) {}
    @Override public int getCount() { return 6; }
}
