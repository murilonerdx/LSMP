package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.QuantumTerminalBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.22 r28: Menu do Quantum Terminal (sem slots — só GUI display).
 */
public class QuantumTerminalMenu extends AbstractContainerMenu {

    private final QuantumTerminalBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public QuantumTerminalMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv,
                (QuantumTerminalBlockEntity) inv.player.level().getBlockEntity(extra.readBlockPos()),
                new SimpleContainerData(6));
    }

    public QuantumTerminalMenu(int id, Inventory inv, QuantumTerminalBlockEntity be, ContainerData data) {
        super(ModMenuTypes.QUANTUM_TERMINAL.get(), id);
        this.blockEntity = be;
        this.level = inv.player.level();
        this.data = data;
        addDataSlots(data);
    }

    public boolean isActive() { return data.get(0) == 1; }
    public int getEnergyStored() { return (data.get(1) & 0xFFFF) | ((data.get(2) & 0xFFFF) << 16); }
    public int getEnergyMax() { return (data.get(3) & 0xFFFF) | ((data.get(4) & 0xFFFF) << 16); }
    public int getLogSize() { return data.get(5); }

    public String getFrequency() { return blockEntity == null ? "" : blockEntity.getFrequency(); }
    public BlockPos getPos() { return blockEntity == null ? BlockPos.ZERO : blockEntity.getBlockPos(); }
    public List<QuantumTerminalBlockEntity.LogEntry> getLog() {
        return blockEntity == null ? java.util.Collections.emptyList() : blockEntity.getLogCopy();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        return ItemStack.EMPTY; // no slots
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.QUANTUM_TERMINAL.get());
    }
}
