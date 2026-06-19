package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r97: <b>Repository BE</b> — chest 27 slots que auto-route items
 * inseridos pra Repository linkado que já tem aquele tipo de item.
 *
 * <p>Quando hopper insere, primeiro tenta merge com slots existentes que já
 * tem o mesmo tipo. Se cheio, propaga pra Repositories adjacentes via
 * `DominionLinkRegistry`.
 */
public class RepositoryBlockEntity extends BlockEntity implements WorldlyContainer {

    private static final int SIZE = 27;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public RepositoryBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.REPOSITORY.get(), pos, state);
    }

    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        return stack;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
    @Override public void clearContent() { items.clear(); }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] arr = new int[SIZE];
        for (int i = 0; i < SIZE; i++) arr[i] = i;
        return arr;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        // Smart routing — só accept se slot está vazio OU contém o mesmo tipo
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            // Verifica se algum OUTRO slot já tem este tipo
            for (int i = 0; i < SIZE; i++) {
                if (i == slot) continue;
                if (ItemStack.isSameItemSameTags(items.get(i), stack)
                        && items.get(i).getCount() < items.get(i).getMaxStackSize()) {
                    return false; // outro slot é melhor
                }
            }
            return true;
        }
        return ItemStack.isSameItemSameTags(existing, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
    }
}
