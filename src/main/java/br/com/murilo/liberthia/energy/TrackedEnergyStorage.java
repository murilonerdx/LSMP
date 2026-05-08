package br.com.murilo.liberthia.energy;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.EnergyStorage;

/**
 * EnergyStorage que marca o BE dono como dirty toda vez que a energia muda.
 *
 * <p><b>Por que existe:</b> o {@link EnergyStorage} padrão do Forge não tem
 * hook pra notificar o BE dono quando a energia é mutada via
 * {@code receiveEnergy} ou {@code extractEnergy} (chamadas via capability
 * por outros blocos — ex: laser puxando do gerador, AE2 acceptor pegando
 * de uma bateria).
 *
 * <p>Sem {@code setChanged()}, o chunk não é marcado pra save. No restart
 * do servidor, o NBT do BE volta pro último estado salvo — que pode ter
 * sido minutos antes. Energia "some" do gerador, da bateria, etc.
 *
 * <p>Esta classe garante: toda vez que a energia muda externamente,
 * {@code owner.setChanged()} é chamado → chunk dirty → save garantido
 * no próximo periodic save (ou no shutdown do servidor).
 */
public class TrackedEnergyStorage extends EnergyStorage {

    private final BlockEntity owner;

    public TrackedEnergyStorage(BlockEntity owner, int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
        this.owner = owner;
    }

    public TrackedEnergyStorage(BlockEntity owner, int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
        this.owner = owner;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int result = super.receiveEnergy(maxReceive, simulate);
        if (result > 0 && !simulate) owner.setChanged();
        return result;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int result = super.extractEnergy(maxExtract, simulate);
        if (result > 0 && !simulate) owner.setChanged();
        return result;
    }

    /** Setter direto (pra usos internos como burn fuel) — também marca dirty. */
    public void setStored(int amount) {
        if (amount < 0) amount = 0;
        if (amount > capacity) amount = capacity;
        if (this.energy != amount) {
            this.energy = amount;
            owner.setChanged();
        }
    }
}
