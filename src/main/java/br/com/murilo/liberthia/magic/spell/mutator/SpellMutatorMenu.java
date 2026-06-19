package br.com.murilo.liberthia.magic.spell.mutator;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * v0.1.162 r138: <b>SpellMutatorMenu</b> — 3 slots: A, B, Output.
 *
 * <p>Layout no GUI 176x166:
 * <pre>
 *  [A]      [→]      [Output]
 *  [B]
 * </pre>
 */
public class SpellMutatorMenu extends AbstractContainerMenu {

    private final SpellMutatorBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public SpellMutatorMenu(int id, Inventory playerInv, SpellMutatorBlockEntity be) {
        super(ModMenuTypes.SPELL_MUTATOR.get(), id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        IItemHandler items = be.getItems();

        // Slot A — esquerda superior
        addSlot(new SlotItemHandler(items, SpellMutatorBlockEntity.SLOT_A, 30, 22));
        // Slot B — esquerda inferior
        addSlot(new SlotItemHandler(items, SpellMutatorBlockEntity.SLOT_B, 30, 50));
        // Output — direita centro
        addSlot(new SlotItemHandler(items, SpellMutatorBlockEntity.SLOT_OUT, 128, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.consumeInputs();
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public SpellMutatorMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBE(playerInv, buf));
    }

    private static SpellMutatorBlockEntity getBE(Inventory inv, FriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        var be = inv.player.level().getBlockEntity(pos);
        if (be instanceof SpellMutatorBlockEntity sme) return sme;
        throw new IllegalStateException("No SpellMutatorBlockEntity at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModBlocks.SPELL_MUTATOR.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0,
                true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack origStack = slot.getItem();
        ItemStack copy = origStack.copy();

        // 0=A, 1=B, 2=OUT, >=3 player
        if (index < 3) {
            if (!moveItemStackTo(origStack, 3, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // From player → put scroll into A first, then B
            if (origStack.getItem() instanceof br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem) {
                if (this.slots.get(0).getItem().isEmpty()) {
                    if (!moveItemStackTo(origStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (this.slots.get(1).getItem().isEmpty()) {
                    if (!moveItemStackTo(origStack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (origStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
