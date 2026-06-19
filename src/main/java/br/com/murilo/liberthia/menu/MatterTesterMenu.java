package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.MatterTesterBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class MatterTesterMenu extends AbstractContainerMenu {

    private final MatterTesterBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MatterTesterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    public MatterTesterMenu(int id, Inventory inv, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MATTER_TESTER.get(), id);
        this.blockEntity = (MatterTesterBlockEntity) blockEntity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        IItemHandler h = this.blockEntity.getItemHandler();
        // Amostra A e B (esquerda); itens não são consumidos — só lidos
        this.addSlot(new SlotItemHandler(h, MatterTesterBlockEntity.SLOT_A, 31, 24));
        this.addSlot(new SlotItemHandler(h, MatterTesterBlockEntity.SLOT_B, 31, 50));

        addDataSlots(data);
    }

    public MatterTesterBlockEntity getBlockEntity() { return blockEntity; }

    public boolean isScanning() { return data.get(0) > 0; }

    /** Progresso do scan 0..w. */
    public int getScaledProgress(int w) {
        int p = data.get(0), max = data.get(1);
        return (max != 0 && p != 0) ? p * w / max : 0;
    }

    public int mutationId()   { return data.get(2); } // 0 = nenhum teste; senão ordinal+1
    public int resDark()      { return data.get(3); }
    public int resWhite()     { return data.get(4); }
    public int resYellow()    { return data.get(5); }
    public boolean isNew()    { return data.get(6) != 0; }
    public boolean computerLinked() { return data.get(7) != 0; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        // 36 slots de player (0..35), depois 2 da máquina (36, 37)
        if (index >= 36) {
            if (!this.moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stack, 36, 38, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.MATTER_TESTER.get());
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }
}
