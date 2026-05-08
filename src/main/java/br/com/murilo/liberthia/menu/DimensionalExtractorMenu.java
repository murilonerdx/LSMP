package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.DimensionalExtractorBlockEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class DimensionalExtractorMenu extends AbstractContainerMenu {
    private final DimensionalExtractorBlockEntity be;
    private final Level level;
    private final ContainerData data;

    public DimensionalExtractorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (DimensionalExtractorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(4));
    }

    public DimensionalExtractorMenu(int id, Inventory inv, DimensionalExtractorBlockEntity be, ContainerData data) {
        super(ModMenuTypes.DIMENSIONAL_EXTRACTOR.get(), id);
        this.be = be;
        this.level = inv.player.level();
        this.data = data;

        addPlayer(inv);
        IItemHandler h = be.getInventory();
        addSlot(new SlotItemHandler(h, DimensionalExtractorBlockEntity.SLOT_BUCKET_INPUT, 56, 35));
        addSlot(new SlotItemHandler(h, DimensionalExtractorBlockEntity.SLOT_OUTPUT, 116, 35));
        addDataSlots(data);
    }
    private void addPlayer(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public int progress()    { return data.get(0); }
    public int progressMax() { return Math.max(1, data.get(1)); }
    public int nearbyCount() { return data.get(2); }
    public int riftDistance(){ return data.get(3); }
    public float progressFrac() { return Math.min(1f, progress() / (float) progressMax()); }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (idx >= 36) {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        } else {
            // Bucket vazio → input slot (36); resto = output (não aceita inserção)
            if (!stack.is(Items.BUCKET)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 36, 37, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, ModBlocks.DIMENSIONAL_EXTRACTOR.get());
    }
}
