package br.com.murilo.liberthia.magic.glyph.inscriber;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * v0.1.150 r118: <b>GlyphInscriberMenu</b> — container 4 input + 1 output + inventário player.
 *
 * <p>Slot layout no GUI 176x166:
 * <pre>
 *   ┌───────────────┐
 *   │   [0] [1]     │   inputs em cross
 *   │ [3] OUT [2]   │
 *   │               │
 *   │ [player inv]  │
 *   └───────────────┘
 * </pre>
 */
public class GlyphInscriberMenu extends AbstractContainerMenu {

    private final GlyphInscriberBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    /** Constructor server-side. */
    public GlyphInscriberMenu(int id, Inventory playerInv, GlyphInscriberBlockEntity be) {
        super(ModMenuTypes.GLYPH_INSCRIBER.get(), id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        IItemHandler items = be.getItems();

        // Layout cruz (4 inputs em volta do output) — coordinates relative to GUI 176x166
        // Centered horizontally on 88, vertically around y=35
        addSlot(new SlotItemHandler(items, 0, 80, 17));   // TOP
        addSlot(new SlotItemHandler(items, 1, 80, 53));   // BOTTOM
        addSlot(new SlotItemHandler(items, 2, 62, 35));   // LEFT
        addSlot(new SlotItemHandler(items, 3, 98, 35));   // RIGHT
        // Output slot — center
        addSlot(new SlotItemHandler(items, 4, 134, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        // Player inventory (3 rows x 9 cols)
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

    /** Constructor client-side (from buf). */
    public GlyphInscriberMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBE(playerInv, buf));
    }

    private static GlyphInscriberBlockEntity getBE(Inventory inv, FriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        var be = inv.player.level().getBlockEntity(pos);
        if (be instanceof GlyphInscriberBlockEntity gbe) return gbe;
        throw new IllegalStateException("No GlyphInscriberBlockEntity at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModBlocks.GLYPH_INSCRIBER.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0,
                true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack origStack = slot.getItem();
        ItemStack copy = origStack.copy();

        // 5 = output index. 0-3 = inputs. >= 5 = player slots.
        if (index < 5) {
            // De BE pra inventário
            if (!moveItemStackTo(origStack, 5, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // De inventário pra BE: tenta inserir em qualquer dos 4 input slots
            if (!moveItemStackTo(origStack, 0, 4, false)) return ItemStack.EMPTY;
        }

        if (origStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
