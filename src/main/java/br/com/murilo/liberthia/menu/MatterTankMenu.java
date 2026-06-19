package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.MatterTankBlockEntity;
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

/**
 * Menu do Matter Tank. 2 slots de inventário + dados sincronizados via ContainerData
 * (amount, capacity, fluidTypeId).
 */
public class MatterTankMenu extends AbstractContainerMenu {

    private final MatterTankBlockEntity be;
    private final ContainerData data;

    public MatterTankMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (MatterTankBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(3));
    }

    public MatterTankMenu(int id, Inventory inv, MatterTankBlockEntity be, ContainerData data) {
        super(ModMenuTypes.MATTER_TANK.get(), id);
        this.be = be;
        this.data = data;

        // 2 slots no centro:
        //   - SLOT_BUCKET_IN (bucket vazio entra, sai cheio) — top
        //   - SLOT_BUCKET_OUT (bucket cheio entra, sai vazio) — bottom
        addSlot(new SlotItemHandler(be.getInventory(), MatterTankBlockEntity.SLOT_BUCKET_IN, 80, 20));
        addSlot(new SlotItemHandler(be.getInventory(), MatterTankBlockEntity.SLOT_BUCKET_OUT, 80, 50));

        // Player inv
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    public int getAmount() { return data.get(0); }
    public int getCapacity() { return data.get(1); }
    /** 0=empty, 1=dark, 2=clear, 3=yellow. */
    public int getFluidTypeId() { return data.get(2); }

    public MatterTankBlockEntity getBlockEntity() { return be; }

    /** Server-side handler do botão Purgar. Chamado via clickMenuButton (botão id 0). */
    @Override
    public boolean clickMenuButton(Player player, int btnId) {
        if (btnId == 0 && be.getLevel() != null && !be.getLevel().isClientSide) {
            be.purge();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // 2 BE slots + 36 player inv = 38 total
        if (idx < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else {
            // v0.1.44: ambos (bucket vazio E matter buckets) vão pro slot IN.
            // OUT é só saída, gerada pelo tick. Player só toca no IN.
            if (!moveItemStackTo(stack, MatterTankBlockEntity.SLOT_BUCKET_IN,
                    MatterTankBlockEntity.SLOT_BUCKET_IN + 1, false)) return ItemStack.EMPTY;
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
                p, ModBlocks.MATTER_TANK.get());
    }
}
