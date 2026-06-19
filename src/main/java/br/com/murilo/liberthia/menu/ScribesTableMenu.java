package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.ScribesTableBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * v0.1.22 r69: Menu da Scribes Table — 10 slots customizados + inventário do player.
 *
 * <h2>Layout slots</h2>
 * <ul>
 *   <li>Slot 0 (parchment in): x=44, y=46</li>
 *   <li>Slots 1-8 (glyphs): row de 8 em y=20, x=8 + i*18</li>
 *   <li>Slot 9 (output): x=116, y=46</li>
 *   <li>Player inv: y=84, hotbar: y=142</li>
 * </ul>
 */
public class ScribesTableMenu extends AbstractContainerMenu {

    private final ScribesTableBlockEntity blockEntity;
    private final Level level;

    /** r164: pra Screen pegar block pos pro packet SetSpellName. */
    public net.minecraft.core.BlockPos getBlockPos() { return blockEntity.getBlockPos(); }

    /** r164: nome customizado atual (do server-side BE). */
    public String getCustomName() { return blockEntity.getCustomSpellName(); }

    /** Client constructor (factory). */
    public ScribesTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
            playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    /** Server constructor. */
    public ScribesTableMenu(int containerId, Inventory playerInventory, BlockEntity be) {
        super(ModMenuTypes.SCRIBES_TABLE.get(), containerId);
        this.blockEntity = (ScribesTableBlockEntity) be;
        this.level = playerInventory.player.level();

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        IItemHandler handler = blockEntity.getItemHandler();

        // Slot 0: parchment in (left)
        addSlot(new SlotItemHandler(handler, ScribesTableBlockEntity.SLOT_PARCHMENT_IN, 44, 46));

        // Slots 1-8: glyphs (row)
        for (int i = 0; i < 8; i++) {
            addSlot(new SlotItemHandler(handler,
                ScribesTableBlockEntity.SLOT_GLYPH_START + i, 8 + i * 18, 20));
        }

        // Slot 9: output (right) — não aceita placement, consume inputs on take
        addSlot(new SlotItemHandler(handler, ScribesTableBlockEntity.SLOT_OUTPUT, 116, 46) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.onOutputTaken();
            }
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        // Player inv: 0-35 (27 main + 9 hotbar)
        // Slots da table: 36 (parchment in), 37-44 (glyphs), 45 (output)
        int playerInvEnd = 36;
        int tableSlotsStart = 36;
        int tableSlotsEnd = 46; // exclusive

        if (slotIndex < playerInvEnd) {
            // Player inv → table (tenta colocar nos slots apropriados)
            if (!moveItemStackTo(slotStack, tableSlotsStart, tableSlotsEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Table → player inv
            if (slotIndex == 45) {
                // Output → inv: take with side-effects
                if (!moveItemStackTo(slotStack, 0, playerInvEnd, true)) return ItemStack.EMPTY;
                slot.onTake(player, slotStack);
            } else {
                if (!moveItemStackTo(slotStack, 0, playerInvEnd, false)) return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (slotStack.getCount() == originalStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
            player, ModBlocks.SCRIBES_TABLE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
}
