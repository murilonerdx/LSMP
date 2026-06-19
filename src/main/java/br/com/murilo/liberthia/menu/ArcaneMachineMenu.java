package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineBlockEntity;
import br.com.murilo.liberthia.block.entity.arcane.ArcaneMachineType;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/** r184 — menu genérico das máquinas Arcanas. Slots posicionados por {@link ArcaneMachineType}. */
public class ArcaneMachineMenu extends AbstractContainerMenu {
    public final ArcaneMachineType type;
    private final ContainerData data;
    private final ArcaneMachineBlockEntity be;
    private final Level level;

    public ArcaneMachineMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, fetch(inv, buf.readBlockPos()), new SimpleContainerData(6));
    }
    private static ArcaneMachineBlockEntity fetch(Inventory inv, BlockPos p) {
        return inv.player.level().getBlockEntity(p) instanceof ArcaneMachineBlockEntity b ? b : null;
    }

    public ArcaneMachineMenu(int id, Inventory inv, ArcaneMachineBlockEntity be, ContainerData data) {
        super(ModMenuTypes.ARCANE_MACHINE.get(), id);
        this.be = be; this.level = inv.player.level(); this.data = data;
        this.type = be != null ? be.type : ArcaneMachineType.METAL_PRESS;
        addPlayer(inv);
        if (be != null) {
            IItemHandler h = be.getInventory();
            for (int i = 0; i < type.inputSlots; i++)
                addSlot(new SlotItemHandler(h, i, slotX(i, false), 35));
            for (int j = 0; j < type.outputSlots; j++)
                addSlot(new SlotItemHandler(h, type.inputSlots + j, slotX(j, true), 35) {
                    @Override public boolean mayPlace(ItemStack s) { return false; }
                });
        }
        addDataSlots(data);
    }

    public static int slotX(int i, boolean out) { return out ? 116 + i * 22 : 30 + i * 22; }

    private void addPlayer(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public int energy() { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int maxEnergy() { int m = (data.get(3) << 16) | (data.get(2) & 0xFFFF); return m <= 0 ? 1 : m; }
    public float energyFrac() { return Math.min(1f, (float) energy() / maxEnergy()); }
    public float progressFrac() { return Math.min(1f, data.get(4) / (float) Math.max(1, data.get(5))); }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        int inEnd = 36 + type.inputSlots, machEnd = 36 + type.totalSlots();
        if (idx < 36) {
            if (type.inputSlots == 0 || !moveItemStackTo(stack, 36, inEnd, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return original;
    }

    @Override public boolean stillValid(Player p) {
        return be != null && stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, be.getBlockState().getBlock());
    }
}
