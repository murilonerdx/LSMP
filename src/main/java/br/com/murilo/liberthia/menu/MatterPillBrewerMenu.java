package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.MatterPillBrewerBlockEntity;
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
import net.minecraft.world.item.Items;
import net.minecraftforge.items.SlotItemHandler;

/**
 * v0.1.44: Menu do Matter Pill Brewer.
 *
 * <p>Layout (176×166):
 * <ul>
 *   <li>SLOT_INGOT em (38, 35) — ingot purified de DM/CM/YM</li>
 *   <li>SLOT_BOTTLE em (62, 35) — glass bottle</li>
 *   <li>SLOT_OUTPUT em (122, 35) — pílula gerada</li>
 * </ul>
 *
 * <p>QuickMove inteligente: ingots vão pra slot 0, glass bottle vai pra slot 1.
 */
public class MatterPillBrewerMenu extends AbstractContainerMenu {

    private final MatterPillBrewerBlockEntity be;
    private final ContainerData data;

    public MatterPillBrewerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (MatterPillBrewerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(4));
    }

    public MatterPillBrewerMenu(int id, Inventory inv, MatterPillBrewerBlockEntity be, ContainerData data) {
        super(ModMenuTypes.MATTER_PILL_BREWER.get(), id);
        this.be = be;
        this.data = data;

        addSlot(new SlotItemHandler(be.getInventory(), MatterPillBrewerBlockEntity.SLOT_INGOT, 38, 35));
        addSlot(new SlotItemHandler(be.getInventory(), MatterPillBrewerBlockEntity.SLOT_BOTTLE, 62, 35));
        addSlot(new SlotItemHandler(be.getInventory(), MatterPillBrewerBlockEntity.SLOT_OUTPUT, 122, 35) {
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

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    /** v0.1.49: energia armazenada (FE) — barra na GUI. */
    public int getEnergy() { return data.get(2); }
    public int getMaxEnergy() { return data.get(3); }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // 3 BE slots + 36 player inv = 39 total
        if (idx < 3) {
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else {
            // Ingot purified → slot 0
            if (MatterPillBrewerBlockEntity.ingotToPill(stack.getItem()) != null) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.is(Items.GLASS_BOTTLE)) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
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
                p, ModBlocks.MATTER_PILL_BREWER.get());
    }
}
