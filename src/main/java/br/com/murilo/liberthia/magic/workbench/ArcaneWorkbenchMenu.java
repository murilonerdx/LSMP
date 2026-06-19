package br.com.murilo.liberthia.magic.workbench;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * r159: Menu container do Arcane Workbench — 11 slots TIPADOS.
 *
 * <h2>Layout</h2>
 * <pre>
 *   Row 1 (y=18):  [Tablet] [Weave]   [Thread]  [Scroll]
 *   Row 2 (y=42):  [Orb]    [Focus]   [School]
 *   Row 3 (y=66):  [ModGlf] [Glyph]   [Parch]    →    [OUTPUT]
 *
 *   Player inv (y=92)
 *   Hotbar     (y=150)
 * </pre>
 */
public class ArcaneWorkbenchMenu extends AbstractContainerMenu {

    private final ArcaneWorkbenchBlockEntity be;

    public ArcaneWorkbenchMenu(int id, Inventory playerInv, ArcaneWorkbenchBlockEntity be) {
        super(br.com.murilo.liberthia.registry.ModMenuTypes.ARCANE_WORKBENCH.get(), id);
        this.be = be;

        // r166: linhas espaçadas 28px (em vez de 24) pra caber o rótulo embaixo de cada slot
        // Row 1
        addRestricted(ArcaneSlotType.SLOT_TABLET,   16,  20);
        addRestricted(ArcaneSlotType.SLOT_WEAVE,    40,  20);
        addRestricted(ArcaneSlotType.SLOT_THREAD,   64,  20);
        addRestricted(ArcaneSlotType.SLOT_SCROLL,   88,  20);
        // Row 2
        addRestricted(ArcaneSlotType.SLOT_ORB,      16,  48);
        addRestricted(ArcaneSlotType.SLOT_FOCUS,    40,  48);
        addRestricted(ArcaneSlotType.SLOT_SCHOOL,   64,  48);
        // Row 3
        addRestricted(ArcaneSlotType.SLOT_MODIFIER_GLYPH, 16, 76);
        addRestricted(ArcaneSlotType.SLOT_GLYPH,          40, 76);
        addRestricted(ArcaneSlotType.SLOT_PARCHMENT,      64, 76);

        // Output (output-only — não aceita input + consome todos ao retirar)
        addSlot(new SlotItemHandler(be.items, ArcaneSlotType.SLOT_OUTPUT, 132, 76) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                be.consumeAllInputs();
                super.onTake(player, stack);
            }
        });

        // Player inventory (descido pra y=114 por causa das linhas mais espaçadas)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 172));
        }
    }

    /** Slot tipado — só aceita item de acordo com {@link ArcaneSlotType}. */
    private void addRestricted(int slotIndex, int x, int y) {
        addSlot(new SlotItemHandler(be.items, slotIndex, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ArcaneSlotType.ORDER[slotIndex].accepts(stack);
            }
        });
    }

    /** Client-side constructor. */
    public ArcaneWorkbenchMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getClientBe(playerInv, buf));
    }

    private static ArcaneWorkbenchBlockEntity getClientBe(Inventory playerInv, FriendlyByteBuf buf) {
        net.minecraft.core.BlockPos pos = buf.readBlockPos();
        var be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof ArcaneWorkbenchBlockEntity wb) return wb;
        return new ArcaneWorkbenchBlockEntity(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    /** r164: pra Screen pegar block pos pro packet SetSpellName. */
    public net.minecraft.core.BlockPos getBlockPos() { return be.getBlockPos(); }

    /** r164: nome customizado atual (vem do server-side BE). */
    public String getCustomName() { return be.getCustomSpellName(); }

    @Override
    public boolean stillValid(Player player) {
        return be.getLevel() != null
                && be.getLevel().getBlockEntity(be.getBlockPos()) == be
                && player.distanceToSqr(be.getBlockPos().getCenter()) < 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();

        int workbenchSlotCount = ArcaneSlotType.TOTAL_SLOTS;
        if (index < workbenchSlotCount) {
            // Workbench → player inv
            if (!moveItemStackTo(original, workbenchSlotCount, slots.size(), false)) return ItemStack.EMPTY;
        } else {
            // Player inv → workbench: find first valid slot for this item
            int targetSlot = findValidSlot(original);
            if (targetSlot < 0 || !moveItemStackTo(original, targetSlot, targetSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Acha o primeiro slot vazio que aceita o item, ou -1. */
    private int findValidSlot(ItemStack stack) {
        for (int i = 0; i < ArcaneSlotType.SLOT_OUTPUT; i++) {
            if (be.items.getStackInSlot(i).isEmpty() && ArcaneSlotType.ORDER[i].accepts(stack)) {
                return i;
            }
        }
        return -1;
    }
}
