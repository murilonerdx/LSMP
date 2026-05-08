package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.AutoFarmerBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class AutoFarmerMenu extends AbstractContainerMenu {
    private final AutoFarmerBlockEntity be;
    private final Level level;
    private final ContainerData data;

    public AutoFarmerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (AutoFarmerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5));
    }

    public AutoFarmerMenu(int id, Inventory inv, AutoFarmerBlockEntity be, ContainerData data) {
        super(ModMenuTypes.AUTO_FARMER.get(), id);
        this.be = be;
        this.level = inv.player.level();
        this.data = data;

        addPlayer(inv);
        IItemHandler h = be.getInventory();
        addSlot(new SlotItemHandler(h, AutoFarmerBlockEntity.SLOT_CATALYST, 56, 35));
        addSlot(new SlotItemHandler(h, AutoFarmerBlockEntity.SLOT_OUTPUT,  116, 35));
        addDataSlots(data);
    }
    private void addPlayer(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    private int read32(int hi, int lo) { return ((data.get(hi) & 0xFFFF) << 16) | (data.get(lo) & 0xFFFF); }
    public int rawEnergy()    { return read32(0, 1); }
    public int rawEnergyMax() { return read32(2, 3); }
    public int cooldown()     { return data.get(4); }
    public float energyFrac() { return Math.min(1f, rawEnergy() / (float) Math.max(1, rawEnergyMax())); }
    public float cooldownFrac() {
        // 1.0 quando recém terminou, 0.0 quando pronto pra próxima
        return cooldown() / (float) AutoFarmerBlockEntity.COOLDOWN_TICKS;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (idx >= 36) {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        } else {
            if (!stack.is(ModItems.DARK_MATTER_CATALYST.get())) return ItemStack.EMPTY;
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
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, ModBlocks.AUTO_FARMER.get());
    }
}
