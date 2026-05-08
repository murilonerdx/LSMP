package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.DarkMatterChestBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

public class DarkMatterChestMenu extends AbstractContainerMenu {
    private final DarkMatterChestBlockEntity be;
    private final Level level;

    public DarkMatterChestMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (DarkMatterChestBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public DarkMatterChestMenu(int id, Inventory inv, DarkMatterChestBlockEntity be) {
        super(ModMenuTypes.DARK_MATTER_CHEST.get(), id);
        this.be = be;
        this.level = inv.player.level();

        // Player inv (slots 0..35) — bottom of GUI (player inv at y=140, hotbar y=198)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 198));

        // Chest 9×6 — top portion (y=18..18+108)
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new SlotItemHandler(be.getInventory(),
                        col + row * 9, 8 + col * 18, 18 + row * 18));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        // 0..35 player, 36..89 chest
        if (idx < 36) {
            if (!moveItemStackTo(stack, 36, 90, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, ModBlocks.DARK_MATTER_CHEST.get());
    }
}
