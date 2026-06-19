package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.MatterPurifierBlockEntity;
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
import net.minecraftforge.items.SlotItemHandler;

public class MatterPurifierMenu extends AbstractContainerMenu {

    private final MatterPurifierBlockEntity be;
    private final ContainerData data;

    public MatterPurifierMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (MatterPurifierBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(6));
    }

    public MatterPurifierMenu(int id, Inventory inv, MatterPurifierBlockEntity be, ContainerData data) {
        super(ModMenuTypes.MATTER_PURIFIER.get(), id);
        this.be = be;
        this.data = data;

        // 2 slots: input em (44, 35), output em (116, 35)
        addSlot(new SlotItemHandler(be.getInventory(), MatterPurifierBlockEntity.SLOT_INPUT, 44, 35));
        addSlot(new SlotItemHandler(be.getInventory(), MatterPurifierBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Player inv
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    public int getEnergy() { return ((data.get(0) & 0xFFFF) << 16) | (data.get(1) & 0xFFFF); }
    public int getMaxEnergy() { return ((data.get(2) & 0xFFFF) << 16) | (data.get(3) & 0xFFFF); }
    public int getProgress() { return data.get(4); }
    public int getMaxProgress() { return data.get(5); }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // 2 slots BE + 36 inv = 38 total
        if (idx < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else {
            // Tenta colocar no INPUT (slot 0) — só se for input válido
            if (MatterPurifierBlockEntity.outputForInput(stack) != null) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()),
                p, ModBlocks.MATTER_PURIFIER.get());
    }
}
