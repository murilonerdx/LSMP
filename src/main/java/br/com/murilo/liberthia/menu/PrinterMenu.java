package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import br.com.murilo.liberthia.storage.PrinterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu da Impressora — slot de papel + inventário do player. A lista de
 * "pendências" (relatórios do Computador vizinho) é enviada no buf de abertura
 * e renderizada pela {@code PrinterScreen}.
 */
public class PrinterMenu extends AbstractContainerMenu {

    private final PrinterBlockEntity be;
    private final Level level;
    /** Só no cliente: lista de relatórios do computador adjacente (pra exibir). */
    private List<ComputerData.Entry> clientFiles = new ArrayList<>();

    public PrinterMenu(int id, Inventory inv, PrinterBlockEntity be) {
        super(ModMenuTypes.PRINTER.get(), id);
        this.be = be;
        this.level = inv.player.level();

        addSlot(new SlotItemHandler(be.getInventory(), PrinterBlockEntity.SLOT_PAPER, 16, 104) {
            @Override public boolean mayPlace(ItemStack s) { return s.is(Items.PAPER); }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 118 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 176));
    }

    /** Client constructor — lê pos + arquivos do computador adjacente do buf. */
    public PrinterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (PrinterBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
        this.clientFiles = ComputerData.fromList(ComputerData.unwrap(buf.readNbt()));
    }

    public List<ComputerData.Entry> getFiles() { return clientFiles; }
    public net.minecraft.core.BlockPos getBlockPos() { return be.getBlockPos(); }
    public boolean hasPaper() { return !be.getPaper().isEmpty(); }
    public int getEnergyStored() { return be.getEnergyStored(); }
    public int getMaxEnergy() { return be.getMaxEnergy(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stk = slot.getItem();
        ItemStack orig = stk.copy();
        if (index == 0) {
            if (!moveItemStackTo(stk, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stk, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (stk.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return orig;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), player, ModBlocks.PRINTER.get());
    }
}
