package br.com.murilo.liberthia.block.entity.arcane;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

/**
 * r184 — receita posicional de máquina (sem o sistema de recipe do MC). inputs.get(i) casa
 * com o slot de entrada i (mesmo item + count suficiente). outputs vão pros slots de saída.
 * processTime &gt; 0 sobrescreve o tempo do tipo. Cadeias multi-etapa = output de uma máquina
 * é input da próxima (acoplamento natural pela registry).
 */
public record ArcaneRecipe(List<ItemStack> inputs, List<ItemStack> outputs, int processTime) {

    public boolean matches(ItemStackHandler inv, int inputSlots, int outputSlots) {
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack need = inputs.get(i);
            ItemStack have = inv.getStackInSlot(i);
            if (!ItemStack.isSameItem(have, need) || have.getCount() < need.getCount()) return false;
        }
        // saídas têm espaço?
        for (int j = 0; j < outputs.size(); j++) {
            ItemStack out = outputs.get(j);
            ItemStack cur = inv.getStackInSlot(inputSlots + j);
            if (!cur.isEmpty() && (!ItemStack.isSameItemSameTags(cur, out)
                    || cur.getCount() + out.getCount() > cur.getMaxStackSize())) return false;
        }
        return true;
    }
}
