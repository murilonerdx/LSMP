package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.MatterExtractorBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Menu do Matter Extractor — sem inventário, só read-only data (FE, status, último player).
 * Decisão: extractor não tem slot porque a saída é via fluido pra rede; eliminar
 * slots simplifica e evita confundir o user com inputs que não existem.
 */
public class MatterExtractorMenu extends AbstractContainerMenu {

    private final MatterExtractorBlockEntity be;
    private final ContainerData data;

    public MatterExtractorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (MatterExtractorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(8));
    }

    public MatterExtractorMenu(int id, Inventory inv, MatterExtractorBlockEntity be, ContainerData data) {
        super(ModMenuTypes.MATTER_EXTRACTOR.get(), id);
        this.be = be;
        this.data = data;

        // Player inv só pra estar accessível em case de quickMove (ainda que não use)
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    public int getEnergy() { return ((data.get(0) & 0xFFFF) << 16) | (data.get(1) & 0xFFFF); }
    public int getMaxEnergy() { return ((data.get(2) & 0xFFFF) << 16) | (data.get(3) & 0xFFFF); }
    public int getStatus() { return data.get(4); }
    public int getLastAmount() { return data.get(5); }
    public int getLastFluidId() { return data.get(6); }
    public int getTankAmount() { return data.get(7); }

    public MatterExtractorBlockEntity getBlockEntity() { return be; }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        return ItemStack.EMPTY; // sem slots de inventário
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                p, ModBlocks.MATTER_EXTRACTOR.get());
    }
}
