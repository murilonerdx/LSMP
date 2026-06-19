package br.com.murilo.liberthia.magic.spell.weaver;

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
 * v0.1.151 r119: <b>SpellWeaverMenu</b> — 9 slots BE + inventário player.
 *
 * <p>Slot layout no GUI 176x166:
 * <pre>
 *  ┌─────────────────────────────────┐
 *  │  [base]                  [out]   │   y=20  base @ x=20, out @ x=152
 *  │                                  │
 *  │  [m1][m2][m3][m4][m5][m6][m7]    │   y=55  modifier row
 *  │                                  │
 *  │     [player inventory]           │
 *  └─────────────────────────────────┘
 * </pre>
 */
public class SpellWeaverMenu extends AbstractContainerMenu {

    private final SpellWeaverBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    public SpellWeaverMenu(int id, Inventory playerInv, SpellWeaverBlockEntity be) {
        super(ModMenuTypes.SPELL_WEAVER.get(), id);
        this.blockEntity = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        IItemHandler items = be.getItems();

        // Base slot (esquerda)
        addSlot(new SlotItemHandler(items, SpellWeaverBlockEntity.BASE_SLOT, 20, 20));

        // 7 modifier slots em linha centralizada
        // 7 slots × 18px = 126; offset = (176-126)/2 = 25
        for (int i = 0; i < 7; i++) {
            addSlot(new SlotItemHandler(items,
                    SpellWeaverBlockEntity.FIRST_MOD_SLOT + i,
                    25 + i * 18, 55));
        }

        // Output slot (direita) — quando extrai, consome ingredientes
        addSlot(new SlotItemHandler(items, SpellWeaverBlockEntity.OUTPUT_SLOT, 152, 20) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                blockEntity.consumeIngredientsOnExtract();
            }
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

    public SpellWeaverMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getBE(playerInv, buf));
    }

    private static SpellWeaverBlockEntity getBE(Inventory inv, FriendlyByteBuf buf) {
        var pos = buf.readBlockPos();
        var be = inv.player.level().getBlockEntity(pos);
        if (be instanceof SpellWeaverBlockEntity swe) return swe;
        throw new IllegalStateException("No SpellWeaverBlockEntity at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModBlocks.SPELL_WEAVER.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0,
                true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack origStack = slot.getItem();
        ItemStack copy = origStack.copy();

        // 0 = base, 1-7 = mods, 8 = output; >= 9 = player
        if (index < 9) {
            if (!moveItemStackTo(origStack, 9, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // From player → tentamos achar slot adequado
            if (origStack.getItem() instanceof br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem) {
                if (!moveItemStackTo(origStack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (origStack.getItem() instanceof br.com.murilo.liberthia.magic.spell.composition.SpellModifierItem) {
                if (!moveItemStackTo(origStack, 1, 8, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (origStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
