package br.com.murilo.liberthia.magic.thread;

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

/**
 * r172: Menu do Tear de Threads — 5 slots de input + 1 slot de output (preview).
 * Retirar o output consome 1 de cada input.
 */
public class ThreadLoomMenu extends AbstractContainerMenu {

    private final ThreadLoomBlockEntity be;
    private final Level level;

    public ThreadLoomMenu(int id, Inventory inv, ThreadLoomBlockEntity be) {
        super(ModMenuTypes.THREAD_LOOM.get(), id);
        this.be = be;
        this.level = inv.player.level();

        // 5 inputs (linha)
        for (int i = 0; i < ThreadLoomBlockEntity.INPUT_COUNT; i++) {
            addSlot(new SlotItemHandler(be.items, i, 26 + i * 18, 34));
        }
        // output (preview) — não aceita inserção; retirar consome inputs
        addSlot(new SlotItemHandler(be.items, ThreadLoomBlockEntity.SLOT_OUTPUT, 134, 34) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
            @Override public void onTake(Player p, ItemStack taken) {
                be.consumeInputs();
                super.onTake(p, taken);
            }
        });

        // inventário do player (abaixado pra caber o painel de preview)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
    }

    public ThreadLoomMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (ThreadLoomBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public net.minecraft.core.BlockPos getBlockPos() { return be.getBlockPos(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stk = slot.getItem();
        ItemStack orig = stk.copy();
        int invStart = ThreadLoomBlockEntity.TOTAL;            // slots 0-5 são do bloco
        if (index < invStart) {
            // do bloco → inventário
            if (!moveItemStackTo(stk, invStart, slots.size(), true)) return ItemStack.EMPTY;
            if (index == ThreadLoomBlockEntity.SLOT_OUTPUT) slot.onTake(player, stk);
        } else {
            // inventário → primeiros 5 inputs
            if (!moveItemStackTo(stk, 0, ThreadLoomBlockEntity.INPUT_COUNT, false)) return ItemStack.EMPTY;
        }
        if (stk.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return orig;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), player, ModBlocks.THREAD_LOOM.get());
    }
}
