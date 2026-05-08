package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.FragmentedGeneratorBlockEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FragmentedGeneratorMenu extends AbstractContainerMenu {
    private final FragmentedGeneratorBlockEntity be;
    private final Level level;
    private final ContainerData data;

    public FragmentedGeneratorMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (FragmentedGeneratorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(7));
    }

    public FragmentedGeneratorMenu(int id, Inventory inv, FragmentedGeneratorBlockEntity be, ContainerData data) {
        super(ModMenuTypes.FRAGMENTED_GENERATOR.get(), id);
        this.be = be;
        this.level = inv.player.level();
        this.data = data;

        addPlayer(inv);
        IItemHandler h = be.getInventory();
        // Layout: fluido (44,30), diamante (44,52), output (122,40), upgrade (102,18)
        addSlot(new SlotItemHandler(h, FragmentedGeneratorBlockEntity.SLOT_FLUID,    44, 30));
        addSlot(new SlotItemHandler(h, FragmentedGeneratorBlockEntity.SLOT_DIAMOND,  44, 52));
        addSlot(new SlotItemHandler(h, FragmentedGeneratorBlockEntity.SLOT_OUTPUT,  122, 40));
        addSlot(new SlotItemHandler(h, FragmentedGeneratorBlockEntity.SLOT_UPGRADE, 152, 18));
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
    public int progress()        { return data.get(4); }
    public int progressMax()     { return Math.max(1, data.get(5)); }
    public int feSpent()         { return data.get(6); }
    public float energyFrac()    { return Math.min(1f, rawEnergy() / (float) Math.max(1, rawEnergyMax())); }
    public float progressFrac()  { return Math.min(1f, progress() / (float) progressMax()); }

    public FragmentedGeneratorBlockEntity getBlockEntity() { return be; }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // 0..35 player, 36..39 machine
        if (idx >= 36) {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        } else {
            int target = -1;
            if (stack.is(ModItems.DARK_MATTER_BUCKET.get())) target = 36;
            else if (stack.is(Items.NETHER_STAR)) target = 37;
            else if (stack.is(ModItems.SPEED_UPGRADE.get())) target = 39;
            if (target < 0) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, ModBlocks.FRAGMENTED_GENERATOR.get());
    }
}
